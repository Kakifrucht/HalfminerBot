package de.halfminer.hmbot;

import com.github.theholywaffle.teamspeak3.TS3Api;
import de.halfminer.hmbot.config.BotPasswordConfig;
import de.halfminer.hmbot.task.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base inheritance class, keeping references to project wide commonly used object instances.
 */
public class BotClass {

    protected final ComponentHolder componentHolder = HalfminerBot.getComponentHolder();
    protected final BotPasswordConfig config = componentHolder.getConfig();
    protected final Scheduler scheduler = componentHolder.getScheduler();
    protected final TS3Api api = componentHolder.getApi();

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
}
