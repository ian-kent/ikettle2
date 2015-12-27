package uk.iankent.ikettle2.client;

import android.util.Log;

/**
 * Created by iankent on 27/12/2015.
 */
public class KettleStatusResponse implements KettleResponse {
    public enum State {
        Ready,
        Boiling,
        KeepWarm,
        Finished,
        Cooling
    }

    protected State status;
    protected int temperature;

    public static KettleStatusResponse fromBytes(byte[] b) {
        KettleStatusResponse ksr = new KettleStatusResponse();

        switch (b[0]) {
            case 0:
                ksr.status = State.Ready;
                break;
            case 1:
                ksr.status = State.Boiling;
                break;
            case 2:
                ksr.status = State.KeepWarm;
                break;
            case 3:
                ksr.status = State.Finished;
                break;
            case 4:
                ksr.status = State.Cooling;
                break;
        }

        ksr.temperature = (int)b[1];

        // b[2] + b[3] = waterlevel
        // b[4] unknown?
        Log.d("KettleStatusResponse", b.toString());

        return ksr;
    }

    public State getStatus() {
        return status;
    }

    public int getTemperature() {
        return temperature;
    }
}
