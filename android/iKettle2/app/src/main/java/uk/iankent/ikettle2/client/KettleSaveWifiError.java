package uk.iankent.ikettle2.client;

import java.io.IOException;

/**
 * Created by iankent on 29/12/2015.
 */
public class KettleSaveWifiError extends KettleError {
    public KettleSaveWifiError(IOException e) {
        super(e);
    }
}
