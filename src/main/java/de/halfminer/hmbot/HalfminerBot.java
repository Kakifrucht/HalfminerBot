package de.halfminer.hmbot;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException;
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler;
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy;
import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import de.halfminer.hmbot.config.BotConfig;
import de.halfminer.hmbot.config.BotPasswordConfig;
import de.halfminer.hmbot.config.PasswordYamlConfig;
import de.halfminer.hmbot.config.YamlConfig;
import de.halfminer.hmbot.storage.DefaultStorage;
import de.halfminer.hmbot.storage.Storage;
import de.halfminer.hmbot.task.ExecutorScheduler;
import de.halfminer.hmbot.task.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

/**
 * Halfminer TeamSpeak 3 query bot, implementing a chat based command interface and automated tasks.
 *
 * @author Fabian Prieto Wunderlich - Kakifrucht
 */
class HalfminerBot implements ComponentHolder, StateHolder {

    private final static Logger logger = LoggerFactory.getLogger(HalfminerBot.class);
    private final static Object mainThreadLock = new Object();
    private static HalfminerBot instance;
    private static boolean startBot = true;

    public static void main(String[] args) {

        logger.info("HalfminerBot v{} is starting", getVersionStatic());

        BotPasswordConfig config = new PasswordYamlConfig("config.yml", args.length > 0 ? args[0] : "");
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

            // start the bot
            Thread botLaunchThread = new Thread((() -> new HalfminerBot(config)), "bot-launch");
            botLaunchThread.setUncaughtExceptionHandler((t, e) -> logger.error("An uncaught exception has occurred", e));
            botLaunchThread.start();

            synchronized (mainThreadLock) {
                try {
                    mainThreadLock.wait();
                    Thread.sleep(2000L);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    static ComponentHolder getComponentHolder() {
        return instance;
    }

    private static String getVersionStatic() {
        Properties properties = new Properties();
        try {
            InputStream inputStream = HalfminerBot.class.getClassLoader().getResourceAsStream("metadata.properties");
            if (inputStream != null) {
                properties.load(inputStream);
            } else {
                return null;
            }

        } catch (IOException e) {
            return null;
        }
        return properties.getProperty("version");
    }

    // -- Static End -- //

    private final BotPasswordConfig config;
    private final BotConfig locale = new YamlConfig("locale.yml");
    private final TS3Query query;

    private Scheduler scheduler;
    private TS3ApiWrapper apiWrapper;
    private Storage storage;
    private BotListeners listeners;

    private HalfminerBot(BotPasswordConfig config) {

        instance = this;
        this.config = config;
        if (!locale.reloadConfig()) {
            query = null;
            stop("Couldn't read locale file, quitting...", false);
            return;
        }

        // configure API
        boolean useSSH = config.getBoolean("useSSH");
        String host = config.getString("host");
        int queryPort = config.getInt("ports.queryPort." + (useSSH ? "SSH" : "raw"));
        TS3Query.Protocol protocol = useSSH ? TS3Query.Protocol.SSH : TS3Query.Protocol.RAW;
        TS3Config apiConfig = new TS3Config()
                .setEnableCommunicationsLogging(true)
                .setHost(host)
                .setLoginCredentials(config.getString("credentials.username"), config.getPassword())
                .setProtocol(protocol)
                .setQueryPort(queryPort);

        if (host.equals("localhost") || config.getBoolean("isWhitelisted")) {
            apiConfig.setFloodRate(TS3Query.FloodRate.UNLIMITED);
        } else {
            logger.info("Command rate is reduced, set isWhitelisted to true in config or connect via localhost");
        }

        apiConfig.setReconnectStrategy(ReconnectStrategy.exponentialBackoff());
        apiConfig.setConnectionHandler(new ConnectionHandler() {

            @Override
            public void onConnect(TS3Api ts3Api) {
                try {
                    apiWrapper = new TS3ApiWrapper(ts3Api);
                    startBot();
                    apiWrapper.setTS3Api(query.getApi());
                } catch (Exception e) {
                    logger.info("An error has occurred while connecting to the server", e);
                }
            }

            @Override
            public void onDisconnect(TS3Query ts3Query) {
                if (scheduler != null) {
                    scheduler.shutdown();
                }
            }
        });

        // connect to query
        query = new TS3Query(apiConfig);
        try {
            logger.info("Trying to connect to query interface via {} on port {}", protocol.name(), queryPort);
            query.connect();
        } catch (TS3ConnectionFailedException e) {
            stop("Failed to connect, reason: " + e.getMessage(), false);
        }
    }

    private void startBot() {

        TS3Api api = apiWrapper.getTS3Api();
        try {
            api.selectVirtualServerByPort(config.getInt("ports.serverPort"));
        } catch (TS3CommandFailedException e) {
            stop("The provided server port is not valid, quitting...", false);
            return;
        }

        String nickName = config.getString("botName");
        if (!setNickname(nickName)) {
            boolean nicknameWasSet = false;
            for (int i = 1; i < 10; i++) {
                if (setNickname(nickName + i)) {
                    nickName = nickName + i;
                    logger.warn("The provided botname is already in use or invalid, using {} as nickname", nickName);
                    nicknameWasSet = true;
                    break;
                }
            }

            if (!nicknameWasSet) {
                stop("The provided botname is invalid or already in use, quitting...", false);
                return;
            }
        }

        // move bot into channel
        boolean channelMoveSuccess = false;
        try {
            List<Channel> channels = api.getChannelsByName(config.getString("botChannelName"));
            if (!channels.isEmpty()) {
                api.moveQuery(channels.get(0));
                channelMoveSuccess = true;
            }
        } catch (TS3CommandFailedException ignored) {}

        if (!channelMoveSuccess) {
            logger.warn("The provided channelname does not exist or can't be accessed, staying in default channel");
        }

        if (scheduler == null) {
            scheduler = new ExecutorScheduler();
        } else {
            scheduler.createNewThreadPool();
        }

        if (storage == null) {
            this.storage = new DefaultStorage();
        } else {
            storage.doFullReload();
        }

        if (listeners != null) {
            api.removeTS3Listeners(listeners);
        }

        listeners = new BotListeners();
        api.addTS3Listeners(listeners);
        api.registerAllEvents();

        scheduler.registerAllTasks();

        logger.info("HalfminerBot connected successfully and ready as {}", nickName);
    }

    private boolean setNickname(String nickName) {
        try {
            apiWrapper.getTS3Api().setNickname(nickName);
            return true;
        } catch (TS3CommandFailedException e) {
            return false;
        }
    }

    @Override
    public boolean reloadConfig() {

        boolean localeReloaded = locale.reloadConfig();
        if (config.reloadConfig()) {
            scheduler.configWasReloaded();
            storage.configWasReloaded();
            logger.info("Config file was reloaded");
            return true;
        } else {
            return localeReloaded;
        }
    }

    @Override
    public void stop(String message, boolean restart) {

        logger.info(message.length() > 0 ? message : "Bot quitting...");

        if (scheduler != null) {
            scheduler.shutdown();
        }

        if (storage != null) {
            storage.saveData();
        }

        if (query != null && query.isConnected()) {
            query.exit();
        }

        synchronized (mainThreadLock) {
            startBot = restart;
            mainThreadLock.notify();
        }
    }

    @Override
    public String getVersion() {
        return HalfminerBot.getVersionStatic();
    }

    @Override
    public StateHolder getStateHolder() {
        return this;
    }

    @Override
    public BotPasswordConfig getConfig() {
        return config;
    }

    @Override
    public BotConfig getLocale() {
        return locale;
    }

    @Override
    public Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    public TS3ApiWrapper getApiWrapper() {
        return apiWrapper;
    }

    @Override
    public Storage getStorage() {
        return storage;
    }
}
