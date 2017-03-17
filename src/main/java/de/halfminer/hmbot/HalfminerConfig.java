package de.halfminer.hmbot;

import de.halfminer.hmbot.exception.NoConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

public class HalfminerConfig {

    private final static Logger logger = LoggerFactory.getLogger(HalfminerConfig.class);

    private final File configFile;
    private Properties properties;

    private String password;

    public HalfminerConfig() throws NoConfigurationException {

        this.configFile = new File("hmbot/halfminerbot.cfg");
        if (!configFile.exists()) {
            createDefaultConfig();
            logger.error("Config file halfminerbot.cfg created, please fill out your details and restart the bot.");
            throw new NoConfigurationException();
        } else readConfig();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getBotName() {
        return properties.getProperty("name");
    }

    public String getChannelMoveName() {
        return properties.getProperty("channelname");
    }

    public String getHost() {
        return properties.getProperty("host");
    }

    public int getPort() {
        return Integer.parseInt(properties.getProperty("port"));
    }

    public int getChannelAdminID() {
        return Integer.parseInt(properties.getProperty("channeladminid"));
    }

    public int getWaitingParamTimeInSeconds() {
        return Integer.parseInt(properties.getProperty("waitingparaminseconds"));
    }

    public boolean hasLocalhost() {
        return properties.get("host").equals("localhost");
    }

    private void readConfig() {
        this.properties = new Properties();
        try (FileReader reader = new FileReader(configFile)) {
            properties.load(reader);
            verifyConfig();
        } catch (FileNotFoundException e) {
            //This can't usually happen, as we already checked if the file actually exists
            e.printStackTrace();
        } catch (IOException e) {
            //Config could not be read somehow
            logger.warn("Config file could not be read. Renamed to halfminerbotinvalid.cfg and regenerated config.");
            File oldConfig = new File("halfminerbotinvalid.cfg");
            if (!(configFile.renameTo(oldConfig)))
                logger.warn("Old config could not be saved.");
            createDefaultConfig(); // regenerate
        }

    }

    private void createDefaultConfig() {

        logger.info("Generating default config halfminerbot.cfg");
        this.properties = new Properties();
        properties.setProperty("name", "HalfminerBot");
        properties.setProperty("host", "localhost");
        properties.setProperty("port", "9987");
        properties.setProperty("channelname", "Welcome");
        properties.setProperty("channeladminid", "1");
        properties.setProperty("waitingparaminseconds", "60");
        writeConfig();
    }

    private void writeConfig() {
        if (properties.containsKey("name")) {
            try (FileWriter writer = new FileWriter(configFile)) {
                properties.store(writer, "HalfminerBot Settings");
                writer.close();
            } catch (IOException e) {
                logger.warn("Config file could not be written.");
                e.printStackTrace();
            }
        }
    }

    private void verifyConfig() {

        Properties toVerify = this.properties;
        this.properties = new Properties();

        if (!toVerify.containsKey("host")) {
            logger.warn("Configuration: host not given. Setting default value localhost");
            properties.put("host", "localhost");
        } else properties.put("host", toVerify.getProperty("host"));

        if (!toVerify.containsKey("name")) {
            logger.warn("Configuration: name not given. Setting default value HalfminerBot");
            properties.put("name", "HalfminerBot");
        } else properties.put("name", toVerify.getProperty("name"));

        if (!toVerify.containsKey("port")) {
            logger.warn("Configuration: port not given. Setting default value 9987");
            properties.put("port", "9987");
        } else {

            String toPut = toVerify.getProperty("port");
            int checkRange = 9987;
            try {
                checkRange = Integer.parseInt(toPut);
            } catch (NumberFormatException e) {
                logger.warn("Configuration: port in wrong format. Setting default value 9987");
                toPut = "9987";
            }

            if (checkRange > 65535 || checkRange < 0) {
                logger.warn("Configuration: port in wrong format. Setting default value 9987");
                toPut = "9987";
            }

            properties.put("port", toPut);

        }
        if (!toVerify.containsKey("channelname")) {
            logger.warn("Configuration: channelname not given. Setting default value Welcome");
            properties.put("channelname", "Welcome");
        } else properties.put("channelname", toVerify.getProperty("channelname"));

        if (!toVerify.containsKey("channeladminid")) {
            logger.warn("Configuration: channeladminid not given. Setting default value 1");
            properties.put("channeladminid", "1");
        } else {
            String toPut = toVerify.getProperty("channeladminid");
            int checkRange = 1;
            try {
                checkRange = Integer.parseInt(toPut);
            } catch (NumberFormatException e) {
                logger.warn("Configuration: channeladminid in wrong format. Setting default value 1");
                toPut = "1";
            }

            if (checkRange < 0) {
                logger.warn("Configuration: channeladminid in wrong format. Setting default value 1");
                toPut = "1";
            }
            properties.put("channeladminid", toPut);
        }

        if (!toVerify.containsKey("waitingparaminseconds")) {
            logger.warn("Configuration: waitingparaminseconds not given. Setting default value 60");
            properties.put("waitingparaminseconds", "60");
        } else {
            String toPut = toVerify.getProperty("waitingparaminseconds");
            int checkRange = 60;
            try {
                checkRange = Integer.parseInt(toPut);
            } catch (NumberFormatException e) {
                logger.warn("Configuration: waitingparaminseconds in wrong format. Setting default value 60");
                toPut = "60";
            }

            if (checkRange < 0) {
                logger.warn("Configuration: waitingparaminseconds in wrong format. Setting default value 60");
                toPut = "60";
            }
            properties.put("waitingparaminseconds", toPut);
        }

        writeConfig();
    }

}
