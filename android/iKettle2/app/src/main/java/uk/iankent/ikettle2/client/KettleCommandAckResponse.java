package uk.iankent.ikettle2.client;

import android.util.Log;

/**
 * Created by iankent on 27/12/2015.
 */
public class KettleCommandAckResponse implements KettleResponse {
    public static KettleCommandAckResponse fromBytes(byte[] b) {
        KettleCommandAckResponse kca = new KettleCommandAckResponse();

        Log.d("KettleCommandAck", b.toString());

        return kca;
    }
}
