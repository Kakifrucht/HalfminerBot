package de.halfminer.hmbot;

import com.github.theholywaffle.teamspeak3.TS3Api;
import de.halfminer.hmbot.config.YamlConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base inheritance class, keeping references to commonly used object instances.
 */
public class HalfminerBotClass {

    protected final static HalfminerBot bot = HalfminerBot.getInstance();
    protected final static YamlConfig config = bot.getBotConfig();
    protected final static TS3Api api = bot.getApi();

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
}
