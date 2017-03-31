package de.halfminer.hmbot.config;

/**
 * Custom password field that is kept after reloads, or read from config if not supplied.
 */
@SuppressWarnings("SameParameterValue")
public class PasswordYamlConfig extends YamlConfig {

    private String password;
    private final boolean hasCustomPassword;

    public PasswordYamlConfig(String fileName, String password) {
        super(fileName);
        this.password = password;
        hasCustomPassword = password.length() > 0;
    }

    @Override
    public boolean reloadConfig() {
        boolean toReturn = super.reloadConfig();
        if (!hasCustomPassword) {
            password = getString("credentials.password");
        }
        return toReturn;
    }

    public String getPassword() {
        return password;
    }
}
