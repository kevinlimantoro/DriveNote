package kevin.thesis.drivenote;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHandler extends SQLiteOpenHelper {
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "DriveNoteDB";
	private static final String TABLE_LOKAL = "Lokal";
	private static final String KEY_ID = "SyncId";
	private static final String KEY_FILEPATH = "FilePath";
	private static final String KEY_USER = "UserName";
	private static final String TABLE_DRIVE = "Drive";
	private static final String KEY_FILEID = "FileId";
	private static final String KEY_SYNCBOOL= "Syncing";

	public DatabaseHandler(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		String CREATE_LOKAL_TABLE = "CREATE TABLE " + TABLE_LOKAL + "("
				+ KEY_ID + " INTEGER PRIMARY KEY," + KEY_FILEPATH + " TEXT,"
				+ KEY_USER + " TEXT," + KEY_SYNCBOOL + " INTEGER" + ")";
		String CREATE_DRIVE_TABLE = "CREATE TABLE " + TABLE_DRIVE + "("
				+ KEY_ID + " INTEGER PRIMARY KEY," + KEY_FILEID + " TEXT,"
				+ KEY_USER + " TEXT," + KEY_SYNCBOOL + " INTEGER" + ")";
		db.execSQL(CREATE_LOKAL_TABLE);
		db.execSQL(CREATE_DRIVE_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		// Drop older table if existed
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOKAL);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_DRIVE);
		// Create tables again
		onCreate(db);
	}

	public void addLokal(CLocalFile lf) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		Cursor cursor = db.query(TABLE_LOKAL, new String[] { KEY_ID,
				KEY_FILEPATH, KEY_USER }, KEY_FILEPATH + "=?",
				new String[] { lf.getFilePath() }, null, null, null, null);
		if (cursor.getCount() == 0) {
			values.put(KEY_ID, lf.getID());
			values.put(KEY_FILEPATH, lf.getFilePath());
			values.put(KEY_USER, lf.getUser());
			values.put(KEY_SYNCBOOL, 1);
			db.insert(TABLE_LOKAL, null, values);
		}
	}
	
	public void addDrive(CDriveFile df) {//TAMBAH Integer revnumber
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		Cursor cursor = db.query(TABLE_DRIVE, new String[] { KEY_ID,
				KEY_FILEID, KEY_USER }, KEY_ID+ "=?" + " or "+ KEY_FILEID+ "=?",
				new String[] { String.valueOf(df.getID()),df.getFileId() }, null, null, null, null);
		if (cursor.getCount() == 0) {
			System.out.println("Masuk lo");
			values.put(KEY_ID, df.getID());
			values.put(KEY_FILEID, df.getFileId());
			values.put(KEY_USER, df.getUser());
			values.put(KEY_SYNCBOOL, 1);
			db.insert(TABLE_DRIVE, null, values);
		}
	}
	

	public CLocalFile getLokalFile(int Id, String username) {
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(TABLE_LOKAL, new String[] { KEY_ID,
				KEY_FILEPATH, KEY_USER }, KEY_ID + "=?" + " and " + KEY_USER
				+ "=?", new String[] { String.valueOf(Id), username }, null,
				null, null, null);
		if (cursor != null)
			cursor.moveToFirst();
		CLocalFile cl = new CLocalFile(cursor.getInt(0),
				cursor.getString(1), cursor.getString(2));
		return cl;
	}
	
	public CDriveFile getDriveFile(int Id, String username) {
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(TABLE_DRIVE, new String[] { KEY_ID,
				KEY_FILEID, KEY_USER }, KEY_ID + "=?" + " and " + KEY_USER
				+ "=?", new String[] { String.valueOf(Id), username }, null,
				null, null, null);
		if (cursor != null)
			cursor.moveToFirst();
		CDriveFile dl = new CDriveFile(Integer.parseInt(cursor
				.getString(0)), cursor.getString(1),cursor
						.getString(2));
		return dl;
	}
	//CEK APAKAH FILE TERSEBUT TELAH DICENTANG ATAU DIDOWNLOAD TAK PEDULI SYNC ATAU TIDAK
	public boolean isExist(String FilePath, String username) {
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(TABLE_LOKAL, new String[] { KEY_ID,
				KEY_FILEPATH, KEY_USER }, KEY_FILEPATH + "=?" + " and "
				+ KEY_USER + "=?", new String[] { FilePath, username }, null,
				null, null, null);
		if (cursor.getCount() > 0) {
			return true;
		} else
			return false;
	}
	
	public boolean IsDriveExist(String fileId, String username) {
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(TABLE_DRIVE, new String[] { KEY_ID,
				KEY_FILEID, KEY_USER }, KEY_FILEID + "=?" + " and "
				+ KEY_USER + "=?" , new String[] { fileId,username }, null,
				null, null, null);
		if (cursor.getCount() > 0) {
			return true;
		} else
			return false;
	}
	//CEK APAKAH SYNCSERVICE.JAVA HARUS SYNC FILE INI
	public boolean IsLokalSyncThis(int syncid,String username){
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(TABLE_LOKAL, new String[] { KEY_ID,
				KEY_FILEPATH, KEY_USER }, KEY_ID+ "=?" + " and "
				+ KEY_USER + "=?" + " and " + KEY_SYNCBOOL + "=?", new String[] { String.valueOf(syncid), username, String.valueOf(1) }, null,
				null, null, null);
		if (cursor.getCount() > 0) {
			return true;
		} else
			return false;
	}
	
	public boolean IsLokalHaveThis(int syncid,String username){
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(TABLE_LOKAL, new String[] { KEY_ID,
				KEY_FILEPATH, KEY_USER }, KEY_ID+ "=?" + " and "
				+ KEY_USER + "=?", new String[] { String.valueOf(syncid), username }, null,
				null, null, null);
		if (cursor.getCount() > 0) {
			return true;
		} else
			return false;
	}
	
	public boolean IsDriveSyncThis(int syncid,String username){
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(TABLE_DRIVE, new String[] { KEY_ID,
				KEY_FILEID, KEY_USER }, KEY_ID+ "=?" + " and "
				+ KEY_USER + "=?" + " and " + KEY_SYNCBOOL + "=?", new String[] { String.valueOf(syncid), username, String.valueOf(1) }, null,
				null, null, null);
		if (cursor.getCount() > 0) {
			return true;
		} else
			return false;
	}
	
	public boolean IsDriveHaveThis(int syncid,String username){
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(TABLE_DRIVE, new String[] { KEY_ID,
				KEY_FILEID, KEY_USER }, KEY_ID+ "=?" + " and "
				+ KEY_USER + "=?", new String[] { String.valueOf(syncid), username }, null,
				null, null, null);
		if (cursor.getCount() > 0) {
			return true;
		} else
			return false;
	}
	//END OF CEK APAKAH SYNCSERVICE.JAVA HARUS SYNC FILE INI
	//DISPLAY CHECKBOX ONLY FUNCTION
	public boolean isLokalSynced(String FilePath, String username) {
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(TABLE_LOKAL, new String[] { KEY_ID,
				KEY_FILEPATH, KEY_USER }, KEY_FILEPATH + "=?" + " and "
				+ KEY_USER + "=?" + " and " + KEY_SYNCBOOL + "=?", new String[] { FilePath, username, String.valueOf(1) }, null,
				null, null, null);
		if (cursor.getCount() > 0) {
			return true;
		} else
			return false;
	}
	
	public boolean IsFileDriveSynced(String fileid,String username){
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(TABLE_DRIVE, new String[] { KEY_ID,
				KEY_FILEID, KEY_USER }, KEY_FILEID+ "=?" + " and "
				+ KEY_USER + "=?" + " and " + KEY_SYNCBOOL + "=?", new String[] { fileid, username, String.valueOf(1) }, null,
				null, null, null);
		if (cursor.getCount() > 0) {
			return true;
		} else
			return false;
	}
//END OF DISPLAY CHECKBOX FUNCTION
	public List<CLocalFile> getAllLokal(String username) {
		List<CLocalFile> localFileList = new ArrayList<CLocalFile>();
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(TABLE_LOKAL, new String[] { KEY_ID,
				KEY_FILEPATH, KEY_USER }, KEY_USER + "=?" + " and " + KEY_SYNCBOOL + "=?",
				new String[] { username,String.valueOf(1) }, null, null, null, null);

		if (cursor.moveToFirst()) {
			do {
				CLocalFile cl = new CLocalFile(Integer.parseInt(cursor
						.getString(0)), cursor.getString(1),
						cursor.getString(2));
				// Adding contact to list
				localFileList.add(cl);
			} while (cursor.moveToNext());
		}
		return localFileList;
	}
	
	public List<CLocalFile> getAllLokal2(String username) {
		List<CLocalFile> localFileList = new ArrayList<CLocalFile>();
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(TABLE_LOKAL, new String[] { KEY_ID,
				KEY_FILEPATH, KEY_USER }, KEY_USER + "=?",
				new String[] { username }, null, null, null, null);

		if (cursor.moveToFirst()) {
			do {
				CLocalFile cl = new CLocalFile(Integer.parseInt(cursor
						.getString(0)), cursor.getString(1),
						cursor.getString(2));
				// Adding contact to list
				localFileList.add(cl);
			} while (cursor.moveToNext());
		}
		return localFileList;
	}
	
	public List<CDriveFile> getAllDrive(String username) {
		List<CDriveFile> driveFileList = new ArrayList<CDriveFile>();
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(TABLE_DRIVE, new String[] { KEY_ID,
				KEY_FILEID, KEY_USER }, KEY_USER + "=?" + " and " + KEY_SYNCBOOL + "=?",
				new String[] { username,String.valueOf(1) }, null, null, null, null);

		if (cursor.moveToFirst()) {
			do {
				CDriveFile cl = new CDriveFile(Integer.parseInt(cursor
						.getString(0)), cursor.getString(1),cursor
								.getString(2));
				driveFileList.add(cl);
			} while (cursor.moveToNext());
		}
		return driveFileList;
	}

	public int getLokalCount(String username) {
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(TABLE_LOKAL, new String[] { KEY_ID,
				KEY_FILEPATH, KEY_USER }, KEY_USER + "=?"+ " and " + KEY_SYNCBOOL + "=?",
				new String[] { username,String.valueOf(1) }, null, null, null, null);

		// return count
		return cursor.getCount();
	}
	public int getLokalCount2(String username) {
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(TABLE_LOKAL, new String[] { KEY_ID,
				KEY_FILEPATH, KEY_USER }, KEY_USER + "=?",
				new String[] { username }, null, null, null, null);

		// return count
		return cursor.getCount();
	}

	public int getDriveCount(String username) {
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(TABLE_DRIVE, new String[] { KEY_ID,
				KEY_FILEID, KEY_USER }, KEY_USER + "=?" + " and " + KEY_SYNCBOOL + "=?",
				new String[] { username,String.valueOf(1) }, null, null, null, null);

		// return count
		return cursor.getCount();
	}
	
	public int getDriveCount2(String username) {
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(TABLE_DRIVE, new String[] { KEY_ID,
				KEY_FILEID, KEY_USER }, KEY_USER + "=?",
				new String[] { username }, null, null, null, null);

		// return count
		return cursor.getCount();
	}
	
	public void SetSyncTrue(String onTable,String FileIdentifier,String username){
		SQLiteDatabase dbIn=this.getReadableDatabase();
		SQLiteDatabase dbOut=this.getWritableDatabase();
		if(onTable=="LOKAL"){
			Cursor cursor = dbIn.query(TABLE_LOKAL, new String[] { KEY_ID,
					KEY_FILEPATH, KEY_USER}, KEY_FILEPATH + "=?" + " and " + KEY_USER
					+ "=?", new String[] { FileIdentifier, username }, null,
					null, null, null);
			if (cursor != null)
				cursor.moveToFirst();
			ContentValues values = new ContentValues();
			values.put(KEY_SYNCBOOL, 1);
			dbOut.update(TABLE_LOKAL, values, KEY_FILEPATH + " = ?",
		            new String[] { String.valueOf(FileIdentifier) });
			dbIn.close();
			dbOut.close();
		}
		else if(onTable=="DRIVE"){
			Cursor cursor = dbIn.query(TABLE_DRIVE, new String[] { KEY_ID,
					KEY_FILEID, KEY_USER}, KEY_FILEID+ "=?" + " and " + KEY_USER
					+ "=?", new String[] { FileIdentifier, username }, null,
					null, null, null);
			if (cursor != null)
				cursor.moveToFirst();
			ContentValues values = new ContentValues();
			values.put(KEY_SYNCBOOL, 1);
			dbOut.update(TABLE_DRIVE, values, KEY_FILEID + " = ?",
		            new String[] { String.valueOf(FileIdentifier) });
			dbIn.close();
			dbOut.close();
		}
	}
	
	public void SetSyncFalse(String onTable,String FileIdentifier,String username){
		SQLiteDatabase dbIn=this.getReadableDatabase();
		SQLiteDatabase dbOut=this.getWritableDatabase();
		if(onTable=="LOKAL"){
			Cursor cursor = dbIn.query(TABLE_LOKAL, new String[] { KEY_ID,
					KEY_FILEPATH, KEY_USER}, KEY_FILEPATH + "=?" + " and " + KEY_USER
					+ "=?", new String[] { FileIdentifier, username }, null,
					null, null, null);
			if (cursor != null)
				cursor.moveToFirst();
			ContentValues values = new ContentValues();
			values.put(KEY_SYNCBOOL, 0);
			dbOut.update(TABLE_LOKAL, values, KEY_FILEPATH + " = ?",
		            new String[] { String.valueOf(FileIdentifier) });
			dbIn.close();
			dbOut.close();
		}
		else if(onTable=="DRIVE"){
			Cursor cursor = dbIn.query(TABLE_DRIVE, new String[] { KEY_ID,
					KEY_FILEID, KEY_USER}, KEY_FILEID+ "=?" + " and " + KEY_USER
					+ "=?", new String[] { FileIdentifier, username }, null,
					null, null, null);
			if (cursor != null)
				cursor.moveToFirst();
			ContentValues values = new ContentValues();
			values.put(KEY_SYNCBOOL, 0);
			dbOut.update(TABLE_DRIVE, values, KEY_FILEID + " = ?",
		            new String[] { String.valueOf(FileIdentifier) });
			dbIn.close();
			dbOut.close();
		}
	}
	
	public void removeLokal(String filepath, String username) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_LOKAL, KEY_FILEPATH + "=?" + " and " + KEY_USER + "=?",
				new String[] { filepath, username });
	}
	
	public void removeDrive(String fileid, String username) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_DRIVE, KEY_FILEID + "=?" + " and " + KEY_USER + "=?",
				new String[] { fileid, username });
	}
	
	public void emptyDrive(){
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_DRIVE, null,null);
		db.delete(TABLE_LOKAL, null, null);
	}
	
	public void onDestroy(SQLiteDatabase db) {
		db.close();
	}
}

