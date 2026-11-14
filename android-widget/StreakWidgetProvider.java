package com.kidus.studyquest;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class StreakWidgetProvider extends AppWidgetProvider {
    static final long DAY = 86400000L;
    static final String ICE = "\uD83E\uDDCA", COLD = "\uD83E\uDD76", TROPHY = "\uD83C\uDFC6";
    static final int DONE = 0, FREEZE = 1, MISS = 2, TODAY = 3, FUTURE = 4, EMPTY = 5;
    static final String[] NAMES = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    static final int[] IC = {R.id.ic0, R.id.ic1, R.id.ic2, R.id.ic3, R.id.ic4, R.id.ic5, R.id.ic6};
    static final int[] EM = {R.id.em0, R.id.em1, R.id.em2, R.id.em3, R.id.em4, R.id.em5, R.id.em6};
    static final int[] LA = {R.id.a0, R.id.a1, R.id.a2, R.id.a3, R.id.a4, R.id.a5, R.id.a6};
    static final int[] LB = {R.id.b0, R.id.b1, R.id.b2, R.id.b3, R.id.b4, R.id.b5, R.id.b6};
    static final int[] DT = {R.id.d0, R.id.d1, R.id.d2, R.id.d3, R.id.d4, R.id.d5, R.id.d6};
    static final int[] LB_TXT = {R.id.l0, R.id.l1, R.id.l2, R.id.l3, R.id.l4, R.id.l5, R.id.l6};

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

    static int col(Context c, int id) { return ContextCompat.getColor(c, id); }

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

        int cText = col(ctx, R.color.sw_text), cText2 = col(ctx, R.color.sw_text2);

        // header
        v.setTextViewText(R.id.sw_pill_txt, streak + (streak == 1 ? " Day Streak" : " Day Streak"));
        if (playedToday) { v.setTextViewText(R.id.sw_sub, "Done for today \u2713"); v.setTextColor(R.id.sw_sub, col(ctx, R.color.sw_good)); }
        else if (streak > 0) { v.setTextViewText(R.id.sw_sub, "Play today to keep your streak"); v.setTextColor(R.id.sw_sub, col(ctx, R.color.sw_warn)); }
        else { v.setTextViewText(R.id.sw_sub, "Start a streak today"); v.setTextColor(R.id.sw_sub, cText2); }

        // Monday-to-Sunday week containing today (UTC, like the app)
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(now);
        int todayIdx = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7; // Mon=0 .. Sun=6

        int cFire = col(ctx, R.color.sw_fire), cIce = col(ctx, R.color.sw_ice), cAcc = col(ctx, R.color.sw_accent);
        int cOff = col(ctx, R.color.sw_line_off), cMissLine = col(ctx, R.color.sw_line_miss);

        for (int j = 0; j < 7; j++) {
            long ms = now + (long) (j - todayIdx) * DAY;
            String k = f.format(new Date(ms));
            String st = days.optString(k, "");
            int state;
            if (k.compareTo(today) > 0) state = FUTURE;
            else if (st.equals("g")) state = DONE;
            else if (st.equals("f")) state = FREEZE;
            else if (k.equals(today)) state = TODAY;
            else if (saved && k.compareTo(sd) > 0 && k.compareTo(today) < 0) state = FREEZE;
            else if (k.compareTo(first) < 0) state = EMPTY;
            else state = MISS;

            boolean vector = state == DONE || state == TODAY || state == FUTURE || state == EMPTY;
            v.setViewVisibility(IC[j], vector ? View.VISIBLE : View.GONE);
            v.setViewVisibility(EM[j], vector ? View.GONE : View.VISIBLE);

            int dot, lineA, lineB;
            switch (state) {
                case DONE:
                    v.setImageViewResource(IC[j], R.drawable.sw_ic_done);
                    dot = R.drawable.sw_dot_done; lineA = cFire; lineB = cFire; break;
                case FREEZE:
                    v.setTextViewText(EM[j], ICE); v.setInt(EM[j], "setBackgroundResource", R.drawable.sw_c_ice);
                    dot = R.drawable.sw_dot_ice; lineA = cIce; lineB = cIce; break;
                case MISS:
                    v.setTextViewText(EM[j], COLD); v.setInt(EM[j], "setBackgroundResource", R.drawable.sw_c_miss);
                    dot = R.drawable.sw_dot_miss; lineA = cMissLine; lineB = cMissLine; break;
                case TODAY:
                    v.setImageViewResource(IC[j], R.drawable.sw_ic_today);
                    dot = R.drawable.sw_dot_today; lineA = cAcc; lineB = cOff; break;
                default: // FUTURE, EMPTY
                    v.setImageViewResource(IC[j], R.drawable.sw_ic_future);
                    dot = R.drawable.sw_dot_off; lineA = cOff; lineB = cOff; break;
            }
            v.setImageViewResource(DT[j], dot);
            v.setInt(LA[j], "setBackgroundColor", lineA);
            v.setInt(LB[j], "setBackgroundColor", lineB);
            if (j == 0) v.setViewVisibility(LA[j], View.INVISIBLE);
            if (j == 6) v.setViewVisibility(LB[j], View.INVISIBLE);

            SpannableString lab = new SpannableString(NAMES[j]);
            if (j == todayIdx) lab.setSpan(new StyleSpan(Typeface.BOLD), 0, lab.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            v.setTextViewText(LB_TXT[j], lab);
            v.setTextColor(LB_TXT[j], j == todayIdx ? cText : cText2);
        }

        // freezes
        StringBuilder ic = new StringBuilder();
        for (int i = 0; i < 2; i++) { if (i > 0) ic.append(" "); ic.append(i < fz ? ICE : "\u25CB"); }
        boolean full = fz >= 2;
        int prog = full ? 7 : streak % 7;
        int left = 7 - prog;
        v.setProgressBar(R.id.sw_pb, 7, prog, false);
        v.setTextViewText(R.id.sw_fz_txt, ic + "  " + (full ? "Freezes full" : left + (left == 1 ? " day" : " days") + " to next freeze"));
        v.setTextViewText(R.id.sw_best, TROPHY + " Best " + best);

        Intent launch = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
        if (launch != null) {
            PendingIntent pi = PendingIntent.getActivity(ctx, 0, launch, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            v.setOnClickPendingIntent(R.id.sw_root, pi);
        }
        return v;
    }
}
