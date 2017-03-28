package de.halfminer.hmbot;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.TS3Query.FloodRate;
import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException;
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler;
import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import de.halfminer.hmbot.config.ConfigurationException;
import de.halfminer.hmbot.config.YamlConfig;
import de.halfminer.hmbot.storage.Storage;
import de.halfminer.hmbot.task.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Halfminer Teamspeak 3 query bot, implementing a chat based command interface and automated tasks.
 *
 * @author Fabian Prieto Wunderlich - Kakifrucht
 */
public class HalfminerBot {

    private final static Logger logger = LoggerFactory.getLogger(HalfminerBot.class);
    private final static Object monitor = new Object();
    private static HalfminerBot instance;
    private static boolean startBot = true;

    public static void main(String[] args) {

        logger.info("HalfminerBot v{} is starting", getVersion());

        YamlConfig config;
        try {
            config = args.length > 0 ? new YamlConfig(args[0]) : new YamlConfig();
        } catch (ConfigurationException e) {

            String message = e.getMessage() + ", quitting...";
            if (e.shouldPrintStacktrace()) {
                logger.error(message, e);
            } else {
                if (e.getMessage() != null) {
                    logger.error(message);
                }
            }

            try {
                Thread.sleep(2000L);
            } catch (InterruptedException ignored) {}
            return;
        }

        // setting startBot to true before stopping threads will restart the bot
        while (startBot) {
            startBot = false;
            new Thread(() -> new HalfminerBot(config), "launch-bot").start();
            synchronized (monitor) {
                try {
                    monitor.wait();
                    Thread.sleep(2000L);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    static HalfminerBot getInstance() {
        return instance;
    }

    public static String getVersion() {
        return HalfminerBot.class.getPackage().getImplementationVersion();
    }

    // -- Static End -- //

    private final YamlConfig botConfig;
    private final Scheduler scheduler;
    private final TS3Query query;
    private TS3Api api;
    private Storage storage;

    private HalfminerBot(YamlConfig botConfig) {

        instance = this;
        this.botConfig = botConfig;
        this.scheduler = new Scheduler();

        // configure API
        String host = botConfig.getString("host");
        TS3Config apiConfig = new TS3Config();
        apiConfig.setHost(host);
        apiConfig.setQueryPort(botConfig.getInt("ports.queryPort"));
        if (host.equals("localhost")) {
            apiConfig.setFloodRate(FloodRate.UNLIMITED);
        } else {
            apiConfig.setFloodRate(FloodRate.DEFAULT);
            logger.info("Command rate is reduced, connect via localhost to remove command delay");
        }

        apiConfig.setConnectionHandler(new ConnectionHandler() {
            @Override
            public void onConnect(TS3Query ts3Query) {
            }

            @Override
            public void onDisconnect(TS3Query ts3Query) {
                logger.warn("Bot lost connection to server, trying to reconnect once in 10 seconds...");
                try {
                    Thread.sleep(10000L);
                } catch (InterruptedException ignored) {}
                stop("Restarting...", true);
            }
        });

        // connect to query
        query = new TS3Query(apiConfig);
        try {
            query.connect();
        } catch (TS3ConnectionFailedException e) {
            stop("Couldn't connect to given server, quitting...", false);
            return;
        }

        this.api = query.getApi();

        // login to server
        if (api.login(botConfig.getString("credentials.username"), botConfig.getString("credentials.password"))) {

            if (!api.selectVirtualServerByPort(botConfig.getInt("ports.serverPort"))) {
                stop("The provided server port is not valid, quitting...", false);
                return;
            }

            if (!api.setNickname(botConfig.getString("botName"))) {
                logger.warn("The provided botname is already in use or invalid, keeping default ({})",
                        api.whoAmI().getNickname());
            }

            this.storage = new Storage();

            // move bot into channel
            List<Channel> channels = api.getChannelsByName(botConfig.getString("botChannelName"));
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
            stop("The provided password is not valid, quitting...", false);
        }
    }

    public boolean reloadConfig() {
        if (botConfig.reloadConfig()) {
            logger.info("Config file was reloaded");
            scheduler.configWasReloaded();
            storage.configWasReloaded();
            return true;
        } return false;
    }

    public void stop(String message, boolean restart) {

        logger.info(message.length() > 0 ? message : "Bot quitting...");
        scheduler.shutdown();
        try {
            query.exit();
        } catch (Exception e) {
            if (!(e instanceof NullPointerException)) {
                logger.warn("Couldn't disconnect from query properly", e);
            }
        }

        synchronized (monitor) {
            startBot = restart;
            monitor.notify();
        }
    }

    YamlConfig getBotConfig() {
        return botConfig;
    }

    Scheduler getScheduler() {
        return scheduler;
    }

    TS3Api getApi() {
        return api;
    }

    public Storage getStorage() {
        return storage;
    }
}
