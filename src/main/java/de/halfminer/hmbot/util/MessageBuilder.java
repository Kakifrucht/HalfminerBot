package de.halfminer.hmbot.util;

import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import de.halfminer.hmbot.HalfminerBotClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Class containing a builder used for placeholder replacements and messaging.
 */
@SuppressWarnings("ALL")
public class MessageBuilder extends HalfminerBotClass {

    private final static Logger logger = LoggerFactory.getLogger(MessageBuilder.class);

    public static MessageBuilder create(String lang) {
        return new MessageBuilder(lang);
    }

    public static String returnMessage(String lang) {
        return create(lang).returnMessage();
    }

    private final static char PLACEHOLDER_CHARACTER = '%';

    private final String lang;
    private final Map<String, String> placeholders = new HashMap<>();

    private boolean getFromLocale = true;

    private MessageBuilder(String lang) {
        this.lang = lang;
    }

    public MessageBuilder setDirectString() {
        getFromLocale = false;
        return this;
    }

    /**
     * Adds a placeholder and what to replace it with to the message. The {@link #PLACEHOLDER_CHARACTER} will be stripped.
     *
     * @param placeholder String to replace
     * @param replaceWith String with what to replace with
     * @return MessageBuilder, same instance
     */
    public MessageBuilder addPlaceholderReplace(String placeholder, String replaceWith) {
        placeholders.put(placeholder.replaceAll(PLACEHOLDER_CHARACTER + "", "").trim(), replaceWith);
        return this;
    }

    private String returnMessage() {

        String toReturn;
        if (getFromLocale) {
            toReturn = getMessage(lang);
            // allow removal of messages
            if (toReturn == null || toReturn.length() == 0)
                return "";
        } else {
            toReturn = lang;
        }

        return placeholderReplace(toReturn);
    }

    public void sendMessage(int clientId) {
        String messageToSend = returnMessage();
        if (messageToSend.length() > 0 || !getFromLocale) {
            api.sendPrivateMessage(clientId, returnMessage());
        }
    }

    public void broadcastMessage(boolean log) {
        broadcastMessage(log, 0);
    }

    public void broadcastMessage(boolean log, int minimumTalkPower) {
        String messageToBroadcast = returnMessage();
        if (messageToBroadcast.length() > 0) {
            for (Client client : api.getClients()) {
                if (client.getTalkPower() >= minimumTalkPower) {
                    api.sendPrivateMessage(client.getId(), messageToBroadcast);
                }
            }
        }

        if (log) {
            logger.info(messageToBroadcast);
        }
    }

    private String getMessage(String messageKey) {
        return bot.getLocale().getString(messageKey);
    }

    /**
     * Replace the placeholders from a given string with the given placeholders. The String will only be iterated once,
     * so the performance of this algorithm lies within O(n). Placeholders must start and end with character '%'.
     *
     * @param toReplace string containing the message that contains the placeholders
     * @return String containing the finished replaced message
     */
    private String placeholderReplace(String toReplace) {

        if (placeholders.size() == 0) return toReplace;

        StringBuilder message = new StringBuilder(toReplace);
        StringBuilder placeholder = new StringBuilder();
        for (int i = 0; i < message.length(); i++) {

            // Go into placeholder read mode
            if (message.charAt(i) == PLACEHOLDER_CHARACTER) {

                // get the placeholder
                for (int j = i + 1; j < message.length() && message.charAt(j) != '%'; j++) {
                    placeholder.append(message.charAt(j));
                }

                // Do the replacement, add length of string to the outer loop index, since we do not want to iterate over
                // it again, or if no replacement was found, add the length of the read placeholder to skip it
                if (placeholders.containsKey(placeholder.toString())) {
                    String replaceWith = placeholders.get(placeholder.toString());
                    message.replace(i, i + placeholder.length() + 2, replaceWith);
                    i += replaceWith.length() - 1;
                } else i += placeholder.length();

                placeholder.setLength(0);
            }
        }
        return message.toString();
    }
}