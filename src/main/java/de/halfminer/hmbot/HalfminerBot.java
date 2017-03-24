package de.halfminer.hmbot;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3ApiAsync;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.TS3Query.FloodRate;
import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException;
import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import de.halfminer.hmbot.storage.BotStorage;
import de.halfminer.hmbot.storage.ConfigurationException;
import de.halfminer.hmbot.storage.YamlConfig;
import de.halfminer.hmbot.task.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Halfminer Teamspeak 3 query bot, implementing a chat based command interface and automatic tasks.
 *
 * @author Fabian Prieto Wunderlich - Kakifrucht
 */
public class HalfminerBot {

    private final static Logger logger = LoggerFactory.getLogger(HalfminerBot.class);

    public static void main(String[] args) {

        logger.info("HalfminerBot is starting...");

        YamlConfig config;
        try {
            config = args.length > 0 ? new YamlConfig(args[0]) : new YamlConfig();
        } catch (ConfigurationException e) {
            String message = ", quitting..." + e.getMessage();
            if (e.shouldPrintStacktrace()) {
                logger.error(message, e);
            } else {
                logger.error(message);
            }

            try {
                Thread.sleep(2000L);
            } catch (InterruptedException ignored) {}

            return;
        }

        new HalfminerBot(config);
    }

    private static HalfminerBot instance;

    static HalfminerBot getInstance() {
        return instance;
    }

    private final YamlConfig botConfig;
    private final Scheduler scheduler;
    private final TS3Query query;
    private TS3Api api;
    private TS3ApiAsync apiAsync;
    private BotStorage storage;

    private HalfminerBot(YamlConfig botConfig) {

        instance = this;
        this.botConfig = botConfig;
        this.scheduler = new Scheduler();

        // START Configure API
        String host = botConfig.getString("host", "localhost");
        TS3Config apiConfig = new TS3Config();
        apiConfig.setHost(host);
        if (host.equals("localhost")) {
            apiConfig.setFloodRate(FloodRate.UNLIMITED);
            logger.info("Floodrate set to unlimited");
        } else {
            apiConfig.setFloodRate(FloodRate.DEFAULT);
        }

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
        if (api.login("serveradmin", botConfig.getString("password", ""))) {

            if (!api.selectVirtualServerByPort(botConfig.getInt("port", 9987))
                    || !api.setNickname(botConfig.getString("botName", "Halfminer TSBot"))) {
                stop("The provided port or botname are not valid, quitting...");
            }

            this.storage = new BotStorage();

            // START Move bot into channel and start listeners
            List<Channel> channels = api.getChannelsByName(botConfig.getString("botChannelName", "Welcome"));
            if (channels == null
                    || channels.isEmpty()
                    || !api.moveClient(api.whoAmI().getId(), channels.get(0).getId())) {
                logger.error("The provided channelname does not exist or can't be accessed, staying in default channel");
            }

            api.registerAllEvents();
            api.addTS3Listeners(new HalfminerBotListeners());
            scheduler.registerAllTasks();

            logger.info("HalfminerBot connected successfully and ready");

        } else {
            stop("The provided password is not valid, quitting...");
        }
    }

    private void stop(String message) {

        if (message.length() > 0) {
            logger.info(message);
        } else {
            logger.info("Bot quitting...");
        }

        query.exit();
        try {
            Thread.sleep(2000L);
        } catch (InterruptedException ignored) {}

        System.exit(1);
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

    YamlConfig getBotConfig() {
        return botConfig;
    }
}
