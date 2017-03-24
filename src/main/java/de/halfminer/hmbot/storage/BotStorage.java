package de.halfminer.hmbot.storage;

import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import de.halfminer.hmbot.HalfminerBotClass;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class BotStorage extends HalfminerBotClass {

    private final Map<Integer, HalfClient> clientsOnline = new ConcurrentHashMap<>();

    public BotStorage() {
        // add already online clients
        for (Client client : api.getClients()) {
            if (client.isRegularClient()) {
                clientJoined(client.getDatabaseId(), client.getId());
            }
        }

        // check for dead client objects every 10 minutes
        scheduler.scheduleRunnable(new Runnable() {
            @Override
            public void run() {
                Iterator<HalfClient> it = clientsOnline.values().iterator();
                while (it.hasNext()) {
                    if (it.next().canBeEvicted()) {
                        it.remove();
                    }
                }
            }
        }, 0, 10, TimeUnit.MINUTES);
    }

    public void clientJoined(int clientDatabaseId, int clientId) {
        if (clientsOnline.containsKey(clientDatabaseId)) {
            clientsOnline.get(clientDatabaseId).updateClientId(clientId);
        } else {
            clientsOnline.put(clientDatabaseId, new HalfClient(clientId));
        }
    }

    public HalfClient getClient(int clientId) {
        return clientsOnline.get(api.getClientInfo(clientId).getDatabaseId());
    }
}
