package uk.iankent.ikettle2.client;

import java.io.IOException;

/**
 * Created by iankent on 29/12/2015.
 */
public class KettleError extends KettleStatusResponse {
    protected IOException exception;

    public KettleError() {}
    public KettleError(IOException e) {
        this.exception = e;
    }

    public IOException getException() {
        return exception;
    }
}