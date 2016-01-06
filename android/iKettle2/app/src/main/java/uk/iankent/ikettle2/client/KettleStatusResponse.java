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

    public enum WaterLevelState {
        Empty,
        Low,
        Half,
        Full,
        Overfilled
    }

    protected State status;
    protected int temperature;
    protected int waterLevel;
    protected WaterLevelState waterLevelStatus;

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

        if(b.length > 3) {
            // b[2] + b[3] = waterlevel
            ksr.waterLevel = (b[2] << 8) + b[3];
        }

        if (ksr.waterLevel < 130) {
            ksr.waterLevelStatus = WaterLevelState.Empty;
        } else if (ksr.waterLevel < 163) {
            ksr.waterLevelStatus = WaterLevelState.Low;
        } else if (ksr.waterLevel < 203) {
            ksr.waterLevelStatus = WaterLevelState.Half;
        } else if (ksr.waterLevel < 270) {
            ksr.waterLevelStatus = WaterLevelState.Full;
        } else {
            ksr.waterLevelStatus = WaterLevelState.Overfilled;
        }

        // b[4] unknown?
        Log.d("KettleStatusResponse", b.toString());

        return ksr;
    }

    public State getStatus() {
        return status;
    }

    public WaterLevelState getWaterLevelStatus() { return waterLevelStatus; }

    public int getWaterLevel() { return waterLevel; }

    public int getTemperature() {
        return temperature;
    }
}
