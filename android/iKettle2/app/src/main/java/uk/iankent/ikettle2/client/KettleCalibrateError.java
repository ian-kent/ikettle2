package uk.iankent.ikettle2.client;

import java.io.IOException;

/**
 * Created by iankent on 07/01/2016.
 */
public class KettleCalibrateError extends KettleError {
    public KettleCalibrateError(IOException e) {
        super(e);
    }
}