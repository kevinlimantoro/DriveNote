package kevin.thesis.drivenote;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Toast;

public class OfflineList extends ListActivity{
	DatabaseHandler db;
	private int syncIndex;
	private String accountName;
	private List<String> fileList;
	private static final String PREF = "MyPrefs";
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences settings = getSharedPreferences(PREF, 0);
		syncIndex=settings.getInt("syncIndex", 0);
		accountName = settings.getString("accountName", null);
        db=new DatabaseHandler(this);
        List<CLocalFile> tempLokal = new ArrayList<CLocalFile>();
		tempLokal = db.getAllLokal2(accountName);
        fileList=new ArrayList<String>();
        for(CLocalFile c:tempLokal){
        	if(!new File(c.getFilePath()).isDirectory()&&new File(c.getFilePath()).exists()){
        	fileList.add(c.getFilePath());}
        }
        setListAdapter(new DataArrayAdapter(this,fileList,1));
	}
	
	public static int getstatus(){
		return 1;
	}
	
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
			File selected = new File(fileList.get(position));
			//System.out.println(absolutePath);
			//  tv.setText(absolutePath);
			//selected=new File(absolutePath);
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
	
	public void addItem(View v){
		//Toast.makeText(this,  v.getTag().toString(),Toast.LENGTH_SHORT).show();
			if(((CheckBox) v).isChecked())
			{
					if(db.isExist(v.getTag().toString(), accountName)==true){
						db.SetSyncTrue("LOKAL", v.getTag().toString(), accountName);
					}else if(db.isExist(v.getTag().toString(), accountName)==false){
						db.addLokal(new CLocalFile(syncIndex++, v.getTag().toString(), accountName));}
				
				SharedPreferences settings = getSharedPreferences(PREF, 0);
				SharedPreferences.Editor editor = settings.edit();
				editor.putInt("syncIndex",syncIndex);
				editor.commit(); 
			}
			else if(!((CheckBox) v).isChecked())
			{
				db.SetSyncFalse("LOKAL", v.getTag().toString(), accountName);
			}
	}

}
