package de.halfminer.hmbot;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.TS3Query.FloodRate;
import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException;
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler;
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy;
import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import de.halfminer.hmbot.config.PasswordYamlConfig;
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

        PasswordYamlConfig config = new PasswordYamlConfig("config.yml", args.length > 0 ? args[0] : "");
        if (!config.reloadConfig() || config.isUsingDefaultConfig()) {

            if (config.isUsingDefaultConfig()) {
                logger.info("Please fill out the config file at \"hmbot/config.yml\" and restart the bot");
            }

            logger.info("Quitting...");
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

    private final PasswordYamlConfig botConfig;
    private final YamlConfig locale = new YamlConfig("locale.yml");
    private final TS3Query query;
    private Scheduler scheduler;
    private TS3Api api;
    private Storage storage;

    private HalfminerBot(PasswordYamlConfig botConfig) {

        instance = this;
        this.botConfig = botConfig;
        if (!locale.reloadConfig()) {
            query = null;
            stop("Couldn't read locale file, quitting...", false);
            return;
        }

        // configure API
        String host = botConfig.getString("host");
        TS3Config apiConfig = new TS3Config();
        apiConfig.setHost(host);
        apiConfig.setQueryPort(botConfig.getInt("ports.queryPort"));
        if (host.equals("localhost") || botConfig.getBoolean("isWhitelisted")) {
            apiConfig.setFloodRate(FloodRate.UNLIMITED);
        } else {
            apiConfig.setFloodRate(FloodRate.DEFAULT);
            logger.info("Command rate is reduced, set isWhitelisted to true in config or connect via localhost");
        }

        apiConfig.setReconnectStrategy(ReconnectStrategy.exponentialBackoff());
        apiConfig.setConnectionHandler(new ConnectionHandler() {

            private boolean hasConnected = false;
            private int attempts = 0;

            @Override
            public void onConnect(TS3Query ts3Query) {
                scheduler = new Scheduler();
                if (hasConnected) {
                    logger.info("Reconnect attempt #{}", ++attempts);
                    if (startBot()) {
                        attempts = 0;
                    }
                } else {
                    startBot();
                    hasConnected = true;
                }
            }

            @Override
            public void onDisconnect(TS3Query ts3Query) {
                logger.warn("Bot lost connection to server, trying to reconnect...");
                scheduler.shutdown();
            }
        });

        // connect to query
        query = new TS3Query(apiConfig);
        try {
            query.connect();
        } catch (TS3ConnectionFailedException e) {
            stop("Couldn't connect to given server, quitting...", false);
        }
    }

    private boolean startBot() {
        this.api = query.getApi();

        // login to server
        if (api.login(botConfig.getString("credentials.username"), botConfig.getPassword())) {

            if (!api.selectVirtualServerByPort(botConfig.getInt("ports.serverPort"))) {
                stop("The provided server port is not valid, quitting...", false);
                return false;
            }

            String nickName = botConfig.getString("botName");
            if (!api.setNickname(nickName)) {
                boolean nicknameWasSet = false;
                for (int i = 1; i < 10; i++) {
                    if (api.setNickname(nickName + i)) {
                        logger.warn("The provided botname is already in use or invalid, logged in as {}", nickName + i);
                        nickName = nickName + i;
                        nicknameWasSet = true;
                        break;
                    }
                }

                if (!nicknameWasSet) {
                    stop("The provided botname is already in use or invalid, quitting...", false);
                    return false;
                }
            }

            // move bot into channel
            List<Channel> channels = api.getChannelsByName(botConfig.getString("botChannelName"));
            if (channels == null
                    || channels.isEmpty()
                    || !api.moveClient(api.whoAmI().getId(), channels.get(0).getId())) {
                logger.error("The provided channelname does not exist or can't be accessed, staying in default channel");
            }

            if (storage == null) {
                this.storage = new Storage();
            } else {
                storage.configWasReloaded();
            }

            api.registerAllEvents();
            api.addTS3Listeners(new HalfminerBotListeners());
            scheduler.registerAllTasks();

            logger.info("HalfminerBot connected successfully and ready as {}", nickName);
            return true;
        } else {
            stop("The provided password is not valid, quitting...", false);
            return false;
        }
    }

    public boolean reloadConfig() {
        boolean localeReloaded = locale.reloadConfig();
        if (botConfig.reloadConfig()) {
            logger.info("Config file was reloaded");
            scheduler.configWasReloaded();
            storage.configWasReloaded();
            return true;
        } else return localeReloaded;
    }

    public void stop(String message, boolean restart) {

        logger.info(message.length() > 0 ? message : "Bot quitting...");
        if (scheduler != null) scheduler.shutdown();
        if (query != null) {
            try {
                query.exit();
            } catch (NullPointerException ignored) {
                // if exiting from query without successful connect NullPointerEx is thrown, we cannot check beforehand
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

    public YamlConfig getLocale() {
        return locale;
    }

    Scheduler getScheduler() {
        return scheduler;
    }

    public TS3Api getApi() {
        return api;
    }

    public Storage getStorage() {
        return storage;
    }
}
