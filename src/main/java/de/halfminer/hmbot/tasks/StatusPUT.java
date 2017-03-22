package de.halfminer.hmbot.tasks;

import de.halfminer.hmbot.HalfminerBotClass;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Sends the current user count to REST API.
 */
public class StatusPUT extends HalfminerBotClass implements Runnable {

    private boolean lastConnectSuccess = true;

    @Override
    public void run() {

        try {
            URL api = new URL("https://api.halfminer.de/storage/status");

            HttpURLConnection connection = (HttpURLConnection) api.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
            out.write("expiry=240&teamspeak=" + apiAsync.getServerInfo().get().getClientsOnline());
            out.close();

            int responseCode = connection.getResponseCode();
            if (responseCode >= 300 || responseCode < 200) {
                if (lastConnectSuccess) {
                    logger.error("Received response code {} on HTTP PUT of user count", responseCode);
                    lastConnectSuccess = false;
                }
            } else lastConnectSuccess = true;
        } catch (Throwable e) {
            logWarning(e);
        }
    }

    private void logWarning(Throwable toLog) {
        logger.error("Could not update Teamspeak status", toLog);
    }
}
