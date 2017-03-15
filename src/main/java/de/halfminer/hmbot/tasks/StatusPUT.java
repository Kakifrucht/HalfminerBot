package de.halfminer.hmbot.tasks;

import de.halfminer.hmbot.HalfminerBotClass;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

/**
 * Created by fabpw on 15.03.2017.
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
            if (lastConnectSuccess && (responseCode >= 300 || responseCode < 200)) {
                logger.warning("Received response code " + responseCode + " on HTTP PUT of user count");
                lastConnectSuccess = false;
            } else lastConnectSuccess = true;
        } catch (Exception e) {
            logWarning(e);
        }
    }

    private void logWarning(Exception toLog) {
        logger.log(Level.WARNING, "Could not update Teamspeak status", toLog);
    }
}
