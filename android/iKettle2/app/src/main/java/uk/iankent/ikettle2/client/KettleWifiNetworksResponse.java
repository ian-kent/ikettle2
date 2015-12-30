package uk.iankent.ikettle2.client;

import android.util.Log;

import java.nio.charset.Charset;
import java.util.ArrayList;

/**
 * Created by iankent on 27/12/2015.
 */
public class KettleWifiNetworksResponse implements KettleResponse {
    protected ArrayList<String> SSIDs = new ArrayList<String>();

    public static KettleWifiNetworksResponse fromBytes(byte[] b) {
        KettleWifiNetworksResponse ksr = new KettleWifiNetworksResponse();

        String s = new String(b, 0, b.length, Charset.forName("ASCII"));
        String[] networks = s.split("\\}");
        for(int i = 0; i < networks.length; i++) {
            String[] network = networks[i].split(",", 2);
            ksr.SSIDs.add(network[0]);
        }

        return ksr;
    }

    public ArrayList<String> getSSIDs() {
        return SSIDs;
    }
}
