package de.halfminer.hmbot;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3ApiAsync;

import java.util.logging.Logger;

/**
 * Created by fabpw on 15.03.2017.
 */
public class HalfminerBotClass {

    protected final static HalfminerBot bot = HalfminerBot.getInstance();
    protected final static Logger logger = HalfminerBot.getLogger();
    protected final static TS3Api api = bot.getApi();
    protected final static TS3ApiAsync apiAsync = bot.getApiAsync();
}
