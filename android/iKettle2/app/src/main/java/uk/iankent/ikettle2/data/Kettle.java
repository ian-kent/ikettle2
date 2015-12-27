package uk.iankent.ikettle2.data;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by iankent on 27/12/2015.
 */
public class Kettle implements Parcelable {
    public String Host;
    public Integer Port;

    public Kettle(String host, Integer port) {
        this.Host = host;
        this.Port = port;
    }

    /* everything below here is for implementing Parcelable */

    // 99.9% of the time you can just ignore this
    @Override
    public int describeContents() {
        return 0;
    }

    // write your object's data to the passed-in Parcel
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(Host);
        out.writeInt(Port);
    }

    // this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<Kettle> CREATOR = new Parcelable.Creator<Kettle>() {
        public Kettle createFromParcel(Parcel in) {
            return new Kettle(in);
        }

        public Kettle[] newArray(int size) {
            return new Kettle[size];
        }
    };

    // example constructor that takes a Parcel and gives you an object populated with it's values
    private Kettle(Parcel in) {
        Host = in.readString();
        Port = in.readInt();
    }
}
