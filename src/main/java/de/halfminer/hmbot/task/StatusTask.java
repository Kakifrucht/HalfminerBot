package de.halfminer.hmbot.task;

import de.halfminer.hmbot.util.RESTHelper;
import okhttp3.*;

/**
 * Sends the current user count to REST API.
 * Leave disabled, as the API is private.
 */
class StatusTask extends Task {

    private final OkHttpClient client = new OkHttpClient();
    private boolean lastConnectSuccess = true;

    @Override
    public void execute() {

        Response response = null;
        try {
            String status = "expiry=240&teamspeak=" + getTS3Api().getServerInfo().getClientsOnline();
            Request request = new Request.Builder()
                    .url(RESTHelper.getBaseUrl("storage/status"))
                    .put(RESTHelper.getRequestBody(status))
                    .build();

            response = client.newCall(request).execute();

            if (!response.isSuccessful()) {
                // only log warning once in a row
                if (lastConnectSuccess) {
                    logger.warn("Received response code {} on HTTP PUT of user count", response.code());
                    lastConnectSuccess = false;
                }
            } else lastConnectSuccess = true;
        } catch (Exception e) {
            logger.error("Could not update TeamSpeak status", e);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }
}
