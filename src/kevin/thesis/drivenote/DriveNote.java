package kevin.thesis.drivenote;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.model.File;

public class DriveNote extends Activity {
	private DatabaseHandler db;
	public GoogleAccountManager accountManager;
	private AccountManager accMgr;
	private Account[] accounts;  
	private String accountName;
	private AlarmManager am;
	private Calendar calendar;
	private Button btn;
	private Boolean res;
	private SharedPreferences settings;
	private static final String PREF = "MyPrefs";
	private final int DIALOG_ACCOUNTS=0;
	private final int TIME_ID=111;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_drive_note);
		accountManager = new GoogleAccountManager(this); 
		accMgr=AccountManager.get(this);
		gotAccount();
		btn=(Button) findViewById(R.id.LocalButton);
		float a=25;
		btn.setTextSize(TypedValue.COMPLEX_UNIT_PX,a);
		btn=(Button) findViewById(R.id.DriveButton);
		btn.setTextSize(TypedValue.COMPLEX_UNIT_PX,a);
		btn=(Button) findViewById(R.id.NoteButton);
		btn.setTextSize(TypedValue.COMPLEX_UNIT_PX,a);
		btn=(Button) findViewById(R.id.SyncList);
		btn.setTextSize(TypedValue.COMPLEX_UNIT_PX,a);
		db=new DatabaseHandler(this);
		calendar=Calendar.getInstance();
		am = (AlarmManager) getSystemService(Context.ALARM_SERVICE); 
		/*db.addLokal(new CLocalFile(1,"ha","he"));
        CLocalFile a=db.getLokalFile(1);
        Toast.makeText(this, a.getUser(), Toast.LENGTH_LONG).show();*/
		//System.out.println(db.getDriveCount(accountName));
		//setRepeatingAlarm(); //gila pake ini ngulang terus tiada henti
	}



	public void setOneTimeAlarm(boolean cancel) {
		System.out.println("ONETIME");
		Intent intent = new Intent(this, SyncAlarm.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
				intent, PendingIntent.FLAG_CANCEL_CURRENT);
		if(cancel){
			am.cancel(pendingIntent);
			SharedPreferences.Editor editor = settings.edit();
			editor.putInt("IntervalType", 0);
			editor.commit();}
		am.set(AlarmManager.RTC_WAKEUP,
				System.currentTimeMillis()+(1 * 1000), pendingIntent);
	}

	public void setRepeatingAlarm(int hour,int minute) {
		System.out.println("REPEATClock");
		Intent intent = new Intent(this, SyncAlarm.class);
		calendar.set(Calendar.HOUR_OF_DAY, hour);
		calendar.set(Calendar.MINUTE, minute);
		calendar.set(Calendar.SECOND, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt("IntervalType", 1);
		editor.putLong("IntervalValue", calendar.getTimeInMillis());
		editor.commit();
		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
				intent, 0);
		am.cancel(pendingIntent);
		am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),am.INTERVAL_DAY, pendingIntent);
	}

	public void setRepeatingInterval(int minute) {
		settings = getSharedPreferences(PREF, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt("IntervalType", 2);
		editor.putLong("IntervalValue", minute*60000);
		editor.commit();
		Intent intent = new Intent(this, SyncAlarm.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
				intent, 0);
		am.cancel(pendingIntent);
		am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),minute*60000, pendingIntent);
		//am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),minute*60000, pendingIntent);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case TIME_ID: // your unique dialog id from step 1
			return new TimePickerDialog(this, timePickerListener, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE),
					false);
		case DIALOG_ACCOUNTS:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Select a Google account");
			accounts = accMgr.getAccountsByType("com.google");
			final int size = accounts.length;
			String[] names = new String[size];
			for (int i = 0; i < size; i++) {
				names[i] = accounts[i].name;
			}
			builder.setItems(names, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					gotAccount(accounts[which]);
				}
			});
			return builder.create();
		}

		return null;
	}

	private TimePickerDialog.OnTimeSetListener timePickerListener = 
			new TimePickerDialog.OnTimeSetListener() {
		public void onTimeSet(TimePicker view, int selectedHour,
				int selectedMinute) {
			Intent intent = new Intent(getApplicationContext(), SyncAlarm.class);
			am.cancel(PendingIntent.getBroadcast(getApplicationContext(), 0,intent, PendingIntent.FLAG_CANCEL_CURRENT));
			setRepeatingAlarm(selectedHour, selectedMinute);
		}
	};


	public void gotAccount() {
		settings = getSharedPreferences(PREF, 0);
		accountName = settings.getString("accountName", null); 
		Account account= accountManager.getAccountByName(accountName);
		if (account!= null) {
			gotAccount(account);
		}
		else{
			showDialog(DIALOG_ACCOUNTS);}
	}

	public void gotAccount(Account got){
		settings = getSharedPreferences(PREF, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("accountName", got.name);
		accountName=got.name;
		editor.commit(); 
	}

	public void loadLocalMemory(View v)
	{
		Intent i=new Intent(this,LocalList.class);
		startActivity(i);
	}
	
	private boolean isNetworkAvailable() {
		  ConnectivityManager connectivityManager 
		          = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
		    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		    return activeNetworkInfo != null;
		}
	
	private class isOnlineConnection extends
	AsyncTask<String, Integer, Boolean> {

		@Override
		protected Boolean doInBackground(String... params) {
			// TODO Auto-generated method stub
			Process p1;
			try {
				p1 = java.lang.Runtime.getRuntime().exec("ping -c 1 www.google.com");
				int returnVal = -1;
				try {
					returnVal=p1.waitFor();
					return (returnVal==0);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return false;
		}

	}

	public void LoadDrive(View v) throws InterruptedException, ExecutionException
	{
		
		if(accountName!=null&&isNetworkAvailable()){
			res=new isOnlineConnection().execute("now").get();
			if(res==true){
			Intent i=new Intent(getApplicationContext(),DriveActivity.class);
			startActivity(i);}
			else if(!res){
				AlertDialog.Builder alert2 = new AlertDialog.Builder(this);

				alert2.setTitle("DriveNote");
				alert2.setMessage("Your connection is not working well.");
				alert2.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						}
				});
				alert2.show();
			}
			}
		else{
			AlertDialog.Builder alert2 = new AlertDialog.Builder(this);

			alert2.setTitle("DriveNote");
			alert2.setMessage("It's either you don't have internet connection or no account associated on this app yet.");
			if(accountName==null){
			alert2.setNeutralButton("Set Account", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					gotAccount();
					}
			});
			alert2.show();
			}
			else{
				alert2.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						}
				});
				alert2.show();
			}
		}
	}

	public void OpenNote(View v){
		Intent i=new Intent(this,NoteEdit.class);
		startActivity(i);
	}

	public void OpenSyncList(View v){
		if(accountName!=null){
			Intent i=new Intent(this,OfflineList.class);
			startActivity(i);}
		else{
			Toast.makeText(this, "Must Have Account To Access This", Toast.LENGTH_LONG).show();
		}
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//db.emptyDrive();
		menu.add(0, 0, 0, "Switch Account");
		if(accountName!=null){
			menu.add(1, 1, 1, "Set Sync Time");
			menu.add(1, 2, 2, "Sync Now");
			menu.add(1, 3, 3, "Set Sync Interval");}
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			showDialog(DIALOG_ACCOUNTS);
			return true;
		case 1:
			showDialog(TIME_ID);
			//Toast.makeText(this, String.valueOf(db.getLokalCount(accountName))+"/"+db.getLokalCount2(accountName)+" "
			//		+String.valueOf(db.getDriveCount(accountName)+"/"+db.getDriveCount2(accountName)), Toast.LENGTH_LONG).show();
			//db.emptyDrive();
			return true;
		case 2:
			AlertDialog.Builder alert2 = new AlertDialog.Builder(this);

			alert2.setTitle("Synchronize");
			alert2.setMessage("Do you want to cancel all alarm synchronize? ");
			alert2.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					setOneTimeAlarm(true);
					Toast.makeText(getApplicationContext(), "OK", Toast.LENGTH_SHORT).show();}
			});
			alert2.setNegativeButton("No", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					setOneTimeAlarm(false);
					Toast.makeText(getApplicationContext(), "NO", Toast.LENGTH_SHORT).show();}
			});
			alert2.show();
			return true;
		case 3:
			AlertDialog.Builder builder3 = new AlertDialog.Builder(this);
			builder3.setTitle("Sync Every ");
			final String[] name2 = new String[6];
			name2[0]="30 Minute";
			name2[1]="1 Hour";
			name2[2]="2 Hours";
			name2[3]="5 Hours";
			name2[4]="12 Hours";
			name2[5]="For Test 5 Minute";
			builder3.setItems(name2, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					if(which==0){
						setRepeatingInterval(30);
					}
					else if(which==1){
						setRepeatingInterval(60);
					}
					else if(which==2){
						setRepeatingInterval(120);
					}
					else if(which==3){
						setRepeatingInterval(300);
					}
					else if(which==4){
						setRepeatingInterval(720);
					}
					else if(which==5){
						setRepeatingInterval(5);
					}
				}
			});
			builder3.create().show();
			return true;
		}
		return false;
	}
}
