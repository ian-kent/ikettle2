package uk.iankent.ikettle2.data;

import android.app.Activity;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by iankent on 27/12/2015.
 */
public class Kettles {
    protected static ArrayList<Kettle> kettles = new ArrayList<Kettle>();

    public static ArrayList<Kettle> Get() {
        return kettles;
    }

    public static void Load(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences("iKettle", 0);
        SharedPreferences.Editor editor = prefs.edit();
        String json = prefs.getString("kettles", "[]");

        // Now convert the JSON string back to your java object
        Type type = new TypeToken<ArrayList<Kettle>>(){}.getType();
        ArrayList<Kettle> inpList = new Gson().fromJson(json, type);
        kettles = inpList;
    }

    public static void Save(Activity activity) {
        String json = new Gson().toJson(kettles);
        SharedPreferences prefs = activity.getSharedPreferences("iKettle", 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("kettles", json);
        editor.apply();
    }
}
