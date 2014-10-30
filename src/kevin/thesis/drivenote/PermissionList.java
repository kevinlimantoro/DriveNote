package kevin.thesis.drivenote;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpRequest;
import com.google.api.client.http.json.JsonHttpRequestInitializer;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveRequest;
import com.google.api.services.drive.model.Permission;

public class PermissionList extends ListActivity {
	static final String API_KEY = "AIzaSyATq1hMYwwfXxv186EJIcix3rld_PKFgow";
	private String accToken,inputEmail,inputRole,fileId,permissionID;
	private Drive service;
	private static final int CHANGE_PRIVILEGE=1;
	private static final int INSERT_EMAIL=2;
	private static final int ADD_PRIVILEGE=3;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.activity_permission_list);
		Bundle b=getIntent().getExtras();
		service=buildService(b.getString("AccessToken"));
		fileId=b.getString("fileId");
		List<Permission> plist=new ArrayList<Permission>();
		try {
			plist=new readPermissionTask().execute(fileId).get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		setListAdapter(new PermissionAdapter(this, plist));
	}
	//KALO PAS NGASIH PRIVILEGE SETEMAILNOTIF true
	private class readPermissionTask extends
	AsyncTask<String, Integer, List<Permission>> {

		@Override
		protected List<Permission> doInBackground(String... params) {
			// TODO Auto-generated method stub
			try {
				com.google.api.services.drive.model.PermissionList permissions = service.permissions().list(params[0]).execute();
				return permissions.getItems();
			} catch (IOException e) {
				System.out.println("An error occurred: " + e);
			}
			return null;
		}

	}
	private Drive buildService(final String authToken) {
		HttpTransport httpTransport = new NetHttpTransport();
		JacksonFactory jsonFactory = new JacksonFactory();
		Drive.Builder b = new Drive.Builder(httpTransport, jsonFactory, null);
		b.setJsonHttpRequestInitializer(new JsonHttpRequestInitializer() {
			@Override
			public void initialize(JsonHttpRequest request) {
				DriveRequest driveRequest = (DriveRequest) request;
				driveRequest.setPrettyPrint(true);
				driveRequest.setKey(API_KEY);
				driveRequest.setOauthToken(authToken);
			}
		});
		return b.build();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, "Add Share");
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			showDialog(INSERT_EMAIL);
			return true;
		}
		return false;
	}


	public void ChangePrivilege(View v){
		permissionID=v.getTag().toString();
		showDialog(CHANGE_PRIVILEGE);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case ADD_PRIVILEGE:
			AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
			builder2.setTitle("Select Privilege");
			final String[] name = new String[2];
			name[0]="Can Edit";
			name[1]="View Only";
			Toast.makeText(this, inputEmail, Toast.LENGTH_SHORT).show();
			builder2.setItems(name, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					if(which==0){
						inputRole="writer";
					}
					else if(which==1){
						inputRole="reader";
					}
					String[] temp=new String [2];
					temp[0]=inputEmail;
					temp[1]=inputRole;
					new InsertPermissionTask().execute(temp);
					Toast.makeText(getApplicationContext(), name[which], Toast.LENGTH_LONG).show();
				}
			});
			return builder2.create();
		case INSERT_EMAIL:
			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			alert.setTitle("Share To");
			alert.setMessage("Input Email");

			// Set an EditText view to get user input 
			final EditText input = new EditText(this);
			alert.setView(input);
			input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
			alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					Editable value = input.getText();
					isEmailValid(value.toString());
				}
			});

			alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					return;
				}
			});

			return alert.show();
		case CHANGE_PRIVILEGE:
			AlertDialog.Builder builder3 = new AlertDialog.Builder(this);
			builder3.setTitle("Select Privilege");
			final String[] name2 = new String[3];
			name2[0]="Can Edit";
			name2[1]="View Only";
			name2[2]="Remove Share";
			Toast.makeText(this, inputEmail, Toast.LENGTH_SHORT).show();
			builder3.setItems(name2, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
						if(which==0){
							inputRole="writer";
						}
						else if(which==1){
							inputRole="reader";
						}
						String[] temp=new String [3];
						temp[0]=permissionID;
						temp[1]=inputRole;
						temp[2]=String.valueOf(which);
						new ChangePermissionTask().execute(temp);
				}
			});
			return builder3.create();
		}
		return null;
	}

	private class InsertPermissionTask extends
	AsyncTask<String, Integer, String> {

		@Override
		protected String doInBackground(String... params) {
			// TODO Auto-generated method stub
			System.out.println(params[0]+" "+params[1]);
			Permission newPermission = new Permission();

			newPermission.setValue(params[0]);
			newPermission.setType("user");
			newPermission.setRole(params[1]);
			try {
				service.permissions().insert(fileId, newPermission).setSendNotificationEmails(true).execute();
				return "OK";
			} catch (IOException e) {
				handleException(e);
				System.out.println("An error occurred: " + e);
			}
			return null;
		}

		@Override
		protected void onPostExecute(String res) {
			if(res.equalsIgnoreCase("ok")){
				finish();
				startActivity(getIntent());
			}
		}

	}

	private class ChangePermissionTask extends
	AsyncTask<String, Integer, String> {

		@Override
		protected String doInBackground(String... params) {
			// TODO Auto-generated method stub
			int code=Integer.parseInt(params[2]);
			System.out.println(code);
			try {
				// First retrieve the permission from the API.
				if(code<2){
					Permission permission = service.permissions().get(
							fileId, params[0]).execute();
					permission.setRole(params[1]);
					service.permissions().update(
							fileId, params[0], permission).execute();}
				else{
					try {
						service.permissions().delete(fileId, params[0]).execute();
					} catch (IOException e) {
						System.out.println("An error occurred: " + e);
					}
				}
				return "OK";
			} catch (IOException e) {
				handleException(e);
				System.out.println("An error occurred: " + e);
			}
			return null;
		}

		@Override
		protected void onPostExecute(String res) {
			if(res.equalsIgnoreCase("ok")){
				finish();
				startActivity(getIntent());
			}
		}

	}

	public void isEmailValid(String email) {
		boolean isValid = false;

		String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
		CharSequence inputStr = email;

		Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(inputStr);
		if (matcher.matches()) {
			isValid = true;
		}

		if(isValid){
			inputEmail=email;
			showDialog(ADD_PRIVILEGE);
		}
		else{
			Toast.makeText(this, "Please Insert Valid Email", Toast.LENGTH_SHORT).show();
			showDialog(INSERT_EMAIL);
		}
	}

	void handleException(Exception e) {
		e.printStackTrace();
		if (e instanceof HttpResponseException) {
			int statusCode = ((HttpResponseException) e).getStatusCode();
			// TODO: should only try this once to avoid infinite loop
			if (statusCode == 401) {
				finish();
			}
		}
		Log.e("Handle", e.getMessage(), e);
	}
}
