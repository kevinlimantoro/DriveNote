package kevin.thesis.drivenote;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Adapter;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpRequest;
import com.google.api.client.http.json.JsonHttpRequestInitializer;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.DriveRequest;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

public class DriveActivity extends ListActivity {
	private static final int CHOOSE_PRIVILEGE = 1;
	private static final int DIALOG_ACCOUNTS = 0;
	private Account account;
	private AccountManagerFuture<Bundle> amf;
	private AccountManager accMgr;
	private Account[] accounts;
	private Drive service;
	private ProgressDialog pdialog;
	private static final String PREF = "MyPrefs";
	private String accountName;
	private DriveActivity cbt = this;
	private DatabaseHandler db;
	private String authTokenType = "oauth2:" + DriveScopes.DRIVE;
	private final android.os.Handler handler = null;
	public GoogleAccountManager accountManager;
	private int syncIndex;
	private String[] path;
	private int level=0;
	private int sortType=1;
	private List<File> temp = new ArrayList<File>();
	private List<String> fileList = new ArrayList<String>();
	private String accToken;
	private File ContextSelected;
	static final String API_KEY = "AIzaSyATq1hMYwwfXxv186EJIcix3rld_PKFgow";

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		// setContentView(R.layout.activity_drive);
		accountManager = new GoogleAccountManager(this);
		accMgr = AccountManager.get(this);
		// System.out.println(authTokenType);
		// authTokenType = "com.google";
		new gotAccountTask().execute(false);
		while(amf==null){
		}
		path=new String[20];
		/*new gotAccountTask().execute(true);
		while(amf==null){
		}*/                     
		try {
			accToken = new setAccessTokenTask().execute("now").get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (accToken != null) {
			System.out.println(accToken);
			service = buildService(accToken, API_KEY);
			SharedPreferences settings = getSharedPreferences(PREF, 0);
			syncIndex=settings.getInt("syncIndex", 0);
			accountName = settings.getString("accountName", null); 
			db=new DatabaseHandler(this);
			try {
				temp.clear();
				temp = new readDriveFileTask().execute("root").get();
				displayFile(temp);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			}
		}
		path[level]="root";
		registerForContextMenu(getListView());
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;


		Adapter adapter = getListAdapter();

		File item = (File) adapter.getItem(info.position);
		//Toast.makeText(cbt,item.getTitle(), Toast.LENGTH_SHORT).show();
		ContextSelected=item;
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.setHeaderTitle(item.getTitle());
		menu.add(0,0, 1, "Open File");
		menu.add(0, 1, 2, "Download File");
		menu.add(0, 2, 3, "Rename File");
		menu.add(0, 3, 4, "Share File");
		menu.add(0, 4, 5, "Delete File");  
	}

	public boolean onContextItemSelected(MenuItem m)
	{switch (m.getItemId())
		{
		case 0:
			Intent i = new
			Intent(Intent.ACTION_VIEW);
			i.setData(Uri.parse(ContextSelected.getAlternateLink()));
			startActivity(i);
			return true;
		case 1:
			if(!ContextSelected.getMimeType().equalsIgnoreCase("application/vnd.google-apps.folder"))
			{new DownloadFile().execute(ContextSelected);}
			else{
				Toast.makeText(this, "Sorry, Downloading Folder not Supported", Toast.LENGTH_LONG).show();
			}
			return true;
		case 2:
			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			alert.setTitle("Rename");
			alert.setMessage("Rename File to");

			// Set an EditText view to get user input 
			final EditText input = new EditText(this);
			input.setText(ContextSelected.getTitle());
			alert.setView(input);

			alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					String value = input.getText().toString();
					if(ContextSelected.getFileExtension()!=null){
						if(!value.contains(ContextSelected.getFileExtension())){
							value+="."+ContextSelected.getFileExtension();
							String[] temp=new String[2];
							temp[0]=ContextSelected.getId();
							temp[1]=value;
							new RenameFiletask().execute(temp);
						}else{
							String[] temp=new String[2];
							temp[0]=ContextSelected.getId();
							temp[1]=value;
							new RenameFiletask().execute(temp);}
					}
					else{
						Toast.makeText(cbt, value, Toast.LENGTH_SHORT).show();
						String[] temp=new String[2];
						temp[0]=ContextSelected.getId();
						temp[1]=value;
						new RenameFiletask().execute(temp);
					}
				}
			});

			alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					return;
				}
			});

			alert.show();
			return true;
		case 3:
			Intent intentPermission=new Intent(this,PermissionList.class);
			intentPermission.putExtra("fileId", ContextSelected.getId());
			intentPermission.putExtra("AccessToken", accToken);
			startActivity(intentPermission);
			return true;
		case 4:
			AlertDialog.Builder alert2 = new AlertDialog.Builder(this);

			alert2.setTitle("Delete");
			alert2.setMessage("Are You Sure to delete "+ContextSelected.getTitle()+"?");
			alert2.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					new TrashFiletask().execute(ContextSelected.getId());}
			});

			alert2.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					return;
				}
			});

			alert2.show();
			return true;
		}
	return false;	
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
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
					startActivity(getIntent());
					finish();
				}
			});
			return builder.create();
		}
		return null;
	}

	private class gotAccountTask extends AsyncTask<Boolean, Integer, Void>{

		@Override
		protected Void doInBackground(Boolean... expired) {
			// TODO Auto-generated method stub
			Boolean tokenExpired=expired[0];
			SharedPreferences settings = getSharedPreferences(PREF, 0);
			String accountName = settings.getString("accountName", null);
			Account account = accountManager.getAccountByName(accountName);
			if (account != null) {
				if (tokenExpired==true) {
					try {
						accMgr.invalidateAuthToken("com.google", amf.getResult()
								.getString(accMgr.KEY_AUTHTOKEN));
					} catch (OperationCanceledException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (AuthenticatorException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					amf = null;
				}
				// System.out.println(account.name);
				gotAccount(account);
			} else {
				showDialog(DIALOG_ACCOUNTS);
			}
			return null;
		}

	}
	/*public void gotAccount(boolean tokenExpired) {
		SharedPreferences settings = getSharedPreferences(PREF, 0);
		String accountName = settings.getString("accountName", null);
		Account account = accountManager.getAccountByName(accountName);
		if (account != null) {
			if (tokenExpired) {
				try {
					accMgr.invalidateAuthToken("com.google", amf.getResult()
							.getString(accMgr.KEY_AUTHTOKEN));
				} catch (OperationCanceledException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (AuthenticatorException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				accToken = null;
			}
			// System.out.println(account.name);
			gotAccount(account);
		} else {
			showDialog(DIALOG_ACCOUNTS);
		}
	}*/

	/**
	 * method is used for checking valid email id format.
	 * 
	 * @param email
	 * @return boolean true for valid false for invalid
	 */


	public void gotAccount(Account got) {
		SharedPreferences settings = getSharedPreferences(PREF, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("accountName", got.name);
		editor.commit();
		account = got;
		amf = accMgr.getAuthToken(account, authTokenType, true,
				new AccountManagerCallback<Bundle>() {
			public void run(AccountManagerFuture<Bundle> arg0) {
				try {
					Bundle result;
					Intent i;

					result = arg0.getResult();
					if (result.containsKey(accMgr.KEY_INTENT)) {
						i = (Intent) result.get(accMgr.KEY_INTENT);
						if (i.toString().contains(
								"GrantCredentialsPermissionActivity")) {
							// Will have to wait for the user to accept
							// the request therefore this will have to
							// run in a foreground application
							cbt.startActivity(i);
						} else {
							cbt.startActivity(i);
						}

					} else if (result.containsKey(accMgr.KEY_AUTHTOKEN)) {
						accToken = result
								.getString(accMgr.KEY_AUTHTOKEN);

						/*
						 * else { token =
						 * (String)result.get(AccountManager
						 * .KEY_AUTHTOKEN);
						 */

						/*
						 * work with token
						 */

						// Remember to invalidate the token if the web
						// service rejects it
						// if(response.isTokenInvalid()){
						// accMgr.invalidateAuthToken(authTokenType,
						// token);
						// }

					}
				} catch (OperationCanceledException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (AuthenticatorException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					handleException(e);
					e.printStackTrace();
				}

			}
		}, handler);

	}

	private Drive buildService(final String authToken, final String ApiKey) {
		HttpTransport httpTransport = new NetHttpTransport();
		JacksonFactory jsonFactory = new JacksonFactory();
		Drive.Builder b = new Drive.Builder(httpTransport, jsonFactory, null);
		b.setJsonHttpRequestInitializer(new JsonHttpRequestInitializer() {
			@Override
			public void initialize(JsonHttpRequest request) {
				DriveRequest driveRequest = (DriveRequest) request;
				driveRequest.setPrettyPrint(true);
				driveRequest.setKey(ApiKey);
				driveRequest.setOauthToken(authToken);
			}
		});
		return b.build();

	}

	private class setAccessTokenTask extends AsyncTask<String, Integer, String> {
		@Override
		protected String doInBackground(String... params) {
			// TODO Auto-generated method stub
			if (amf != null) {
				try {
					return amf.getResult().getString(accMgr.KEY_AUTHTOKEN);
				} catch (OperationCanceledException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (AuthenticatorException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					handleException(e);
					e.printStackTrace();
				}
			} else {
				showDialog(DIALOG_ACCOUNTS);
			}
			return null;
		}
	}

	private class createDocumentTask extends AsyncTask<String, Integer, String> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pdialog = new ProgressDialog(cbt);
			pdialog.setTitle("DriveNote");
			pdialog.setMessage("Creating File..");
			pdialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pdialog.show();
		}
		@Override
		protected String doInBackground(String... filetitle) {
			// TODO Auto-generated method stub
			File body = new File();
			body.setTitle(filetitle[0]);
			body.setMimeType("application/vnd.google-apps.document");
			try {
				File f = service.files().insert(body).execute();
				return "a";
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
		@Override
		protected void onPostExecute(String a){
			pdialog.dismiss();
			try {
				temp.clear();
				temp = new readDriveFileTask().execute(path[level]).get();
				displayFile(temp);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private class DownloadFile extends
	AsyncTask<File, Integer, String> {

		@Override
		protected String doInBackground(File... file) {
			// TODO Auto-generated method stub
			long totalSize=0;
			if(file[0].getDownloadUrl()!=null){
				if (file[0].getDownloadUrl() != null && file[0].getDownloadUrl().length() > 0) {
					try {
						HttpClient client = new DefaultHttpClient();//application/pdf
						HttpGet get = new HttpGet(file[0].getDownloadUrl());
						get.setHeader("Authorization", "Bearer " + accToken);
						org.apache.http.HttpResponse response = client.execute(get);
						totalSize=response.getEntity().getContentLength();
						createFilewithExtension(file[0].getTitle(),response.getEntity().getContent(),totalSize,file[0].getMimeType());
						return file[0].getTitle();
					} catch (IOException e) {
						handleException(e);
						e.printStackTrace();
					}
				}
			}
			else if(file[0].getMimeType().equalsIgnoreCase("application/vnd.google-apps.document")){
				if(file[0].getExportLinks().get("application/vnd.openxmlformats-officedocument.wordprocessingml.document")!=null){
					try {
						HttpClient client = new DefaultHttpClient();//application/pdf
						HttpGet get = new HttpGet(file[0].getExportLinks().get("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
						get.setHeader("Authorization", "Bearer " + accToken);
						org.apache.http.HttpResponse response = client.execute(get);
						totalSize=response.getEntity().getContentLength();
						if(file[0].getTitle().contains(".doc")&&!file[0].getTitle().contains(".docx")){
							createFile(file[0].getTitle()+"x",response.getEntity().getContent(),totalSize);
							return file[0].getTitle()+"x";
						}
						else if(!file[0].getTitle().contains(".docx")){
							createFile(file[0].getTitle()+".docx",response.getEntity().getContent(),totalSize);
							return file[0].getTitle()+".docx";
						}
						else{
							createFile(file[0].getTitle(),response.getEntity().getContent(),totalSize);
							return file[0].getTitle();}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						handleException(e);
						e.printStackTrace();
					}
				}
			}
			else if(file[0].getMimeType().equalsIgnoreCase("application/vnd.google-apps.spreadsheet")){
				if(file[0].getExportLinks().get("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")!=null){
					try {
						HttpClient client = new DefaultHttpClient();//application/pdf
						HttpGet get = new HttpGet(file[0].getExportLinks().get("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
						get.setHeader("Authorization", "Bearer " + accToken);
						org.apache.http.HttpResponse response = client.execute(get);
						totalSize=response.getEntity().getContentLength();
						if(file[0].getTitle().contains(".xls")&&!file[0].getTitle().contains(".xlsx")){
							createFile(file[0].getTitle()+"x",response.getEntity().getContent(),totalSize);
							return file[0].getTitle()+"x";
						}else if(!file[0].getTitle().contains(".xlsx")){
							createFile(file[0].getTitle()+".xlsx",response.getEntity().getContent(),totalSize);
							return file[0].getTitle();
						}else{
							createFile(file[0].getTitle(),response.getEntity().getContent(),totalSize);
							return file[0].getTitle();}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						handleException(e);
						e.printStackTrace();
					}
				}
			}
			else if(file[0].getMimeType().equalsIgnoreCase("application/vnd.google-apps.presentation")){
				if(file[0].getExportLinks().get("application/vnd.openxmlformats-officedocument.presentationml.presentation")!=null){
					try {
						HttpClient client = new DefaultHttpClient();//application/pdf
						HttpGet get = new HttpGet(file[0].getExportLinks().get("application/vnd.openxmlformats-officedocument.presentationml.presentation"));
						get.setHeader("Authorization", "Bearer " + accToken);
						org.apache.http.HttpResponse response = client.execute(get);
						totalSize=response.getEntity().getContentLength();
						if(file[0].getTitle().contains(".ppt")&&!file[0].getTitle().contains(".pptx")){
							createFile(file[0].getTitle()+"x",response.getEntity().getContent(),totalSize);
							return file[0].getTitle()+"x";
						}else if(!file[0].getTitle().contains(".pptx")){
							createFile(file[0].getTitle()+".pptx",response.getEntity().getContent(),totalSize);
							return file[0].getTitle()+".pptx";
						}
						else{
							createFile(file[0].getTitle(),response.getEntity().getContent(),totalSize);
							return file[0].getTitle();}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						handleException(e);
						e.printStackTrace();
					}
				}
			}
			return null;
		}

		protected void onPostExecute(String filename){
			Toast.makeText(cbt,filename+" Downloaded",Toast.LENGTH_LONG).show();
		}

	}

	private class readDriveFileTask extends
	AsyncTask<String, Integer, List<File>> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pdialog = new ProgressDialog(cbt);
			pdialog.setTitle("DriveNote");
			pdialog.setMessage("Loading File, Please wait..");
			pdialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pdialog.show();
		}

		@Override
		protected List<File> doInBackground(String... parentId) {
			// TODO Auto-generated method stub
			List<File> result = new ArrayList<File>();
			parentId[0] = "'" + parentId[0] + "'";
			Files.List request = null;
			try {
				request = service.files().list()
						.setQ(parentId[0] + " in parents and trashed=false");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				handleException(e1);
				e1.printStackTrace();
			}
			do {
				try {
					// System.out.println(request.getQ());
					FileList files = request.execute();
					result.addAll(files.getItems());
					request.setPageToken(files.getNextPageToken());
				} catch (IOException e) {
					handleException(e);
					request.setPageToken(null);
				}
			} while (request.getPageToken() != null
					&& request.getPageToken().length() > 0);

			return result;
		}

		@Override
		protected void onPostExecute(List<File> a) {
			pdialog.dismiss();
		}

	}

	private class readShareFileTask extends
	AsyncTask<String, Integer, List<File>> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pdialog = new ProgressDialog(cbt);
			pdialog.setTitle("DriveNote");
			pdialog.setMessage("Opening Share");
			pdialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pdialog.show();
		}
		@Override
		protected List<File> doInBackground(String... parentId) {
			// TODO Auto-generated method stub
			List<File> result = new ArrayList<File>();
			parentId[0] = "'" + parentId[0] + "'";
			Files.List request = null;
			try {
				request = service.files().list()
						.setQ("sharedWithMe");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				handleException(e1);
				e1.printStackTrace();
			}
			do {
				try {
					// System.out.println(request.getQ());
					FileList files = request.execute();
					result.addAll(files.getItems());
					request.setPageToken(files.getNextPageToken());
				} catch (IOException e) {
					handleException(e);
					request.setPageToken(null);
				}
			} while (request.getPageToken() != null
					&& request.getPageToken().length() > 0);

			return result;
		}

		@Override
		protected void onPostExecute(List<File> a) {
			pdialog.dismiss();
		}

	}

	private class RenameFiletask extends
	AsyncTask<String, Integer, String> {

		@Override
		protected String doInBackground(String... params) {
			// TODO Auto-generated method stub
			try {
				File file = new File();
				file.setTitle(params[1]);

				// Rename the file.
				Files.Patch patchRequest = service.files().patch(params[0], file);
				patchRequest.setFields("title");
				patchRequest.execute();
				return "OK";
			} catch (IOException e) {
				handleException(e);
				System.out.println("An error occurred: " + e);
				return null;
			}
		}

		@Override
		protected void onPostExecute(String res) {
			if(res.equalsIgnoreCase("ok")){
				try {
					temp.clear();
					temp = new readDriveFileTask().execute(path[level]).get();
					displayFile(temp);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}

	}

	private class TrashFiletask extends
	AsyncTask<String, Integer, String> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pdialog = new ProgressDialog(cbt);
			pdialog.setTitle("DriveNote");
			pdialog.setMessage("Deleting File,Please Wait");
			pdialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pdialog.show();
		}
		@Override
		protected String doInBackground(String... params) {
			// TODO Auto-generated method stub
			try {
				service.files().trash(params[0]).execute();
				return "OK";
			} catch (IOException e) {
				handleException(e);
				System.out.println("An error occurred: " + e);
				return null;
			}
		}

		@Override
		protected void onPostExecute(String res) {
			pdialog.dismiss();
			if(res.equalsIgnoreCase("ok")){
				try {
					temp.clear();
					temp = new readDriveFileTask().execute(path[level]).get();
					displayFile(temp);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}


	/*
	 * public List<File> retrieveDriveFile(String parentId) throws IOException{
	 * List<File> result = new ArrayList<File>(); parentId="'"+parentId+"'";
	 * Files.List request =
	 * service.files().list().setQ(parentId+" in parents and trashed=false");
	 * 
	 * do { try { FileList files = request.execute();
	 * 
	 * result.addAll(files.getItems());
	 * request.setPageToken(files.getNextPageToken()); } catch (IOException e) {
	 * handleException(e); request.setPageToken(null); } } while
	 * (request.getPageToken() != null && request.getPageToken().length() > 0);
	 * 
	 * return result; }
	 */// buat referensi di buku koding ini cuma isa jalan di 2.3 masuk honey
	// sama ice udah ga isa soale
	// ics honey langsung kill process yg makan wkt lama dikit, bt juga ga
	// boleh diakses tanpa run background
	// harus di background kl ics

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		String MimeType = temp.get(position).getMimeType();
		if (MimeType.equalsIgnoreCase("application/vnd.google-apps.folder")) {
			String fileId = temp.get(position).getId();
			path[++level]= temp.get(position).getId();
			try {
				temp.clear();
				temp = new readDriveFileTask().execute(fileId).get();
				displayFile(temp);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			}
		} else {
			//new DownloadFile().execute(temp.get(position));
			// Toast.makeText(cbt, temp.get(position).getFileExtension(),
			// Toast.LENGTH_LONG).show();

			Toast.makeText(cbt, temp.get(position).getAlternateLink(),
					Toast.LENGTH_LONG).show(); Intent i = new
					Intent(Intent.ACTION_VIEW);
					i.setData(Uri.parse(temp.get(position).getAlternateLink()));
					startActivity(i);

		}
		//Toast.makeText(cbt, MimeType, Toast.LENGTH_LONG).show();
	}

	public void onBackPressed(){
		if(level>0){
			String fileId=path[--level];
			try {
				temp.clear();
				temp = new readDriveFileTask().execute(fileId).get();
				displayFile(temp);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			}
		}
		else{
			super.finish();
		}
	}

	public void addItem(View v){
		if(((CheckBox) v).isChecked())
		{
			if(db.IsDriveExist(v.getTag().toString(), accountName)==true){
				db.SetSyncTrue("DRIVE", v.getTag().toString(), accountName);
			}else if(db.IsDriveExist(v.getTag().toString(), accountName)==false){
				db.addDrive(new CDriveFile(syncIndex++, v.getTag().toString(), accountName));}

			SharedPreferences settings = getSharedPreferences(PREF, 0);
			SharedPreferences.Editor editor = settings.edit();
			editor.putInt("syncIndex",syncIndex);
			editor.commit(); 
		}
		else if(!((CheckBox) v).isChecked())
		{
			db.SetSyncFalse("DRIVE", v.getTag().toString(), accountName);
		}

	}
	
	private void createFilewithExtension(String filename,InputStream in,Long totalSize,String mime) throws FileNotFoundException{
		OutputStream out=new FileOutputStream(new java.io.File(
				Environment.getExternalStorageDirectory()
				.getAbsolutePath()+"/"+filename+".tmp"));
		int read = 0;
		int sizeCount=0;
		byte[] bytes = new byte[1024];
		java.io.File objfile=new java.io.File(
				Environment.getExternalStorageDirectory()
				.getAbsolutePath()+"/"+filename+".tmp");
		try {
			while ((read = in.read(bytes)) != -1) {
				sizeCount+=read;
				out.write(bytes, 0, read);
			}
			if(sizeCount==totalSize||totalSize==-1){
				objfile.renameTo(new java.io.File(objfile.getAbsolutePath().substring(0, objfile.getAbsolutePath().length()-4)));
			}
			else{
				objfile.delete();
			}
			objfile=new java.io.File(
				Environment.getExternalStorageDirectory()
				.getAbsolutePath()+"/"+filename);
			if(!filename.contains(".")){
				objfile.renameTo(new java.io.File(objfile.getAbsolutePath()+"."+MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)));
			}
			System.out.println(sizeCount+"="+totalSize);
			in.close();
			out.flush();
			out.close();
			System.out.println("Downloaded");
			//Toast.makeText(cbt, filename+" Downloaded!",Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}


	private void createFile(String filename,InputStream in,Long totalSize) throws FileNotFoundException{
		OutputStream out=new FileOutputStream(new java.io.File(
				Environment.getExternalStorageDirectory()
				.getAbsolutePath()+"/"+filename+".tmp"));
		int read = 0;
		int sizeCount=0;
		byte[] bytes = new byte[1024];
		java.io.File objfile=new java.io.File(
				Environment.getExternalStorageDirectory()
				.getAbsolutePath()+"/"+filename+".tmp");
		try {
			while ((read = in.read(bytes)) != -1) {
				sizeCount+=read;
				out.write(bytes, 0, read);
			}
			if(sizeCount==totalSize||totalSize==-1){
				objfile.renameTo(new java.io.File(objfile.getAbsolutePath().substring(0, objfile.getAbsolutePath().length()-4)));
			}
			else{
				objfile.delete();
			}
			System.out.println(sizeCount+"="+totalSize);
			in.close();
			out.flush();
			out.close();
			System.out.println("Downloaded");
			//Toast.makeText(cbt, filename+" Downloaded!",Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}


	public void displayFile(List<File> temp) {
		// System.out.println(temp.size());
		if(sortType==1){
			Collections.sort(temp,new Comparator<File>(){
				public int compare(File f1,File f2){
					return f1.getTitle().compareToIgnoreCase(f2.getTitle());
				}
			});}
		else if(sortType==2){
			Collections.sort(temp,new Comparator<File>(){
				public int compare(File f1,File f2){
					return f1.getTitle().compareToIgnoreCase(f2.getTitle());
				}
			});
			Collections.sort(temp,new Comparator<File>(){
				public int compare(File f1,File f2){
					return f1.getMimeType().compareToIgnoreCase(f2.getMimeType());
				}
			});}
		fileList.clear();
		for (File f : temp) {
			if (f.getTitle() != null) {
				fileList.add(f.getTitle());
			}
		}
		setListAdapter(new DriveDataAdapter(cbt, temp));
		// setListAdapter(new ArrayAdapter<String>
		// (this,android.R.layout.simple_list_item_1,fileList));

	}

	// ...

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, "Switch Account");
		menu.add(0,2,3,"Sort By");
		menu.add(0,3,2,"Shared");
		menu.add(0, 1, 1, "Create Document");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			showDialog(DIALOG_ACCOUNTS);
			return true;
		case 1:
			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			alert.setTitle("DriveNote");
			alert.setMessage("Document name:");

			// Set an EditText view to get user input
			final EditText input = new EditText(this);
			alert.setView(input);

			alert.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,
						int whichButton) {
					Editable value = input.getText();
					try {
						new createDocumentTask().execute(String
								.valueOf(value)).get();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ExecutionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			});

			alert.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,
						int whichButton) {
					return;
				}
			});

			alert.show();
			return true;
		case 2:     CharSequence[] sortMtd={"By Name", "By Latest", "By Type"};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Pick Sort Method");
		builder.setItems(sortMtd, new OnClickListener() {									
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				if(which==0){
					sortType=1;
				}
				else if(which==1){
					sortType=3;
				}
				else if(which==2){
					sortType=2;
				}
				try {
					temp.clear();
					temp = new readDriveFileTask().execute(path[level]).get();
					displayFile(temp);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				}
				return;
			}
		});
		builder.create().show();
		break;

		case 3:

			try {
				temp.clear();
				temp = new readShareFileTask().execute("now").get();
				displayFile(temp);
				++level;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			break;
		}
		return false;
	}

	void handleException(Exception e) {
		e.printStackTrace();
		if (e instanceof HttpResponseException) {
			int statusCode = ((HttpResponseException) e).getStatusCode();
			// TODO: should only try this once to avoid infinite loop
			if (statusCode == 401) {
				new gotAccountTask().execute(true);
				finish();
				startActivity(getIntent());
				return;
			}
		}
		Log.e("Handle", e.getMessage(), e);
	}


}
