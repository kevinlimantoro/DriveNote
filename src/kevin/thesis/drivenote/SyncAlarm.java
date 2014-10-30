package kevin.thesis.drivenote;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

public class SyncAlarm extends BroadcastReceiver{
private NotificationManager nm;
@Override
	public void onReceive(Context context, Intent intent) {
		if(isNetworkAvailable(context)==true){
		nm = (NotificationManager) context
			    .getSystemService(Context.NOTIFICATION_SERVICE);
			  CharSequence from = "DriveNote";
			  CharSequence message = "Starting Sync Process...";
			  System.out.println("JALAN");
			  PendingIntent contentIntent = PendingIntent.getActivity(context, 0,new Intent(context,DriveNote.class), 0);
			  Notification notif = new Notification(R.drawable.ic_launcher,
			    "Starting Sync Process...", System.currentTimeMillis());
			  notif.setLatestEventInfo(context, from, message, contentIntent);
			  nm.notify(1, notif);
			  Intent scheduledIntent=new Intent(context,SyncService.class);
			  scheduledIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			  context.startService(scheduledIntent);}
	}
	
	private boolean isNetworkAvailable(Context context) {
	   ConnectivityManager connectivityManager 
	          = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    return activeNetworkInfo != null;
	}

}
