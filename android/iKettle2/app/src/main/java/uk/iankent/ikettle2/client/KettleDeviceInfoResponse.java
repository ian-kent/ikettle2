package uk.iankent.ikettle2.client;

import android.util.Log;

/**
 * Created by iankent on 31/12/2015.
 */
public class KettleDeviceInfoResponse implements KettleResponse {
    protected String hostname;
    protected Integer port;

    public KettleDeviceInfoResponse() {
        this("");
    }
    public KettleDeviceInfoResponse(String host) {
        this(host, 2081);
    }
    public KettleDeviceInfoResponse(String host, Integer port) {
        this.hostname = host;
        this.port = port;
    }

    public static KettleDeviceInfoResponse fromBytes(byte[] b) {
        KettleDeviceInfoResponse kca = new KettleDeviceInfoResponse();

        Log.d("KettleDeviceInfo", b.toString());

        return kca;
    }

    public String getHostname() {
        return hostname;
    }

    public Integer getPort() {
        return port;
    }
}
