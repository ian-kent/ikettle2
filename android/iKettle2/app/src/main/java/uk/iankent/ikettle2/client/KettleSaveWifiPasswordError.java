package uk.iankent.ikettle2.client;

import java.io.IOException;

/**
 * Created by iankent on 29/12/2015.
 */
public class KettleSaveWifiPasswordError extends KettleError {
    public KettleSaveWifiPasswordError(IOException e) {
        super(e);
    }
}
