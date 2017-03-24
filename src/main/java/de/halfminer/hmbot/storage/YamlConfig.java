package de.halfminer.hmbot.storage;

import de.halfminer.hmbot.util.StringArgumentSeparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Map;

/**
 * Class loading and writing yaml based configuration file from disk.
 * Uses {@link org.yaml.snakeyaml.Yaml SnakeYAML} for parsing.
 */
public class YamlConfig {

    private static final Logger logger = LoggerFactory.getLogger(YamlConfig.class);

    private final File configFile = new File("hmbot/config.yml");
    private final Yaml yaml = new Yaml();
    private Map<String, Object> yamlParsed;
    private long lastModified;

    public YamlConfig() throws ConfigurationException {
        this("");
    }

    public YamlConfig(String password) throws ConfigurationException {

        if (configFile.exists()) {
            loadYaml(password);
            logger.info("Configuration loaded successfully");
        } else {

            try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("config.yml");
                 OutputStream outputStream = new FileOutputStream(configFile)) {

                byte[] buffer = new byte[1024];
                int read;
                while ((read = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, read);
                }

                logger.info("Config file was written to {}, please fill it out and restart the bot",
                        configFile.getAbsolutePath());

            } catch (Exception e) {
                throw new ConfigurationException("Couldn't write config file", e);
            }

            throw new ConfigurationException();
        }
    }

    private void loadYaml(String password) throws ConfigurationException {

        lastModified = configFile.lastModified();

        Object loaded;
        try (FileInputStream stream = new FileInputStream(configFile)) {
            loaded = yaml.load(stream);
        } catch (Exception e) {
            throw new ConfigurationException("Could not read config file", e);
        }

        if (loaded instanceof Map) {
            try {
                //noinspection unchecked
                yamlParsed = (Map) loaded;
            } catch (ClassCastException e) {
                throw new ConfigurationException("Config file is in invalid format", e);
            }

            if (password.length() > 0) {
                yamlParsed.put("password", password);
            } else if (getString("password", "").length() == 0) {
                throw new ConfigurationException("No password was set");
            }

        } else {
            throw new ConfigurationException("Config file is in invalid format");
        }
    }

    /**
     * Reloads the configuration file. Will only run if file was modified since last
     * reload and won't reload if configuration is broken.
     *
     * @return true if reload was successful, false if not modified or {@link ConfigurationException} was thrown
     */
    public boolean reloadConfig() {

        if (configFile.lastModified() == lastModified) {
            lastModified = configFile.lastModified();
            return false;
        }

        Map<String, Object> oldParsed = yamlParsed;
        try {
            loadYaml(getString("password", ""));
            return true;
        } catch (ConfigurationException e) {
            logger.warn(e.getMessage(), e);
            yamlParsed = oldParsed;
            return false;
        }
    }

    public int getInt(String path, int returnIfNull) {
        Object toGet = get(path);
        if (toGet instanceof Integer) return (int) toGet;
        else return returnIfNull;
    }

    public String getString(String path, String returnIfNull) {
        Object toGet = get(path);
        if (toGet != null) return toGet.toString();
        else return returnIfNull;
    }

    private Object get(String path) {

        StringArgumentSeparator separator = new StringArgumentSeparator(path, '.');

        Map<?, ?> currentSection = yamlParsed;
        int currentIndex = 0;
        while (separator.meetsLength(currentIndex + 2)) {
            Object checkIfSubsection = currentSection.get(separator.getArgument(currentIndex));
            if (checkIfSubsection instanceof Map) {
                currentSection = (Map) checkIfSubsection;
            } else {
                return null;
            }
            currentIndex++;
        }

        return currentSection.get(separator.getArgument(currentIndex));
    }
}
