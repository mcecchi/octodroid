package com.mariogrip.octodroid;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by mariogrip on 30.10.14.
 *
 * GNU Affero General Public License http://www.gnu.org/licenses/agpl.html
 *
 * Service file, takes care of notifications
 */

public class service extends IntentService {
    private Timer timer = new Timer();
    private TimerTask timerTask;
    private Timer timer2 = new Timer();
    private TimerTask timerTask2;
    private double complete;
    public static boolean isIRunning = false;
    private int intcom;
    private boolean notefaRunning = false;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;

    public service() {
        super("OctoDroidService");
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        runner();
    }

    public void runner() {
        isIRunning = true;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int savemode2 = 10000;
        if (prefs.getBoolean("battery", false)){
            savemode2 = 40000;
        }
        timerTask = new TimerTask() {
            @Override
            public void run() {
                if (memory.isServerUp()) {
                    util_decode.decodeConnections();
                    Log.d("OctoDroid Service", "runner " + mainActivity.printing);
                    if (mainActivity.printing) {
                        mainActivity.printing = true;
                        timerTask.cancel();
                        startPrintService();
                        return;
                    }
                    if (memory.connection.current.getState().equals("Printing") && !mainActivity.printing) {
                        mainActivity.printing = true;
                        timerTask.cancel();
                        startPrintService();
                        return;
                    }
                    if (!memory.connection.current.getState().equals("Printing") && mainActivity.printing) {
                        mainActivity.printing = false;
                    }
                    util.logD(memory.connection.current.getState());
                }
            }
        };
        timer.schedule(timerTask, 0, savemode2);

    }

    protected void startPrintService() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int savemode = 4000;
        if (prefs.getBoolean("battery", false)){
            savemode = 20000;
        }
        final boolean notef = prefs.getBoolean("stick", true);
        final int id = 1;
        Log.d("OctoDroid Service", "StartprintService");


            mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mBuilder = new NotificationCompat.Builder(this);
            mBuilder.setContentTitle("OctoDroid")
                    .setContentText("Printing")
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.octodroid_smal);
        if (notef && !notefaRunning) {
            mNotifyManager.notify(id, mBuilder.build());
            notefaRunning = true;
        }

        timerTask2 = new TimerTask() {
            @Override
            public void run() {
                Log.d("OctoDroid Service", "startPrintService timertask");
                try {
                    util_decode.decodeConnections();
                    util_decode.decodeJob();
                    complete = memory.job.progress.completion;
                }catch (Exception e){
                    complete = 0;
                }
                if (!memory.connection.current.getState().equals("Printing")) {
                    Log.d("OctoDroid Service", "startPrintService stopping");
                    mainActivity.printing = false;
                    notefaRunning = false;
                    mNotifyManager.cancel(id);
                    Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    mBuilder.setContentText("Print complete")
                            .setContentTitle("OctoDroid")
                            .setSound(soundUri)
                            .setSmallIcon(R.drawable.octodroid_smal)
                            .setOngoing(false)
                            .setProgress(0, 0, false);
                    mNotifyManager.notify(id, mBuilder.build());
                    timerTask2.cancel();
                    runner();
                    return;
                }
                Log.d("OctoDroid Service", "startPrintService Notify" + complete );
                if (notef) {
                    mBuilder.setProgress(100, (int) complete, false).setContentText("Printing (" + (int) complete + "%)");
                    mNotifyManager.notify(id, mBuilder.build());
                }
            }
        };
        timer2.schedule(timerTask2, 0, savemode);
    }
}