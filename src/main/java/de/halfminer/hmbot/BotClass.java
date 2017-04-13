package de.halfminer.hmbot;

import com.github.theholywaffle.teamspeak3.TS3Api;
import de.halfminer.hmbot.config.YamlConfig;
import de.halfminer.hmbot.task.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base inheritance class, keeping references to commonly used object instances.
 */
public class BotClass {

    protected final HalfminerBot bot = HalfminerBot.getInstance();
    protected final YamlConfig config = bot.getConfig();
    protected final Scheduler scheduler = bot.getScheduler();
    protected final TS3Api api = bot.getApi();

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
}
