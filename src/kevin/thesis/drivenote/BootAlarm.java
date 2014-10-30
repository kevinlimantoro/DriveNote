package kevin.thesis.drivenote;

import java.util.Calendar;

import android.accounts.Account;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

public class BootAlarm extends BroadcastReceiver{
	private final String BOOT_COMPLETED_ACTION = "android.intent.action.BOOT_COMPLETED";
	private SharedPreferences settings;
	private static final String PREF = "MyPrefs";
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		 if(intent.getAction().equals(BOOT_COMPLETED_ACTION)){
			// Toast.makeText(context, "RESTARTED", Toast.LENGTH_SHORT).show();
			 settings = context.getSharedPreferences(PREF, 0);
			int type=settings.getInt("IntervalType", 0);
			AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE); 
			Long value=settings.getLong("IntervalValue", 0);
			if(type==1){
				//Toast.makeText(context, "setted", Toast.LENGTH_LONG).show();
				Intent intent1 = new Intent(context, SyncAlarm.class);
				PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
						intent1, PendingIntent.FLAG_CANCEL_CURRENT);
				am.setRepeating(AlarmManager.RTC_WAKEUP, value,am.INTERVAL_DAY, pendingIntent);
			}
			else if(type==2){
				//Toast.makeText(context, "setted", Toast.LENGTH_LONG).show();
				Intent intent1 = new Intent(context, SyncAlarm.class);
				PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
						intent1, PendingIntent.FLAG_CANCEL_CURRENT);
				am.setRepeating(AlarmManager.RTC_WAKEUP,System.currentTimeMillis(),value, pendingIntent);
			}
        }
	}

}
