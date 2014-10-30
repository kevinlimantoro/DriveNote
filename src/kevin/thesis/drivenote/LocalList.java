package kevin.thesis.drivenote;



import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Adapter;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class LocalList extends ListActivity {

	private static final int progress_bar_type = 0;
	private List<String> fileList = new ArrayList<String>();
	private String backPath,currentPath;
	private int level=0;
	private int sortType=1;//1,2 by name ascend,des  3,4 by date asc,desc
	private int copyTotal=0;
	private int buffSize;
	private static int LOKALMODE=0;
	private static int SYNCMODE=1;
	private static int MYMODE;
	private DatabaseHandler db;
	private static final String PREF = "MyPrefs";
	private int syncIndex;
	private String accountName;
	private ProgressDialog pDialog;
	private Parcelable[] state=new Parcelable[20];
	private static List<File> selectedList=new ArrayList<File>();
	private File contextSelected;
	private int CopyMode=0;//0 not copy anything, 1 copy 2 cut
	//private TextView tv=(TextView) findViewById(R.id.titleManager);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		currentPath=Environment
				.getExternalStorageDirectory()
				.getAbsolutePath();
		ListDir(new File(currentPath));
		registerForContextMenu(getListView());
		MYMODE=LOKALMODE;
		db=new DatabaseHandler(this);
		SharedPreferences settings = getSharedPreferences(PREF, 0);
		syncIndex=settings.getInt("syncIndex", 0);
		accountName = settings.getString("accountName", null); 
		// tv.setText(Environment
		//  .getExternalStorageDirectory()
		//.getAbsolutePath());

	}

	public static int getstatus(){
		return MYMODE;
	}
	
	public static boolean isContextAvailable(String fp){
		for(int i=0;i<selectedList.size();i++){
			if(fp.compareToIgnoreCase(selectedList.get(i).getAbsolutePath())==0){
				return true;
			}
		}
		return false;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		if(fileList.get(position)=="Up"){
			Up1Level();
		}
		else{
			File selected = new File(fileList.get(position));
			//System.out.println(absolutePath);
			//  tv.setText(absolutePath);
			//selected=new File(absolutePath);
			if(selected.isDirectory()){
				//currentPath=absolutePath;
				//fungsi dibwawah ini untuk simpen posisi scroll
				
				state[level]=getListView().onSaveInstanceState();
				level++;
				currentPath = fileList.get(position);
				backPath = currentPath.
						substring(0,currentPath.lastIndexOf(File.separator));
				ListDir(selected);
			}
			else {
				Uri selectedUri = Uri.fromFile(selected);
				String fileExtension 
				= MimeTypeMap.getFileExtensionFromUrl(selectedUri.toString());
				String mimeType 
				= MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);

				// Toast.makeText(AndroidListFilesActivity.this, 
				//  "FileExtension: " + fileExtension + "\n" +
				// "MimeType: " + mimeType, 
				//Toast.LENGTH_LONG).show();

				//Start Activity to view the selected file
				if(mimeType!=null)
				{
					Intent intent;
					intent = new Intent(Intent.ACTION_VIEW);
					intent.setDataAndType(selectedUri, mimeType);
					startActivity(intent);
				}
				else
				{
					Toast.makeText(this, "Cannot Find Application to open this file", Toast.LENGTH_LONG).show();
				}
			}
		}
		//get selected items
		//String selectedValue = (String) getListAdapter().getItem(position);
		//Toast.makeText(this, selectedValue, Toast.LENGTH_SHORT).show();

	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		// Get the info on which item was selected
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;

		// Get the Adapter behind your ListView (this assumes you're using
		// a ListActivity; if you're not, you'll have to store the Adapter yourself
		// in some way that can be accessed here.)
		Adapter adapter = getListAdapter();

		// Retrieve the item that was clicked on
		//String item = (String) adapter.getItem(info.position);
		//Toast.makeText(this, item, Toast.LENGTH_LONG).show();



		//create context menu uda isa ndapetin nama file tinggal rename yaitu copy menuju file baru yg di inputkan terus delete
		//dest+=file.separator+textinput;
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.setHeaderTitle("Edit Item");
		menu.add(0,0, 1, "Rename File");
		menu.add(0, 1, 2, "Copy File");
		menu.add(0, 2, 3, "Cut File");
		menu.add(0, 3, 4, "Delete File");
		menu.add(0, 4, 5, "Sync this item");
		menu.add(0, 5, 6, "Property ");
		menu.add(0, 6, 7, "Cancel Action");

		String item = (String) adapter.getItem(info.position);
		contextSelected=new File(item);
	}
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case progress_bar_type:
			pDialog = new ProgressDialog(this);
			pDialog.setMessage("Paste file. Please wait...");
			pDialog.setIndeterminate(false);
			pDialog.setMax(100);
			pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			pDialog.setCancelable(true);
			pDialog.show();
			return pDialog;
		default:
			return null;
		}
	}

	public boolean onContextItemSelected(MenuItem m)
	{switch (m.getItemId())
		{
		case 0:
			if(!db.isLokalSynced(contextSelected.getAbsolutePath(), accountName)){
			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			alert.setTitle("Rename");
			alert.setMessage("Rename File to");

			// Set an EditText view to get user input 
			final EditText input = new EditText(this);
			input.setText(contextSelected.getName());
			alert.setView(input);
			input.setInputType(InputType.TYPE_CLASS_TEXT);
			alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					Editable value = input.getText();
					contextSelected.renameTo(new File(currentPath+File.separator+value));
					ListDir(new File(currentPath));
				}
			});

			alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					return;
				}
			});

			alert.show();}
			else{
				Toast.makeText(this, "Cannot Rename Sync File", Toast.LENGTH_LONG).show();
			}
			return true;
		case 1:
			selectedList.add(contextSelected);
			CopyMode=1;
			copyTotal=selectedList.size();
			Toast.makeText(getApplicationContext(), "Files Copied", Toast.LENGTH_LONG).show();
			return true;
		case 2:
			selectedList.add(contextSelected);
			CopyMode=2;
			copyTotal=selectedList.size();
			Toast.makeText(getApplicationContext(), "Files Moved", Toast.LENGTH_LONG).show();
			return true;
		case 3:
			selectedList.add(contextSelected);
			deleteFile(selectedList);
			selectedList.clear();
			ListDir(new File(currentPath));
			return true;
		case 4:
			if(!contextSelected.isDirectory())
			{db.addLokal(new CLocalFile(syncIndex++, contextSelected.getAbsolutePath(), accountName));}
			else if(contextSelected.isDirectory())
			{
				List<File> a=new ArrayList<File>();
				a.clear();
				a.add(contextSelected);
				multiLevelAdd(a);
			}
			SharedPreferences settings = getSharedPreferences(PREF, 0);
			SharedPreferences.Editor editor = settings.edit();
			editor.putInt("syncIndex",syncIndex);
			editor.commit(); 
			return true;
		case 5:
			Intent i=new Intent(this,FileProperty.class);
			i.putExtra("file", contextSelected.getAbsolutePath());
			startActivity(i);
			return true;
		case 6:
			return false;
		}
	return false;	
	}

	//naek ke folder atase
	public void Up1Level(){
		File back=new File(backPath);
		currentPath=backPath;
		backPath = backPath.
				substring(0,backPath.lastIndexOf("/"));
		level--;
		ListDir(back);
	}

	public void onBackPressed(){
		if(level>0){
			Up1Level();
		}
		else{
			super.finish();
		}
	}

	void ListDir(File f){
		File[] files = f.listFiles();
		fileList.clear();
		//if(lagi copy jangan clear; di clear kalo delete aja
		if(CopyMode==0)
			selectedList.clear();
		for (File file : files){
			if(!file.isHidden())
				fileList.add(file.getAbsolutePath());  
		}
		if(sortType<3)
			Collections.sort(fileList,new Comparator<String>(){
				public int compare(String f1,String f2){
					if(sortType==1)
						return f1.compareToIgnoreCase(f2);
					else
						return f1.compareToIgnoreCase(f2)*-1;
				}
			});
		else if(sortType<5)
			Collections.sort(fileList,new Comparator<String>(){
				public int compare(String f1,String f2){
					if(sortType==3)
						return Long.valueOf((new File(f1).lastModified())).compareTo((new File(f2).lastModified()));
					else
						return Long.valueOf((new File(f1).lastModified())).compareTo((new File(f2).lastModified()))*-1;
				}
			});
		else if(sortType==5){
			Collections.sort(fileList,new Comparator<String>(){
				public int compare(String f1,String f2){
						return f1.compareToIgnoreCase(f2);
				}
			});
			Collections.sort(fileList,new Comparator<String>(){
				public int compare(String f1,String f2){
					Uri selectedUri = Uri.fromFile(new File(f1));
					String fileExtension 
					= MimeTypeMap.getFileExtensionFromUrl(selectedUri.toString());
					String mimeType 
					= MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
					selectedUri = Uri.fromFile(new File(f2));
					fileExtension 
					= MimeTypeMap.getFileExtensionFromUrl(selectedUri.toString());
					String mimeType2
					= MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
					if(new File(f1).isDirectory()){
						if(new File(f2).isDirectory())
							return 0;
						return 1;
					}
					else if(new File(f2).isDirectory()){
						if(new File(f1).isDirectory())
							return 0;
						return -1;
					}
					else if(mimeType==null){
						if(mimeType2==null)
							return 0;
						return 1;
					}
					else if(mimeType2==null){
						if(mimeType==null)
							return 0;
						return -1;
					}
					else{
				    return mimeType.compareToIgnoreCase(mimeType2);}
				}
			});
		}
		if(level>0){
			fileList.add(0,"Up");
		}


		//TextView path=new TextView(this);
		// path.setText(currentPath);
		//getListView().addHeaderView(path);
		setTitle(currentPath);
		setListAdapter(new DataArrayAdapter(this,fileList,0));
		//fungsi dibwawah ini untuk baca posisi scroll
		if(state[level]!=null)
			getListView().onRestoreInstanceState(state[level]);
	}

	public void addItem(View v){
		//Toast.makeText(this,  v.getTag().toString(),Toast.LENGTH_SHORT).show();
		if(MYMODE==LOKALMODE){
			if(((CheckBox) v).isChecked())
			{
				if(CopyMode>0)
				{
					if(!currentPath.equalsIgnoreCase(selectedList.get(0).getPath().substring(0, selectedList.get(0).getPath().lastIndexOf(File.separator))))
					{
						CopyMode=0;
						copyTotal=0;
						selectedList.clear();
					}
				}
				selectedList.add(new File(v.getTag().toString()));
			}
			else if(!((CheckBox) v).isChecked())
			{selectedList.remove(new File(v.getTag().toString()));}
			//Toast.makeText(this,  v.getTag().toString(),Toast.LENGTH_SHORT).show();
		}
		else if(MYMODE==SYNCMODE){
			if(((CheckBox) v).isChecked())
			{
				if(!new File(v.getTag().toString()).isDirectory())
				{
					if(db.isExist(v.getTag().toString(), accountName)==true){
						db.SetSyncTrue("LOKAL", v.getTag().toString(), accountName);
					}else if(db.isExist(v.getTag().toString(), accountName)==false){
						db.addLokal(new CLocalFile(syncIndex++, v.getTag().toString(), accountName));}
				}
				else if(new File(v.getTag().toString()).isDirectory())
				{
					List<File> a=new ArrayList<File>();
					a.clear();
					a.add(new File(v.getTag().toString()));
					multiLevelAdd(a);
				}
				SharedPreferences settings = getSharedPreferences(PREF, 0);
				SharedPreferences.Editor editor = settings.edit();
				editor.putInt("syncIndex",syncIndex);
				editor.commit(); 
			}
			else if(!((CheckBox) v).isChecked())
			{
				if(!new File(v.getTag().toString()).isDirectory())
				{db.SetSyncFalse("LOKAL", v.getTag().toString(), accountName);}
				else if(new File(v.getTag().toString()).isDirectory())
				{
					List<File> b=new ArrayList<File>();
					b.clear();
					b.add(new File(v.getTag().toString()));
					multiLevelRemove(b);
				}
			}
		}
	}

	private void multiLevelRemove(List<File> addlist){
		for(File f:addlist){
			if(f.isDirectory())
			{
				File[] files = f.listFiles();
				List<File> tempList = new ArrayList<File>();
				for (File file : files){
					tempList.add(file);
				}
				if(tempList.size()>0)
					multiLevelRemove(tempList);
				else if (tempList.size()==0)
					db.SetSyncFalse("LOKAL", f.getAbsolutePath(), accountName);
			}

			db.SetSyncFalse("LOKAL", f.getAbsolutePath(), accountName);
		}
	}

	private void multiLevelAdd(List<File> addlist){
		for(File f:addlist){
			if(f.isDirectory())
			{
				File[] files = f.listFiles();
				List<File> tempList = new ArrayList<File>();
				for (File file : files){
					tempList.add(file);
				}
				if(tempList.size()>0)
					multiLevelAdd(tempList);
				else if (tempList.size()==0){
					if(db.isExist(f.getAbsolutePath(), accountName)==true){
						db.SetSyncTrue("LOKAL", f.getAbsolutePath(), accountName);
					}else if(db.isExist(f.getAbsolutePath(), accountName)==false){
						db.addLokal(new CLocalFile(syncIndex++, f.getAbsolutePath(), accountName));}
				}
				//db.addLokal(new CLocalFile(syncIndex++, f.getAbsolutePath(), accountName));
			}
			if(db.isExist(f.getAbsolutePath(), accountName)==true){
				db.SetSyncTrue("LOKAL", f.getAbsolutePath(), accountName);
			}else if(db.isExist(f.getAbsolutePath(), accountName)==false){
				db.addLokal(new CLocalFile(syncIndex++, f.getAbsolutePath(), accountName));}
			//db.addLokal(new CLocalFile(syncIndex++, f.getAbsolutePath(), accountName));
		}
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_drive_note, menu);
		return true;
	}

	public void deleteFile(List<File> srcList){
		for(File f : srcList)
		{
			if(f.isDirectory())
			{
				File[] files = f.listFiles();
				List<File> tempList = new ArrayList<File>();
				for (File file : files){
					tempList.add(file);
				}
				if(tempList.size()>0)
					deleteFile(tempList);
				else if (tempList.size()==0)
					f.delete();
			}

			f.delete();
		}
	}

	public void pasteFile(List<File> srcList,String dest,int mode) throws IOException{//1 copy,2 cut
		for(File f : srcList)
		{
			//  InputStream in = new FileInputStream(f);
			// System.out.println(f.getPath());
			String saveDest=dest;
			saveDest+=File.separator+f.getName();
			if(new File(saveDest).exists())
			{
				saveDest=dest;
				saveDest+=File.separator+"Copy of "+f.getName();
			}
			//mbikin rename berdasarkan move cuma namae user yang nyesuaino.. milih cuman 1 aja file
			if(f.isDirectory())
			{
				new File (saveDest).mkdir();
				File[] files = f.listFiles();
				List<File> tempList = new ArrayList<File>();
				for (File file : files){
					tempList.add(file);
				}
				Log.d("e", String.valueOf(tempList.size()));
				if(tempList.size()>0)
					pasteFile(tempList,saveDest,CopyMode);
			}
			//kl ga dikasih else bisa sangat bermasalah
			else{
				buffSize=(int) (f.length()/40);
				String[] toSent = new String[2];
				toSent[0]=saveDest;
				toSent[1]=f.getPath();
				new PasteFileTask().execute(toSent);
			}
		}
	}

	private class PasteFileTask extends AsyncTask<String, String, String> {


		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			showDialog(progress_bar_type);
		}


		@Override
		protected String doInBackground(String... saveDest) {
			int count;
			try {
				InputStream in=new FileInputStream(new File(saveDest[1]));
				OutputStream out = new FileOutputStream(new File(saveDest[0]));
				// Transfer bytes from in to out
				byte[] buf = new byte[(int) (buffSize)];
				count=0;
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
					count+=len;
					publishProgress(""+(int)((count*100)/(buffSize*40)));
					//System.out.println(len);
				}
				in.close();
				out.close();
				// 
			}
			finally{
				return null;
			}
		}


		/**
		 * Updating progress bar
		 * */
		protected void onProgressUpdate(String... progress) {
			// setting progress percentage
			pDialog.setProgress(Integer.parseInt(progress[0]));
		}

		/**
		 * After completing background task
		 * Dismiss the progress dialog
		 * **/
		@Override
		protected void onPostExecute(String file_url) {
			dismissDialog(progress_bar_type);
			ListDir(new File(currentPath));
		}

	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId())
		{
		case R.id.sortMenu:     CharSequence[] sortMtd={"Asc By Name", "Desc By Name", "Asc By Date", "Desc By Date","Group By Type"};
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
					sortType=2;
				}
				else if(which==2){
					sortType=3;
				}
				else if(which==3){
					sortType=4;
				}
				else if(which==4){
					sortType=5;
				}
				ListDir(new File(currentPath));
				return;
			}
		});
		builder.create().show();
		break;
		case R.id.exitMenu:     super.finish();
		break;
		case R.id.SyncToogle:	MYMODE++;
		MYMODE=MYMODE%2;
		ListDir(new File(currentPath));
		if(MYMODE==LOKALMODE)
		{Toast.makeText(getApplicationContext(), "On Lokal List Mode", Toast.LENGTH_SHORT).show();}
		else if(MYMODE==SYNCMODE)
		{
			if(accountName!=null){
				Toast.makeText(getApplicationContext(), "On Lokal Sync Selection Mode", Toast.LENGTH_SHORT).show();}
			else if(accountName==null){
				super.finish();
				Toast.makeText(getApplicationContext(), "Pick Account First before sync", Toast.LENGTH_SHORT).show();}
		}
		break;
		case R.id.createMenu:   
		CharSequence[] createOpt={"Folder", "Document"};
		AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
		builder2.setTitle("Pick Sort Method");
		final Context con=this;
		builder2.setItems(createOpt, new OnClickListener() {									
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				if(which==0){
					AlertDialog.Builder alert = new AlertDialog.Builder(con);
					alert.setTitle("Create Folder");
					alert.setMessage("Input Folder Name :");						
					// Set an EditText view to get user input 
					final EditText input = new EditText(con);
					input.setInputType(InputType.TYPE_CLASS_TEXT);
					alert.setView(input);								
					alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							Editable value = input.getText();
							File directory = new File(currentPath+File.separator+value);
							directory.mkdirs();
							ListDir(directory);
						}
					});							
					alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							return;
						}
					});								
					alert.show();
				}
				else if(which==1){
					AlertDialog.Builder alert = new AlertDialog.Builder(con);
					alert.setTitle("Create Document");
					alert.setMessage("Input Document Name :");						
					// Set an EditText view to get user input 
					final EditText input = new EditText(con);
					input.setInputType(InputType.TYPE_CLASS_TEXT);
					alert.setView(input);								
					alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							Editable value = input.getText();
							File directory = new File(currentPath+File.separator+value+".docx");
							try {
								directory.createNewFile();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							ListDir(new File(currentPath));
							Uri selectedUri = Uri.fromFile(directory);
							String fileExtension 
							= MimeTypeMap.getFileExtensionFromUrl(selectedUri.toString());
							String mimeType 
							= MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
							if(mimeType!=null)
							{
								Intent intent;
								intent = new Intent(Intent.ACTION_VIEW);
								intent.setDataAndType(selectedUri, mimeType);
								startActivity(intent);
							}
						}
					});							
					alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							return;
						}
					});								
					alert.show();
				}
				return;
			}
		});
		builder2.create().show();
		break;
		case R.id.EditMenu:     CharSequence[] EditList={"Copy"+" "+(selectedList.size())+" Files"
				, "Move"+" "+(selectedList.size())+" Files"
				, "Paste"+" "+copyTotal+" Files"
				, "Delete"+" "+selectedList.size()+" Files"};
		builder = new AlertDialog.Builder(this);
		builder.setTitle("Edit File");
		builder.setItems(EditList, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				if(which==0){
					if(selectedList.size()>0){
						CopyMode=1;
						copyTotal=selectedList.size();
						Toast.makeText(getApplicationContext(), "Files Copied", Toast.LENGTH_LONG).show();
					}
					else
						Toast.makeText(getApplicationContext(), "Nothing to copy", Toast.LENGTH_LONG).show();

				}
				else if(which==1){
					if(selectedList.size()>0){
						CopyMode=2;
						copyTotal=selectedList.size();
						Toast.makeText(getApplicationContext(), "Files Copied", Toast.LENGTH_LONG).show();
					}
					else
						Toast.makeText(getApplicationContext(), "Nothing to move", Toast.LENGTH_LONG).show();
				}
				else if(which==2){
					if(selectedList.size()>0){
						try {
							pasteFile(selectedList,currentPath,CopyMode);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if(CopyMode==2)
							deleteFile(selectedList);
						selectedList.clear();
						ListDir(new File(currentPath));
						CopyMode=0;
						copyTotal=0;}
					else{
						Toast.makeText(getApplicationContext(), "Nothing to paste", Toast.LENGTH_LONG).show();
					}
				}
				else if(which==3){
					if(selectedList.size()>0)
					{deleteFile(selectedList);
					selectedList.clear();
					ListDir(new File(currentPath));}
					else 
						Toast.makeText(getApplicationContext(), "Nothing to delete", Toast.LENGTH_LONG).show();
				}
				return;
			}
		});
		builder.create().show();
		break;
		}
		return true;
	}
}