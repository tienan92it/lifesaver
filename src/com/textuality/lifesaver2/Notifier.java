package com.textuality.lifesaver2;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class Notifier {

    private Context mContext;
    private NotificationManager mManager;
    private PendingIntent mPI;

    private static final int NOTIFIER_ID = 39;

    public Notifier(Context context) {
        mContext = context;
        mManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mPI = PendingIntent.getActivity(context, 0, new Intent(context, LifeSaver.class), 0);
    }

    public void notifySave(String message, boolean done) {
        notify(R.drawable.white_statusbar, str(R.string.saving_ticker), message, done);
    }

    public void notifyRestore(String message, boolean done) {
        notify(R.drawable.red_statusbar, str(R.string.restoring_ticker), message, done);
    }
    private void notify(int icon, String ticker, String message, boolean done) {
        Notification n = new Notification(icon, ticker,  System.currentTimeMillis());
        n.tickerText = ticker;
        n.flags = Notification.FLAG_AUTO_CANCEL;
        if (!done) 
            n.flags |= Notification.FLAG_ONGOING_EVENT;
        n.defaults = 0;
        n.sound = null;
        n.vibrate = null;
        n.setLatestEventInfo(mContext, "LifeSaver", message, mPI);
        mManager.notify(NOTIFIER_ID, n);       
    }

    private String str(int id) {
        return mContext.getString(id);
    }
}
