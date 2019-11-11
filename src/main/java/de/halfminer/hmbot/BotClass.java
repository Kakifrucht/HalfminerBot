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

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected final ComponentHolder componentHolder = HalfminerBot.getComponentHolder();
    protected final BotPasswordConfig config = componentHolder.getConfig();
    protected final Scheduler scheduler = componentHolder.getScheduler();

    private final TS3ApiWrapper apiWrapper = componentHolder.getApiWrapper();

    protected TS3Api getTS3Api() {
        return apiWrapper.getTS3Api();
    }
}