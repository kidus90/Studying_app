package com.kidus.studyquest;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.core.content.ContextCompat;
import android.widget.RemoteViews;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class StreakWidgetProvider extends AppWidgetProvider {
    static final long DAY = 86400000L;
    static final String FIRE = "\uD83D\uDD25", ICE = "\uD83E\uDDCA", COLD = "\uD83E\uDD76", TROPHY = "\uD83C\uDFC6", FLAME_BIG = "\uD83C\uDF0B", GEM = "\uD83D\uDC8E";

    @Override
    public void onUpdate(Context ctx, AppWidgetManager m, int[] ids) {
        for (int id : ids) m.updateAppWidget(id, build(ctx));
    }

    public static void refreshAll(Context ctx) {
        AppWidgetManager m = AppWidgetManager.getInstance(ctx);
        int[] ids = m.getAppWidgetIds(new ComponentName(ctx, StreakWidgetProvider.class));
        for (int id : ids) m.updateAppWidget(id, build(ctx));
    }

    static SimpleDateFormat df() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC")); // same day boundary as the app
        return f;
    }

    static RemoteViews build(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences("sq_widget", Context.MODE_PRIVATE);
        RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.widget_streak);
        SimpleDateFormat f = df();
        long now = System.currentTimeMillis();
        String today = f.format(new Date(now));

        int streak = p.getInt("streak", 0), best = p.getInt("best", 0), fz = p.getInt("fz", 0);
        String sd = p.getString("sd", ""), first = p.getString("first", today);
        JSONObject days;
        try { days = new JSONObject(p.getString("days", "{}")); } catch (Exception e) { days = new JSONObject(); }

        // Mirror the app's rule: a gap of g days is bridged by g-1 freezes, otherwise the streak dies.
        long g = 0;
        if (!sd.isEmpty()) {
            try { g = (f.parse(today).getTime() - f.parse(sd).getTime()) / DAY; } catch (Exception e) { g = 0; }
        }
        boolean saved = false;
        if (streak > 0 && g > 1) {
            if (fz >= g - 1) { saved = true; fz -= (int) (g - 1); } else { streak = 0; }
        }
        boolean playedToday = "g".equals(days.optString(today, ""));

        int cText2 = ContextCompat.getColor(ctx, R.color.sw_text2);
        int cAccent = ContextCompat.getColor(ctx, R.color.sw_accent);

        v.setTextViewText(R.id.sw_flame, streak >= 30 ? GEM : streak >= 7 ? FLAME_BIG : FIRE);
        v.setTextViewText(R.id.sw_streak, String.valueOf(streak));
        if (playedToday) { v.setTextViewText(R.id.sw_sub, "Done for today \u2713"); v.setTextColor(R.id.sw_sub, ContextCompat.getColor(ctx, R.color.sw_good)); }
        else if (streak > 0) { v.setTextViewText(R.id.sw_sub, "Play today to keep it"); v.setTextColor(R.id.sw_sub, ContextCompat.getColor(ctx, R.color.sw_warn)); }
        else { v.setTextViewText(R.id.sw_sub, "Start a streak today"); v.setTextColor(R.id.sw_sub, cText2); }
        v.setTextViewText(R.id.sw_best, TROPHY + " Best " + best);

        int[] E = {R.id.e0, R.id.e1, R.id.e2, R.id.e3, R.id.e4, R.id.e5, R.id.e6};
        int[] L = {R.id.l0, R.id.l1, R.id.l2, R.id.l3, R.id.l4, R.id.l5, R.id.l6};
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        for (int i = 0; i < 7; i++) {
            long ms = now - (long) (6 - i) * DAY;
            String k = f.format(new Date(ms));
            String st = days.optString(k, "");
            boolean isToday = k.equals(today);
            String e;
            int bg;
            if (st.equals("g")) { e = FIRE; bg = R.drawable.sw_c_fire; }
            else if (st.equals("f")) { e = ICE; bg = R.drawable.sw_c_ice; }
            else if (isToday) { e = ""; bg = R.drawable.sw_c_today; }
            else if (saved && k.compareTo(sd) > 0 && k.compareTo(today) < 0) { e = ICE; bg = R.drawable.sw_c_ice; }
            else if (k.compareTo(first) < 0) { e = ""; bg = R.drawable.sw_c_empty; }
            else { e = COLD; bg = R.drawable.sw_c_miss; }
            cal.setTimeInMillis(ms);
            String lab = isToday ? "Today" : String.valueOf("SMTWTFS".charAt(cal.get(Calendar.DAY_OF_WEEK) - 1));
            v.setTextViewText(E[i], e);
            v.setInt(E[i], "setBackgroundResource", bg);
            v.setTextViewText(L[i], lab);
            v.setTextColor(L[i], isToday ? cAccent : cText2);
        }

        StringBuilder ic = new StringBuilder();
        for (int i = 0; i < 2; i++) { if (i > 0) ic.append(" "); ic.append(i < fz ? ICE : "\u25CB"); }
        v.setTextViewText(R.id.sw_fz, ic.toString());
        boolean full = fz >= 2;
        int prog = full ? 7 : streak % 7;
        v.setProgressBar(R.id.sw_pb, 7, prog, false);
        v.setTextViewText(R.id.sw_pt, full ? "Freezes full" : (7 - prog) + (7 - prog == 1 ? " day" : " days") + " to " + ICE);

        Intent launch = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
        if (launch != null) {
            PendingIntent pi = PendingIntent.getActivity(ctx, 0, launch, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            v.setOnClickPendingIntent(R.id.sw_root, pi);
        }
        return v;
    }
}
