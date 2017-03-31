package de.halfminer.hmbot.cmd;

import java.util.HashMap;
import java.util.Map;

/**
 * Access to {@link Command command's} class name and their usage and description key for messaging.
 */
public enum CommandEnum {

    ADMIN,
    BROADCAST,
    CHANNEL,
    HELP;

    String getReflectionPath() {
        return "de.halfminer.hmbot.cmd." + "Cmd" + getNameFirstUppercase();
    }

    String getPermission() {
        return "cmd." + name().toLowerCase();
    }

    String getUsageKey() {
        return "cmd" + getNameFirstUppercase() + "Usage";
    }

    String getDescriptionKey() {
        return "cmd" + getNameFirstUppercase() + "Description";
    }

    private String getNameFirstUppercase() {
        StringBuilder sb = new StringBuilder(name().toLowerCase());
        sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
        return sb.toString();
    }

    private static final Map<String, CommandEnum> aliases;

    static {
        aliases = new HashMap<>();
        putAliases(ADMIN, "bot", "hmbot", "halfminer", "hm");
        putAliases(BROADCAST, "bc");
        putAliases(CHANNEL, "c", "create");
        putAliases(HELP, "h", "?", "version", "ver", "hilfe");
    }

    private static void putAliases(CommandEnum command, String... aliasesToPut) {
        for (String alias : aliasesToPut) {
            aliases.put(alias, command);
        }
    }

    static CommandEnum getCommand(String command) {
        String lowerCommand = command.toLowerCase();
        if (lowerCommand.startsWith("!")) {
            lowerCommand = lowerCommand.substring(1);
        }

        try {
            return valueOf(lowerCommand.toUpperCase());
        } catch (IllegalArgumentException e) {
            return aliases.get(lowerCommand);
        }
    }
}
