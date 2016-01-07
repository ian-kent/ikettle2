package uk.iankent.ikettle2.client;

import android.util.Log;

/**
 * Created by iankent on 07/01/2016.
 */
public class KettleAutoDacResponse implements KettleResponse {
    protected int offBaseWeight = 0;

    public KettleAutoDacResponse(int offBaseWeight) {
        this.offBaseWeight = offBaseWeight;
    }

    public int getOffBaseWeight() {
        return offBaseWeight;
    }
}
