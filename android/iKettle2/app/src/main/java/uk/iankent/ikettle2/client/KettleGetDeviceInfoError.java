package uk.iankent.ikettle2.client;

import java.io.IOException;

/**
 * Created by iankent on 31/12/2015.
 */
public class KettleGetDeviceInfoError extends KettleError {
    public KettleGetDeviceInfoError(IOException e) {
        super(e);
    }
}
