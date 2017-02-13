package de.halfminer.hmtsbot;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3ApiAsync;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.TS3Query.FloodRate;
import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException;
import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import com.github.theholywaffle.teamspeak3.api.wrapper.ChannelGroup;
import de.halfminer.hmtsbot.actions.ActionManager;
import de.halfminer.hmtsbot.exception.NoConfigurationException;
import de.halfminer.hmtsbot.scheduled.InactivityCheck;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HalfminerBot {

    public static void main(String[] args) {

        logger = Logger.getLogger("HalfminerBot");
        logger.setLevel(Level.INFO);
        logger.info("HalfminerBot is starting...");

        HalfminerConfig config;
        try {
            config = new HalfminerConfig();
        } catch (NoConfigurationException e) {
            try {
                Thread.sleep(2000L);
            } catch (InterruptedException ignored) {
            }
            return;
        }

        if (args.length > 0) {

            config.setPassword(args[0]);
            new HalfminerBot(config);
        } else logger.severe("No password given. Please provide it via commandline argument.");
    }

    private static HalfminerBot instance;
    private static Logger logger;
    private final HalfminerConfig config;

    private final TS3Query query;
    private TS3Api api;
    private TS3ApiAsync apiAsync;
    private HalfminerStorage storage;
    private ActionManager actions;

    private HalfminerBot(HalfminerConfig config) {

        instance = this;
        this.config = config;

        // START Configure API
        TS3Config ts3config = new TS3Config();
        ts3config.setHost(config.getHost());
        ts3config.setDebugLevel(Level.WARNING);
        if (config.hasLocalhost()) {
            ts3config.setFloodRate(FloodRate.UNLIMITED);
            logger.info("Floodrate set to unlimited");
        } else ts3config.setFloodRate(FloodRate.DEFAULT);

        // START Open Query
        query = new TS3Query(ts3config);
        try {
            query.connect();
        } catch (TS3ConnectionFailedException e) {
            logger.warning("Couldn't connect to given server, quitting...");
            return;
        }

        this.api = query.getApi();
        this.apiAsync = query.getAsyncApi();
        // START Check login, port and Nickname
        if (api.login("serveradmin", config.getPassword())) {
            if (!api.selectVirtualServerByPort(config.getPort()) || !api.setNickname(config.getBotName())) {
                stop("The provided port or botname are not valid, quitting...", 2);
            }

            // START Check if channelgroup exists
            boolean channelGroupExists = false;
            for (ChannelGroup o : api.getChannelGroups()) {
                if (o.getId() == config.getChannelAdminID()) channelGroupExists = true;
            }
            if (!channelGroupExists) stop("The provided channelAdminID does not exist, quitting...", 2);

            // START Move bot into channel and start listeners
            List<Channel> channels = api.getChannelsByName(config.getChannelMoveName());

            if (channels == null || !api.moveClient(api.whoAmI().getId(), channels.get(0).getId())) {
                logger.warning("The provided channelname does not exist or can't be accessed, staying in default channel");
            }

            this.storage = new HalfminerStorage();
            this.actions = new ActionManager();
            api.registerAllEvents();
            api.addTS3Listeners(new HalfminerBotListener());

            InactivityCheck inactivityCheck = new InactivityCheck();
            if (inactivityCheck.isEnabled())
                Executors.newScheduledThreadPool(1).scheduleAtFixedRate(inactivityCheck, 10, 10, TimeUnit.SECONDS);
            else logger.info("Could not get AFK Channel. Inactivity check disabled.");

            logger.info("HalfminerBot connected successfully and ready");
        } else {
            stop("The provided password is not valid, quitting...", 2);
        }
    }

    public static HalfminerBot getInstance() {
        return instance;
    }

    public static Logger getLogger() {
        return logger;
    }

    private void stop(String message, int seconds) {

        query.exit();
        if (message.length() > 0) {
            logger.warning(message);
        } else logger.warning("Bot quitting...");

        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException ignored) {
        }
        Runtime.getRuntime().exit(0);
    }

    public TS3Api getApi() {
        return api;
    }

    public TS3ApiAsync getApiAsync() {
        return apiAsync;
    }

    public HalfminerStorage getStorage() {
        return storage;
    }

    public HalfminerConfig getConfig() {
        return config;
    }

    public ActionManager getActionmanager() {
        return actions;
    }

}
