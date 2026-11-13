package com.mehmetsahcicekdag.segem;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Calendar;

public class ReminderReceiver extends BroadcastReceiver {
    static final String CH = "hatirlatma";

    @Override
    public void onReceive(Context c, Intent i) {
        notifyNow(c);
        scheduleAll(c);
    }

    static void notifyNow(Context c) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(CH, "Çalışma hatırlatması", NotificationManager.IMPORTANCE_DEFAULT));
        }
        String name = c.getSharedPreferences("rem", Context.MODE_PRIVATE).getString("name", "");
        Intent open = new Intent(c, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(c, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, CH) : new Notification.Builder(c);
        b.setSmallIcon(android.R.drawable.ic_dialog_info)
         .setContentTitle("Sigorta Sınavı")
         .setContentText((name.isEmpty() ? "" : name + ", ") + "bugünkü sorularını çözme zamanı!")
         .setContentIntent(pi).setAutoCancel(true);
        nm.notify(1, b.build());
    }

    static void scheduleAll(Context c) {
        SharedPreferences p = c.getSharedPreferences("rem", Context.MODE_PRIVATE);
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        String csv = p.getString("times", "");
        String[] parts = csv.isEmpty() ? new String[0] : csv.split(",");
        for (int k = 0; k < 8; k++) {
            Intent in = new Intent(c, ReminderReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(c, 100 + k, in, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            am.cancel(pi);
        }
        if (!p.getBoolean("on", false)) return;
        for (int k = 0; k < parts.length && k < 8; k++) {
            try {
                String[] hm = parts[k].trim().split(":");
                Calendar t = Calendar.getInstance();
                t.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hm[0]));
                t.set(Calendar.MINUTE, Integer.parseInt(hm[1]));
                t.set(Calendar.SECOND, 0);
                if (t.getTimeInMillis() <= System.currentTimeMillis()) t.add(Calendar.DAY_OF_YEAR, 1);
                Intent in = new Intent(c, ReminderReceiver.class);
                PendingIntent pi = PendingIntent.getBroadcast(c, 100 + k, in, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                am.set(AlarmManager.RTC_WAKEUP, t.getTimeInMillis(), pi);
            } catch (Exception e) { }
        }
    }
}
