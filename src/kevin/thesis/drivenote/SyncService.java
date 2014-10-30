package kevin.thesis.drivenote;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
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
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpRequest;
import com.google.api.client.http.json.JsonHttpRequestInitializer;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveRequest;
import com.google.api.services.drive.DriveScopes;

public class SyncService extends Service {
	private static final int CHOOSE_ACCOUNT = 0;
	private static final int DIALOG_ACCOUNTS = 0;
	private Account account;
	private DatabaseHandler db;
	private AccountManagerFuture<Bundle> amf;
	private AccountManager accMgr;
	private Account[] accounts;
	private String accountName,root;
	private Drive service;
	private ProgressDialog pdialog;
	private String tempModDate;
	private static final String PREF = "MyPrefs";
	private SyncService cbt = this;
	private String authTokenType = "oauth2:" + DriveScopes.DRIVE;
	private final android.os.Handler handler = null;
	public GoogleAccountManager accountManager;
	private int MaxRetries = 3;
	private List<File> temp = new ArrayList<File>();
	private List<String> fileList = new ArrayList<String>();
	private String accToken;
	static final String API_KEY = "AIzaSyATq1hMYwwfXxv186EJIcix3rld_PKFgow";

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public void onCreate() {
		db = new DatabaseHandler(this);
		accountManager = new GoogleAccountManager(this);
		accMgr = AccountManager.get(this);
		new gotAccountTask().execute(false);
		root=Environment.getExternalStorageDirectory()
				.getAbsolutePath();
		while(amf==null){
		}
		//new gotAccountTask().execute(true);
		System.out.println("onCreate()");
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
		}
		SyncDrive();
		stopSelf();
		// Toast.makeText(this, String.valueOf(db.getLokalCount(accountName)),
		// Toast.LENGTH_LONG).show();
	}

	private void SyncDrive() {
		// TODO Auto-generated method stub
		List<CLocalFile> tempLokal = new ArrayList<CLocalFile>();
		tempLokal = db.getAllLokal(accountName);
		int i = 0;
		while (i < tempLokal.size()) {
			System.out.println(String.valueOf(tempLokal.get(i).getID())+" "+accountName);
			if(db.IsDriveSyncThis(tempLokal.get(i).getID(), accountName)==true)
			{
				new SynchronizeFile().execute(tempLokal.get(i));
			}
			else if(db.IsDriveHaveThis(tempLokal.get(i).getID(), accountName)==false &&
					new File(tempLokal.get(i).getFilePath()).isDirectory()!=true) {
				String temp[]=new String[3];
				temp[0]=String.valueOf(tempLokal.get(i).getID());
				temp[1]=tempLokal.get(i).getFilePath();
				temp[2]=new DateTime(new File(tempLokal.get(i).getFilePath().toString()).lastModified()).toStringRfc3339();

				//System.out.println(db.getDriveCount(accountName));
				new UploadtoDrive().execute(temp);
			}
			i++;
		}
		List<CDriveFile> tempDrive=new ArrayList<CDriveFile>();
		tempDrive=db.getAllDrive(accountName);
		i = 0;
		while (i < tempDrive.size()) {
			System.out.println(String.valueOf(tempDrive.get(i).getID())+" "+accountName);
			if(db.IsLokalHaveThis(tempDrive.get(i).getID(), accountName)==false) {
				String[] temp=new String[2];
				temp[0]=tempDrive.get(i).getFileId();
				temp[1]=String.valueOf(tempDrive.get(i).getID());
				new DownloadFile().execute(temp);
			}
			i++;
		}
	}

	private class SynchronizeFile extends AsyncTask<CLocalFile, Integer, String>{
		protected String doInBackground(CLocalFile... file){
			CDriveFile dt=db.getDriveFile(file[0].getID(), accountName);
			try {
				com.google.api.services.drive.model.File drivefile=service.files().get(dt.getFileId()).execute();
				File lokal=new File(file[0].getFilePath());
				Uri selectedUri = Uri.fromFile(lokal);
				String fileExtension 
				= MimeTypeMap.getFileExtensionFromUrl(selectedUri.toString());
				String mimeType 
				= MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
				if(lokal.exists()==true){
					if(drivefile.getLabels().getTrashed()==false){
						//CEK REVISION DULU BARU TANGGAL, KL TANGGAL DULU BURUK SOALE GA FIX
						//if revisionDrive>revisionDB && Date Drive>DateLokal  soale kl berdasarkan revision tok, yg update
						//dari phone isa ga dianggap meski lebih baru dari revisi..
						String LokalDate=new DateTime(lokal.lastModified()).toStringRfc3339()+"Z";
						String DriveDate=drivefile.getModifiedDate().toStringRfc3339();
						LokalDate=LokalDate.substring(0,LokalDate.length()-5);
						DriveDate=DriveDate.substring(0,DriveDate.length()-5);
						System.out.println(lokal.getAbsolutePath()+" "+DriveDate+" k "+LokalDate);
						if(LokalDate.compareToIgnoreCase(DriveDate)<0)
						{System.out.println("BARUAN DARI DRIVE");
						new DownloadFile().execute(drivefile.getId());
						}
						else if(LokalDate.compareToIgnoreCase(DriveDate)>0)
						{System.out.println("BARUAN DARI PHONE");
						String temp2[]=new String[2];
						temp2[0]=file[0].getFilePath();
						temp2[1]=drivefile.getId();
						new UpdateDriveFile().execute(temp2);}
						else{
							System.out.println("SAMA");
						}
					}
				}
				if(lokal.exists()==false){
					db.removeLokal(file[0].getFilePath(), accountName);
				}
				if(drivefile.getLabels().getTrashed()==true){
					db.removeDrive(dt.getFileId(), accountName);
				}
				if(db.IsDriveHaveThis(file[0].getID(), accountName)==false &&
						new File(file[0].getFilePath()).isDirectory()!=true) {
					String temp[]=new String[3];
					temp[0]=String.valueOf(file[0].getID());
					temp[1]=file[0].getFilePath();
					temp[2]=new DateTime(new File(file[0].getFilePath().toString()).lastModified()).toString();

					//System.out.println(db.getDriveCount(accountName));
					new UploadtoDrive().execute(temp);
				}
				if(db.IsLokalHaveThis(file[0].getID(), accountName)==false) {
					String[] temp=new String[2];
					temp[0]=drivefile.getId();
					temp[1]=String.valueOf(file[0].getID());
					new DownloadFile().execute(temp);
				}
				//habis ngesync jgn lupa setlastmodified(di dua sisi isa kok ngelakuno itu)
			} catch (IOException e) {
				// TODO Auto-generated catch block
				handleException(e);
				e.printStackTrace();
			}
			return "OK";
		}
	}

	private class UploadtoDrive extends AsyncTask<String, Integer, CDriveFile> {
		@Override
		protected CDriveFile doInBackground(String... file) {
			// TODO Auto-generated method stub
			com.google.api.services.drive.model.File f=null;
			File a=new File(file[1]);
			com.google.api.services.drive.model.File body = new com.google.api.services.drive.model.File();
			body.setTitle(file[1].substring(file[1].lastIndexOf("/")+1, file[1].length()));
			Uri selectedUri = Uri.fromFile(a);
			String fileExtension 
			= MimeTypeMap.getFileExtensionFromUrl(selectedUri.toString());
			String mimeType 
			= MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
			body.setMimeType(mimeType);
			System.out.println(DateTime.parseRfc3339(file[2]));
			//body.setModifiedDate(DateTime.parseRfc3339(file[2]));
			FileContent mediaContent = new FileContent(mimeType, a);
			try {
				if(mimeType.contains("officedocument") || mimeType.equalsIgnoreCase("text/plain")){
					f = service.files().insert(body, mediaContent).setConvert(true).execute();
				}
				else{
					System.out.println(mimeType+"MASUK SINI");
					f = service.files().insert(body, mediaContent).execute();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				handleException(e);
			}
			String temp2[]=new String[2];
			temp2[0]=file[1];
			temp2[1]=f.getId();
			new UpdateDriveFile().execute(temp2);
			return new CDriveFile(Integer.parseInt(file[0]),f.getId(),accountName);
		}

		@Override
		protected void onPostExecute(CDriveFile result) {
			System.out.println("masuk");
			//new CountRevision().execute(result.getFileId());PASTI SATU KALO mari Upload
			db.addDrive(result);
			//pdialog.dismiss();
		}
	}
	//DISINI PERNAH ADA COUNTREVISION
	private class UpdateDriveFile extends AsyncTask<String, Integer, String> {
		protected String doInBackground(String... file) {
			// TODO Auto-generated method stub
			com.google.api.services.drive.model.File f=null;
			File a=new File(file[0]);
			Uri selectedUri = Uri.fromFile(a);
			String fileExtension 
			= MimeTypeMap.getFileExtensionFromUrl(selectedUri.toString());
			String mimeType 
			= MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
			com.google.api.services.drive.model.File body = new com.google.api.services.drive.model.File();
			body.setTitle(file[0].substring(file[0].lastIndexOf("/")+1, file[0].length()));
			body.setMimeType(mimeType);
			body.setModifiedDate(new DateTime(new DateTime(a.lastModified()).toStringRfc3339()+"Z"));
			System.out.println(new DateTime(new DateTime(a.lastModified()).toStringRfc3339()+"Z"));
			//body.setModifiedDate(DateTime.parseRfc3339(file[2]));
			FileContent mediaContent = new FileContent(mimeType, a);
			try {
				f = service.files().update(file[1],body,mediaContent).setSetModifiedDate(true).setUpdateViewedDate(false).execute();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				handleException(e);
			}
			return "OK";
		}

	}

	private class DownloadFile extends
	AsyncTask<String, Integer, String> {

		@Override
		protected String doInBackground(String... file) {
			// TODO Auto-generated method stub
			com.google.api.services.drive.model.File drivefile=null;
			try {
				drivefile = service.files().get(file[0]).execute();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			long totalSize=0;
			if(drivefile.getDownloadUrl()!=null){
				if (drivefile.getDownloadUrl() != null && drivefile.getDownloadUrl().length() > 0) {
					try {
						HttpClient client = new DefaultHttpClient();//application/pdf
						HttpGet get = new HttpGet(drivefile.getDownloadUrl());
						get.setHeader("Authorization", "Bearer " + accToken);
						org.apache.http.HttpResponse response = client.execute(get);
						totalSize=response.getEntity().getContentLength();
						if(!drivefile.getTitle().contains(".")){
							String mime=drivefile.getMimeType();
						createFile(drivefile.getTitle()+"."+MimeTypeMap.getSingleton().getExtensionFromMimeType(mime),drivefile.getModifiedDate().getValue(),response.getEntity().getContent(),totalSize);
						if(file.length>1){
							CLocalFile cl=new CLocalFile(Integer.parseInt(file[1]),
									root+"/"+drivefile.getTitle()+"."+MimeTypeMap.getSingleton().getExtensionFromMimeType(mime),accountName);
							db.addLokal(cl);}}
						else if(drivefile.getTitle().contains(".")){
						createFile(drivefile.getTitle(),drivefile.getModifiedDate().getValue(),response.getEntity().getContent(),totalSize);
						if(file.length>1){
							CLocalFile cl=new CLocalFile(Integer.parseInt(file[1]),
									root+"/"+drivefile.getTitle(),accountName);
							db.addLokal(cl);}}
						return drivefile.getTitle();
					} catch (IOException e) {
						handleException(e);
						e.printStackTrace();
					}
				}
			}
			else if(drivefile.getTitle().toLowerCase().contains(".txt")){
				if(drivefile.getExportLinks().get("text/plain")!=null){
					try {
						HttpClient client = new DefaultHttpClient();//application/pdf
						HttpGet get = new HttpGet(drivefile.getExportLinks().get("text/plain"));
						get.setHeader("Authorization", "Bearer " + accToken);
						org.apache.http.HttpResponse response = client.execute(get);
						totalSize=response.getEntity().getContentLength();
						if(!drivefile.getTitle().contains(".txt")){
							createFile(drivefile.getTitle()+".txt",drivefile.getModifiedDate().getValue(),response.getEntity().getContent(),totalSize);}
						else{
							createFile(drivefile.getTitle(),drivefile.getModifiedDate().getValue(),response.getEntity().getContent(),totalSize);
						}
						if(file.length>1){
							CLocalFile cl=new CLocalFile(Integer.parseInt(file[1]),
									root+"/"+drivefile.getTitle(),accountName);
							db.addLokal(cl);}
						return drivefile.getTitle();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						handleException(e);
						e.printStackTrace();
					}
				}
			}
			else if(drivefile.getMimeType().equalsIgnoreCase("application/vnd.google-apps.document")){
				if(drivefile.getExportLinks().get("application/vnd.openxmlformats-officedocument.wordprocessingml.document")!=null){
					try {
						HttpClient client = new DefaultHttpClient();//application/pdf
						HttpGet get = new HttpGet(drivefile.getExportLinks().get("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
						get.setHeader("Authorization", "Bearer " + accToken);
						org.apache.http.HttpResponse response = client.execute(get);
						totalSize=response.getEntity().getContentLength();
						if(drivefile.getTitle().contains(".doc")&&!drivefile.getTitle().contains(".docx")){
							createFile(drivefile.getTitle()+"x",drivefile.getModifiedDate().getValue(),response.getEntity().getContent(),totalSize);
							if(file.length>1){
								System.out.println("MASUK KE INPUT");
								CLocalFile cl=new CLocalFile(Integer.parseInt(file[1]),
										root+"/"+drivefile.getTitle()+"x",accountName);
								db.addLokal(cl);}
						}
						else if(!drivefile.getTitle().contains(".docx")){
							createFile(drivefile.getTitle()+".docx",drivefile.getModifiedDate().getValue(),response.getEntity().getContent(),totalSize);
							if(file.length>1){
								System.out.println("MASUK KE INPUT");
								CLocalFile cl=new CLocalFile(Integer.parseInt(file[1]),
										root+"/"+drivefile.getTitle()+".docx",accountName);
								db.addLokal(cl);}
						}		
						else{
							createFile(drivefile.getTitle(),drivefile.getModifiedDate().getValue(),response.getEntity().getContent(),totalSize);
							if(file.length>1){
								CLocalFile cl=new CLocalFile(Integer.parseInt(file[1]),
										root+"/"+drivefile.getTitle(),accountName);
								db.addLokal(cl);}
						}
						return drivefile.getTitle();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						handleException(e);
						e.printStackTrace();
					}
				}
			}
			else if(drivefile.getMimeType().equalsIgnoreCase("application/vnd.google-apps.spreadsheet")){
				if(drivefile.getExportLinks().get("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")!=null){
					try {
						HttpClient client = new DefaultHttpClient();//application/pdf
						HttpGet get = new HttpGet(drivefile.getExportLinks().get("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
						get.setHeader("Authorization", "Bearer " + accToken);
						org.apache.http.HttpResponse response = client.execute(get);
						totalSize=response.getEntity().getContentLength();
						if(drivefile.getTitle().contains(".xls")&&!drivefile.getTitle().contains(".xlsx")){
							createFile(drivefile.getTitle()+"x",drivefile.getModifiedDate().getValue(),response.getEntity().getContent(),totalSize);
							if(file.length>1){
								System.out.println("MASUK KE INPUT");
								CLocalFile cl=new CLocalFile(Integer.parseInt(file[1]),
										root+"/"+drivefile.getTitle()+"x",accountName);
								db.addLokal(cl);}
						}
						else if(!drivefile.getTitle().contains(".xlsx")){
							createFile(drivefile.getTitle()+".xlsx",drivefile.getModifiedDate().getValue(),response.getEntity().getContent(),totalSize);
							if(file.length>1){
								CLocalFile cl=new CLocalFile(Integer.parseInt(file[1]),
										root+"/"+drivefile.getTitle()+".xlsx",accountName);
								db.addLokal(cl);}
						}
						else{
							createFile(drivefile.getTitle(),drivefile.getModifiedDate().getValue(),response.getEntity().getContent(),totalSize);
							if(file.length>1){
								CLocalFile cl=new CLocalFile(Integer.parseInt(file[1]),
										root+"/"+drivefile.getTitle(),accountName);
								db.addLokal(cl);}
						}
						return drivefile.getTitle();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						handleException(e);
						e.printStackTrace();
					}
				}
			}
			else if(drivefile.getMimeType().equalsIgnoreCase("application/vnd.google-apps.presentation")){
				if(drivefile.getExportLinks().get("application/vnd.openxmlformats-officedocument.presentationml.presentation")!=null){
					try {
						HttpClient client = new DefaultHttpClient();//application/pdf
						HttpGet get = new HttpGet(drivefile.getExportLinks().get("application/vnd.openxmlformats-officedocument.presentationml.presentation"));
						get.setHeader("Authorization", "Bearer " + accToken);
						org.apache.http.HttpResponse response = client.execute(get);
						totalSize=response.getEntity().getContentLength();
						if(drivefile.getTitle().contains(".ppt")&&!drivefile.getTitle().contains(".pptx")){
							createFile(drivefile.getTitle()+"x",drivefile.getModifiedDate().getValue(),response.getEntity().getContent(),totalSize);
							if(file.length>1){
								System.out.println("MASUK KE INPUT");
								CLocalFile cl=new CLocalFile(Integer.parseInt(file[1]),
										root+"/"+drivefile.getTitle()+"x",accountName);
								db.addLokal(cl);}
						}
						else if(!drivefile.getTitle().contains(".pptx")){
							createFile(drivefile.getTitle()+".pptx",drivefile.getModifiedDate().getValue(),response.getEntity().getContent(),totalSize);
							if(file.length>1){
								CLocalFile cl=new CLocalFile(Integer.parseInt(file[1]),
										root+"/"+drivefile.getTitle()+".pptx",accountName);
								db.addLokal(cl);}
						}	
						else{
							createFile(drivefile.getTitle(),drivefile.getModifiedDate().getValue(),response.getEntity().getContent(),totalSize);
							if(file.length>1){
								CLocalFile cl=new CLocalFile(Integer.parseInt(file[1]),
										root+"/"+drivefile.getTitle(),accountName);
								db.addLokal(cl);}
						}
						return drivefile.getTitle();
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

	private void createFile(String filename,long date,InputStream in,long totalSize) throws FileNotFoundException{
		OutputStream out=new FileOutputStream(new java.io.File(
				root+"/"+filename+".tmp"));
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
		File a=new java.io.File(
				root+"/"+filename);
		System.out.println(date+" "+a.lastModified());
		while(a.exists()==false){
		}
		a.setLastModified(date);
	}


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
			}
			return null;
		}
	}

	private class gotAccountTask extends AsyncTask<Boolean, Integer, Void>{

		@Override
		protected Void doInBackground(Boolean... expired) {
			// TODO Auto-generated method stub
			Boolean tokenExpired=expired[0];
			SharedPreferences settings = getSharedPreferences(PREF, 0);
			accountName = settings.getString("accountName", null);
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
			}
			return null;
		}

	}

	void handleException(Exception e) {
		e.printStackTrace();
		if (e instanceof HttpResponseException) {
			int statusCode = ((HttpResponseException) e).getStatusCode();
			// TODO: should only try this once to avoid infinite loop
			if (statusCode == 401) {
				new gotAccountTask().execute(true);
				Intent i=new Intent(this,SyncService.class);
				startService(i);
				return;
			}
		}
		Log.e("Handle", e.getMessage(), e);
	}

}
