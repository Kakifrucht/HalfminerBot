package de.halfminer.hmbot.config;

import de.halfminer.hmbot.util.StringArgumentSeparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Collections;
import java.util.Map;

/**
 * Class loading and parsing yaml based configuration file from disk.
 * Uses {@link org.yaml.snakeyaml.Yaml SnakeYAML} for parsing.
 */
public class YamlConfig implements BotConfig {

    private static final Logger logger = LoggerFactory.getLogger(YamlConfig.class);

    private final String fileName;
    private final File configFile;

    private final Yaml yamlParser = new Yaml();

    private Map<String, Object> defaultParsed;
    private Map<String, Object> configParsed;

    private long lastModified;
    private boolean isUsingDefaultConfig = false;

    public YamlConfig(String fileName) {
        this.fileName = fileName;
        configFile = new File("hmbot/", fileName);
    }

    @Override
    public boolean reloadConfig() {

        if (configFile.exists() && configFile.lastModified() == lastModified) {
            logger.info("Configuration {} was not reloaded, as it wasn't modified", configFile.getName());
            return false;
        }

        Map<String, Object> oldParsed = configParsed;
        try {
            loadYaml();
            return true;
        } catch (ConfigurationException e) {
            logger.warn(e.getMessage(), e);
            configParsed = oldParsed;
            return false;
        }
    }

    @Override
    public boolean isUsingDefaultConfig() {
        return isUsingDefaultConfig;
    }

    private void loadYaml() throws ConfigurationException {

        try {
            defaultParsed = yamlParser.load(this.getClass().getClassLoader().getResourceAsStream(fileName));
        } catch (Exception e) {
            // easiest way to check if format is valid
            throw new ConfigurationException("Default config is not in valid format", e);
        }

        if (!configFile.exists()) {

            isUsingDefaultConfig = true;
            configParsed = defaultParsed;

            try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(fileName);
                 OutputStream outputStream = new FileOutputStream(configFile)) {

                // manually copy file
                byte[] buffer = new byte[1024];
                int read;
                while ((read = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, read);
                }

                logger.info("Config file was written to {}", configFile.getAbsolutePath());
            } catch (Exception e) {
                throw new ConfigurationException("Could not write file to " + configFile.getAbsolutePath(), e);
            }

            return;
        }

        lastModified = configFile.lastModified();

        Object loaded;
        try (FileInputStream stream = new FileInputStream(configFile)) {
            loaded = yamlParser.load(stream);
        } catch (Exception e) {
            throw new ConfigurationException("Could not read config file", e);
        }

        if (loaded == null || loaded instanceof Map) {
            try {
                //noinspection unchecked
                configParsed = loaded != null ? (Map) loaded : Collections.emptyMap();
                logger.info("Configuration file {} loaded successfully", configFile.getName());
            } catch (ClassCastException e) {
                throw new ConfigurationException("Config file is in invalid format", e);
            }

        } else {
            throw new ConfigurationException();
        }
    }

    @Override
    public boolean getBoolean(String path) {
        return (boolean) get(path, Boolean.class);
    }

    @Override
    public int getInt(String path) {
        return (int) get(path, Integer.class);
    }

    @Override
    public String getString(String path) {
        return String.valueOf(get(path, Object.class));
    }

    @Override
    public Object get(String path, Class<?> instanceOf) {

        Object toGet = get(path, configParsed);
        if (toGet != null
                && (instanceOf.equals(String.class) || instanceOf.isAssignableFrom(toGet.getClass()))) {
            return toGet;
        } else {
            if (toGet != null) {
                logger.warn("Value at path {} not instance of {}, falling back to default value(s)",
                        path, instanceOf.getSimpleName());
            }
            // no need to check instance on defaultParsed, as it is part of the classpath
            return get(path, defaultParsed);
        }
    }

    private Object get(String path, Map<String, Object> getFrom) {

        StringArgumentSeparator separator = new StringArgumentSeparator(path, '.');

        Map<?, ?> currentSection = getFrom;
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

    private static class ConfigurationException extends RuntimeException {

        ConfigurationException() {
            super("Config file is in invalid format");
        }

        ConfigurationException(String error, Throwable cause) {
            super(error, cause);
        }
    }
}
