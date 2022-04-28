package de.halfminer.hmbot.cmd;

import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import de.halfminer.hmbot.storage.HalfClient;
import de.halfminer.hmbot.util.StringArgumentSeparator;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Access to {@link Command command's} class name and their usage and description key for messaging.
 */
enum CommandEnum {

    ADMIN       (CmdAdmin.class),
    BROADCAST   (CmdBroadcast.class),
    CHANNEL     (CmdChannel.class),
    HELP        (CmdHelp.class),
    RANK        (CmdRank.class);

    private final Class<?> aClass;

    CommandEnum(Class<?> aClass) {
        this.aClass = aClass;
    }

    Command getInstance(HalfClient client, ClientInfo clientInfo, StringArgumentSeparator command) throws InvalidCommandException {
        try {
            return (Command) aClass
                    .getConstructor(HalfClient.class, ClientInfo.class, StringArgumentSeparator.class)
                    .newInstance(client, clientInfo, command);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof InvalidCommandException) {
                throw (InvalidCommandException) e.getCause();
            }
            throw new InvalidCommandException(this, e.getCause());
        } catch (Throwable e) {
            throw new InvalidCommandException(this, e);
        }
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

    private final static Map<String, CommandEnum> aliases;

    static {
        aliases = new HashMap<>();
        putAliases(ADMIN, "bot", "hmbot", "halfminer", "hm");
        putAliases(BROADCAST, "bc");
        putAliases(CHANNEL, "c", "create");
        putAliases(HELP, "h", "?", "version", "ver", "hilfe");
        putAliases(RANK, "rang", "premium", "vip", "freischalten", "pin");
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
