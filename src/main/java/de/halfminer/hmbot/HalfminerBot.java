package de.halfminer.hmbot;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3ApiAsync;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.TS3Query.FloodRate;
import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException;
import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import com.github.theholywaffle.teamspeak3.api.wrapper.ChannelGroup;
import de.halfminer.hmbot.exception.NoConfigurationException;
import de.halfminer.hmbot.storage.BotConfig;
import de.halfminer.hmbot.storage.BotStorage;
import de.halfminer.hmbot.tasks.StatusPUT;
import de.halfminer.hmbot.tasks.TaskInactivityCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Halfminer Teamspeak 3 query bot, implementing a chat based commandLine interface and automatic tasks.
 *
 * @author Fabian Prieto Wunderlich - Kakifrucht
 */
public class HalfminerBot {

    private final static Logger logger = LoggerFactory.getLogger(HalfminerBot.class);

    public static void main(String[] args) {

        logger.info("HalfminerBot is starting...");

        BotConfig botConfig;
        try {
            botConfig = new BotConfig();
        } catch (NoConfigurationException e) {
            try {
                Thread.sleep(2000L);
            } catch (InterruptedException ignored) {
            }
            return;
        }

        if (args.length > 0) {

            botConfig.setPassword(args[0]);
            new HalfminerBot(botConfig);
        } else logger.error("No password given. Please provide it via commandline argument.");
    }

    private static HalfminerBot instance;

    private final BotConfig botConfig;
    private final TS3Query query;
    private TS3Api api;
    private TS3ApiAsync apiAsync;
    private BotStorage storage;

    private HalfminerBot(BotConfig botConfig) {

        instance = this;
        this.botConfig = botConfig;

        // START Configure API
        TS3Config apiConfig = new TS3Config();
        apiConfig.setHost(botConfig.getHost());
        apiConfig.setDebugLevel(java.util.logging.Level.WARNING);
        if (botConfig.hasLocalhost()) {
            apiConfig.setFloodRate(FloodRate.UNLIMITED);
            logger.info("Floodrate set to unlimited");
        } else apiConfig.setFloodRate(FloodRate.DEFAULT);

        // START Open Query
        query = new TS3Query(apiConfig);
        try {
            query.connect();
        } catch (TS3ConnectionFailedException e) {
            logger.error("Couldn't connect to given server, quitting...");
            return;
        }

        this.api = query.getApi();
        this.apiAsync = query.getAsyncApi();
        // START Check login, port and Nickname
        if (api.login("serveradmin", botConfig.getPassword())) {
            if (!api.selectVirtualServerByPort(botConfig.getPort()) || !api.setNickname(botConfig.getBotName())) {
                stop("The provided port or botname are not valid, quitting...");
            }

            // START Check if channelgroup exists
            boolean channelGroupExists = false;
            for (ChannelGroup o : api.getChannelGroups()) {
                if (o.getId() == botConfig.getChannelAdminID()) channelGroupExists = true;
            }
            if (!channelGroupExists) stop("The provided channelAdminID does not exist, quitting...");

            // START Move bot into channel and start listeners
            List<Channel> channels = api.getChannelsByName(botConfig.getChannelMoveName());

            if (channels == null || !api.moveClient(api.whoAmI().getId(), channels.get(0).getId())) {
                logger.error("The provided channelname does not exist or can't be accessed, staying in default channel");
            }

            this.storage = new BotStorage();
            api.registerAllEvents();
            api.addTS3Listeners(new HalfminerBotListeners());

            ScheduledExecutorService schedule = Executors.newScheduledThreadPool(1);
            schedule.scheduleAtFixedRate(new StatusPUT(), 0, 1, TimeUnit.MINUTES);

            TaskInactivityCheck inactivityCheck = new TaskInactivityCheck();
            if (inactivityCheck.isEnabled())
                schedule.scheduleAtFixedRate(inactivityCheck, 10, 10, TimeUnit.SECONDS);
            else logger.info("Could not get AFK Channel. Inactivity check disabled.");

            logger.info("HalfminerBot connected successfully and ready");
        } else {
            stop("The provided password is not valid, quitting...");
        }
    }

    public static HalfminerBot getInstance() {
        return instance;
    }

    private void stop(String message) {

        query.exit();
        if (message.length() > 0) {
            logger.info(message);
        } else {
            logger.info("Bot quitting...");
        }

        try {
            Thread.sleep(2000L);
        } catch (InterruptedException ignored) {}

        Runtime.getRuntime().exit(0);
    }

    public TS3Api getApi() {
        return api;
    }

    TS3ApiAsync getApiAsync() {
        return apiAsync;
    }

    public BotStorage getStorage() {
        return storage;
    }

    public BotConfig getBotConfig() {
        return botConfig;
    }
}
