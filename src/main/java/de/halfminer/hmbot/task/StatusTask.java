package de.halfminer.hmbot.task;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Sends the current user count to REST API.
 */
class StatusTask extends Task {

    private boolean lastConnectSuccess = true;

    StatusTask() {
        super(0, 2, TimeUnit.MINUTES);
    }

    @Override
    boolean checkIfEnabled() {
        return true;
    }

    @Override
    public void execute() {

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
