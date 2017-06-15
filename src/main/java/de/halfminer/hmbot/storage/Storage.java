package de.halfminer.hmbot.storage;

import com.github.theholywaffle.teamspeak3.api.wrapper.Client;

import java.util.Map;

/**
 * Interface for storage and state related functionality.
 */
public interface Storage {

    void doFullReload();

    void configWasReloaded();

    void clientJoinedOrReloaded(Client client);

    void clientLeft(int clientId);

    HalfClient getClient(Client client);

    Map<Client, HalfClient> getOnlineClients();

    void saveData();
}
