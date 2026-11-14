package com.kidus.studyquest;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
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

        v.setTextViewText(R.id.sw_streak, (streak >= 30 ? GEM : streak >= 7 ? FLAME_BIG : FIRE) + " " + streak);
        if (playedToday) { v.setTextViewText(R.id.sw_sub, "day streak \u2713"); v.setTextColor(R.id.sw_sub, Color.parseColor("#0B6B3A")); }
        else if (streak > 0) { v.setTextViewText(R.id.sw_sub, "play today to keep it!"); v.setTextColor(R.id.sw_sub, Color.parseColor("#D9480F")); }
        else { v.setTextViewText(R.id.sw_sub, "start a streak today"); v.setTextColor(R.id.sw_sub, Color.parseColor("#4A5B73")); }
        v.setTextViewText(R.id.sw_best, TROPHY + " " + best);

        int[] E = {R.id.e0, R.id.e1, R.id.e2, R.id.e3, R.id.e4, R.id.e5, R.id.e6};
        int[] L = {R.id.l0, R.id.l1, R.id.l2, R.id.l3, R.id.l4, R.id.l5, R.id.l6};
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        for (int i = 0; i < 7; i++) {
            long ms = now - (long) (6 - i) * DAY;
            String k = f.format(new Date(ms));
            String st = days.optString(k, "");
            String e;
            if (st.equals("g")) e = FIRE;
            else if (st.equals("f")) e = ICE;
            else if (k.equals(today)) e = "\u25CB";
            else if (saved && k.compareTo(sd) > 0 && k.compareTo(today) < 0) e = ICE;
            else if (k.compareTo(first) < 0) e = "\u00B7";
            else e = COLD;
            cal.setTimeInMillis(ms);
            String lab = k.equals(today) ? "Today" : String.valueOf("SMTWTFS".charAt(cal.get(Calendar.DAY_OF_WEEK) - 1));
            v.setTextViewText(E[i], e);
            v.setTextViewText(L[i], lab);
        }

        StringBuilder ic = new StringBuilder();
        for (int i = 0; i < 2; i++) ic.append(i < fz ? ICE : "\u25AB");
        v.setTextViewText(R.id.sw_fz, ic.toString());
        boolean full = fz >= 2;
        int prog = full ? 7 : streak % 7;
        v.setProgressBar(R.id.sw_pb, 7, prog, false);
        v.setTextViewText(R.id.sw_pt, full ? "freezes full" : (7 - prog) + "d to " + ICE);

        Intent launch = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
        if (launch != null) {
            PendingIntent pi = PendingIntent.getActivity(ctx, 0, launch, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            v.setOnClickPendingIntent(R.id.sw_root, pi);
        }
        return v;
    }
}
