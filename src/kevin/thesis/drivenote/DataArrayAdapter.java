package kevin.thesis.drivenote;


import java.io.File;
import java.util.List;
import kevin.thesis.drivenote.R;
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
public class DataArrayAdapter extends ArrayAdapter<String> {
	private final Context context;
	private final List<String> values;
	private int LOKALMODE=0;
	 private int SYNCMODE=1;
	 private int MYMODE;
	 private DatabaseHandler db;
	public DataArrayAdapter(Context context, List<String> fileList,int MODE) {
		super(context, R.layout.row, fileList);
		db=new DatabaseHandler(getContext());
		MYMODE=MODE;
		this.context = context;
		this.values = fileList;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
			.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		//System.out.println(kevin.thesis.drivenote.LocalList.getstatus());
		View rowView = inflater.inflate(R.layout.row ,parent, false);
		TextView textView = (TextView) rowView.findViewById(R.id.item_name);
		ImageView imageView = (ImageView) rowView.findViewById(R.id.item_icon);
		CheckBox selBox=(CheckBox) rowView.findViewById(R.id.item_check);
		if(values.get(position)!="Up"){
			textView.setText(values.get(position).substring(values.get(position).lastIndexOf("/")+1, values.get(position).length()));
		}
		else{
			textView.setText(values.get(position));
		}
		SharedPreferences settings = context.getSharedPreferences("MyPrefs", 0);
        String accountName = settings.getString("accountName", null); 
		if(db.isLokalSynced(values.get(position),accountName)==true)
		{if(kevin.thesis.drivenote.LocalList.getstatus()==SYNCMODE||MYMODE==SYNCMODE)
		{selBox.setChecked(true);}}
		if(kevin.thesis.drivenote.LocalList.isContextAvailable(values.get(position))){
			selBox.setChecked(true);
		}
		selBox.setTag(values.get(position));
		// Change icon based on name
		String s = values.get(position);
        String fileType;
        fileType=s.substring(s.lastIndexOf(".")+1,s.length()).toLowerCase();
    	System.out.println(fileType);
    	imageView.setImageResource(R.drawable.file_icon);
		//System.out.println(s);
		TextView tgl=(TextView) rowView.findViewById(R.id.item_date);
		tgl.setText("");
		//tgl.setText(new SimpleDateFormat("yyyy-MM-dd  hh:mm:ss").format((new File(s).lastModified())));
        File file=new File(s);
        if(file.isDirectory()){
        	imageView.setImageResource(R.drawable.directory_icon);
        }else if(s=="Up"&&!file.isDirectory()&&!file.isFile()){
        	imageView.setImageResource(R.drawable.directory_up);
        	selBox.setVisibility(View.GONE);
        	tgl.setVisibility(View.GONE);
        	selBox.setEnabled(false);//Up ga mungkin dipilih
        }
        else {
        	float size=file.length()/1024;
        	if(file.length()<1024){
        		tgl.setText(String.format("%d",file.length())+" B");
        	}
        	else if(size>=1024){
        		tgl.setText(String.format("%.0f", size/1024)+" MB");}
        	else if(size<1024)
        		{tgl.setText(String.format("%.2f",size)+" KB");}
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
		return rowView;
	}
	
}
