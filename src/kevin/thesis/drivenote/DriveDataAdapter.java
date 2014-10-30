package kevin.thesis.drivenote;


import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
 
@SuppressLint("ParserError")
public class DriveDataAdapter extends ArrayAdapter<com.google.api.services.drive.model.File> {
	private final Context context;
	private final List<com.google.api.services.drive.model.File> values;
	private DatabaseHandler db;
	public DriveDataAdapter(Context context, List<com.google.api.services.drive.model.File> fileList) {
		super(context, R.layout.row, fileList);
		db=new DatabaseHandler(getContext());
		this.context = context;
		this.values = fileList;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
			.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
 
		View rowView = inflater.inflate(R.layout.row, parent, false);
		TextView textView = (TextView) rowView.findViewById(R.id.item_name);
		ImageView imageView = (ImageView) rowView.findViewById(R.id.item_icon);
		CheckBox selBox=(CheckBox) rowView.findViewById(R.id.item_check);
		TextView tgl=(TextView) rowView.findViewById(R.id.item_date);
		if(values.get(position).getTitle()!="Up"){
			textView.setText(values.get(position).getTitle());
			}
		else{
			textView.setText(values.get(position).getTitle());
		}
		
		selBox.setTag(values.get(position).getId());
		// Change icon based on name
		SharedPreferences settings = context.getSharedPreferences("MyPrefs", 0);
        String accountName = settings.getString("accountName", null); 
		if(db.IsFileDriveSynced(values.get(position).getId(), accountName)==true)
		{selBox.setChecked(true);}
        String fileType;
        fileType=values.get(position).getFileExtension();
    	//System.out.println(fileType);
    	imageView.setImageResource(R.drawable.file_icon);
		tgl.setText(values.get(position).getModifiedDate().toString());
		tgl.setVisibility(View.GONE);
        //File file=new File(s);
		String MimeType=values.get(position).getMimeType();
		if(MimeType.equalsIgnoreCase("application/vnd.google-apps.folder")){
        	imageView.setImageResource(R.drawable.directory_icon);
        }/*else if(s=="Up"&&!file.isDirectory()&&!file.isFile()){
        	imageView.setImageResource(R.drawable.directory_up);
        	selBox.setVisibility(View.GONE);
        	tgl.setVisibility(View.GONE);
        	selBox.setEnabled(false);//Up ga mungkin dipilih
        }*/
        else if(fileType!=null){
        	if(fileType.equalsIgnoreCase("xlsx")||fileType.equalsIgnoreCase("xls"))
            	imageView.setImageResource(R.drawable.excel);
            else if(fileType.equalsIgnoreCase("mp3"))
                	imageView.setImageResource(R.drawable.music);
            else if(fileType.equalsIgnoreCase("mp4")||fileType.equalsIgnoreCase("avi")||fileType.equalsIgnoreCase("mkv"))
                	imageView.setImageResource(R.drawable.movies);
            else if(fileType.equalsIgnoreCase("pdf"))
                	imageView.setImageResource(R.drawable.pdf);
            else if(fileType.equalsIgnoreCase("ppt")||fileType.equalsIgnoreCase("pptx"))
                	imageView.setImageResource(R.drawable.ppt);
            else if(fileType.equalsIgnoreCase("doc")||fileType.equalsIgnoreCase("docx"))
                	imageView.setImageResource(R.drawable.word);
            else if(fileType.equalsIgnoreCase("jpg")||fileType.equalsIgnoreCase("jpeg"))
                	imageView.setImageResource(R.drawable.image);
            else if(fileType.equalsIgnoreCase("rar")||fileType.equalsIgnoreCase("zip"))
                	imageView.setImageResource(R.drawable.zip);
            else if(fileType.equalsIgnoreCase("txt"))
                	imageView.setImageResource(R.drawable.text);
            else if(fileType.equalsIgnoreCase("xml"))
                	imageView.setImageResource(R.drawable.xml32);
            else if(fileType.equalsIgnoreCase("apk"))
            	imageView.setImageResource(R.drawable.apk);
            }
        else if(fileType==null){
        	String mimetype=values.get(position).getMimeType();
        	if(mimetype.equalsIgnoreCase("application/vnd.google-apps.folder")){
        		imageView.setImageResource(R.drawable.directory_icon);
        	}
        	else if(mimetype.equalsIgnoreCase("application/vnd.google-apps.document")){
        		imageView.setImageResource(R.drawable.gdoc);
        	}
        	else if(mimetype.equalsIgnoreCase("application/vnd.google-apps.spreadsheet")){
        		imageView.setImageResource(R.drawable.gxls);
        	}
        	else if(mimetype.equalsIgnoreCase("application/vnd.google-apps.presentation")){
        		imageView.setImageResource(R.drawable.gppt);
        	}
        }
		return rowView;
	}


	
}
