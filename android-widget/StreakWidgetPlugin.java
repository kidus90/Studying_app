package com.kidus.studyquest;

import android.content.Context;
import android.content.SharedPreferences;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/** Receives streak data from the web app and refreshes the home/lock screen widget. */
@CapacitorPlugin(name = "StreakWidget")
public class StreakWidgetPlugin extends Plugin {
    @PluginMethod
    public void update(PluginCall call) {
        Context c = getContext();
        SharedPreferences.Editor e = c.getSharedPreferences("sq_widget", Context.MODE_PRIVATE).edit();
        e.putInt("streak", call.getInt("streak", 0));
        e.putInt("best", call.getInt("best", 0));
        e.putInt("fz", call.getInt("fz", 0));
        e.putString("sd", call.getString("sd", ""));
        e.putString("first", call.getString("first", ""));
        e.putString("days", call.getString("days", "{}"));
        e.apply();
        StreakWidgetProvider.refreshAll(c);
        call.resolve();
    }
}
