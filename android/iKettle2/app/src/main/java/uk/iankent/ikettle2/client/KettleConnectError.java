package uk.iankent.ikettle2.client;

import java.io.IOException;

/**
 * Created by iankent on 29/12/2015.
 */
public class KettleConnectError extends KettleError {
    public KettleConnectError(IOException e) {
        super(e);
    }
}
