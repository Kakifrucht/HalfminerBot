package de.halfminer.hmbot.task;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Sends the current user count to REST API.
 */
class StatusTask extends Task {

    private boolean lastConnectSuccess = true;

    @Override
    boolean checkIfEnabled() {
        return true;
    }

    @Override
    public void execute() {

        try {
            URL apiURL = new URL("https://api.halfminer.de/storage/status");

            HttpURLConnection connection = (HttpURLConnection) apiURL.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
            out.write("expiry=240&teamspeak=" + api.getServerInfo().getClientsOnline());
            out.close();

            int responseCode = connection.getResponseCode();
            if (responseCode >= 300 || responseCode < 200) {
                // only log warning once in a row
                if (lastConnectSuccess) {
                    logger.warn("Received response code {} on HTTP PUT of user count", responseCode);
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
