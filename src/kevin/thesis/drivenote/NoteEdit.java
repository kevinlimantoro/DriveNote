package kevin.thesis.drivenote;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class NoteEdit extends Activity {
	private EditText mTitleText;
	private EditText mBodyText;
	private DatabaseHandler db;
	private File teks;
	private int syncIndex;
	private String accountName;
	private static final String PREF = "MyPrefs";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//mDbHelper = new NotesDbAdapter(this);
		//mDbHelper.open();
		setContentView(R.layout.note_edit);
		SharedPreferences settings = getSharedPreferences(PREF, 0);
		syncIndex=settings.getInt("syncIndex", 0);
		accountName = settings.getString("accountName", null);
		mTitleText = (EditText) findViewById(R.id.title);
		mBodyText = (EditText) findViewById(R.id.body);
		db=new DatabaseHandler(this);
		if(getIntent().getData()!=null){
			teks = new File(getIntent().getData().getPath());
			//Toast.makeText(this, teks.getAbsolutePath(), Toast.LENGTH_LONG).show();

			mTitleText.setText(teks.getAbsolutePath().substring(teks.getAbsolutePath().lastIndexOf("/")+1,
					teks.getAbsolutePath().length()-4));
			try {
				FileInputStream fIn = new FileInputStream(teks);
				BufferedReader myReader = new BufferedReader(
						new InputStreamReader(fIn));
				String aDataRow = "";
				String aBuffer = "";
				while ((aDataRow = myReader.readLine()) != null) {
					aBuffer += aDataRow + "\n";
				}
				mBodyText.setText(aBuffer);
				myReader.close();
			} catch (Exception e) {
				Toast.makeText(getBaseContext(), e.getMessage(),
						Toast.LENGTH_SHORT).show();
			}
			
		}

	}

	public void Save(View v){
		String title = mTitleText.getText().toString();
		String body = mBodyText.getText().toString();
		String FilePath=Environment.getExternalStorageDirectory()
				.getAbsolutePath()+"/"+title+".txt";
		if(!db.isExist(FilePath, accountName)&&title.length()>0){
			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			alert.setTitle("DriveNote");
			alert.setMessage("Do You Want To Sync This File To Drive");

			alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					try {
						saveState(true);
						finish();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});

			alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					try {
						saveState(false);
						finish();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});

			alert.show();
		}
		else if(body.length()>0&&title.length()==0){
			Toast.makeText(this, "You forget the file name", Toast.LENGTH_LONG).show();
		}
		else{
		finish();}
	}

	@Override
	protected void onPause() {
		super.onPause();
		System.out.println("PAUSE");
		try {
			saveState(false);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private void saveState(boolean Sync) throws IOException {
		String title = mTitleText.getText().toString();
		String body = mBodyText.getText().toString();

	
		if(title.length()>0){
			String FilePath=Environment.getExternalStorageDirectory()
					.getAbsolutePath()+"/"+title+".txt";
			File myFile = new File(FilePath);
			String[] result=body.split("\n");
			myFile.createNewFile();
			PrintWriter writer=new PrintWriter(myFile);
			for(int i=0;i<result.length;i++){
				System.out.println(result[i]);
				writer.println(result[i]);
			}
			writer.close();
			if(!db.isExist(FilePath, accountName)&&Sync==true){
				db.addLokal(new CLocalFile(syncIndex++, FilePath, accountName));
				SharedPreferences settings = getSharedPreferences(PREF, 0);
				SharedPreferences.Editor editor = settings.edit();
				editor.putInt("syncIndex",syncIndex);
				editor.commit(); 
			}
		}

	}
}
