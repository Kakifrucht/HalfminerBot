package de.halfminer.hmbot;

import com.github.theholywaffle.teamspeak3.TS3Api;

/**
 * Wrapping a {@link #ts3Api} class to allow updating the currently active {@link TS3Api} instance,
 * since we use a different instance during connection phase and while the bot is running.
 */
class TS3ApiWrapper {

    private TS3Api ts3Api;


    TS3ApiWrapper(TS3Api ts3Api) {
        this.ts3Api = ts3Api;
    }

    void setTS3Api(TS3Api ts3Api) {
        this.ts3Api = ts3Api;
    }

    TS3Api getTS3Api() {
        return ts3Api;
    }
}