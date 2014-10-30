package kevin.thesis.drivenote;

import java.io.File;

import com.google.api.client.util.DateTime;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.MimeTypeMap;
import android.widget.TextView;
import android.support.v4.app.NavUtils;

public class FileProperty extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_property);
        Bundle b=getIntent().getExtras();
        File file=new File(b.getString("file"));
        TextView temp=(TextView) findViewById(R.id.nama);
        temp.append(file.getAbsolutePath().substring(
        		file.getAbsolutePath().lastIndexOf("/")+1, 
        		file.getAbsolutePath().length()));
        temp=(TextView) findViewById(R.id.path);
        temp.append(file.getAbsolutePath());
        temp=(TextView) findViewById(R.id.lastaccess);
        String d=new DateTime(file.lastModified()).toString();
        d=d.replace("T", " ");
        d=d.substring(0, d.length()-4);
        temp.append(d);
        temp=(TextView) findViewById(R.id.filetype);
        Uri selectedUri = Uri.fromFile(file);
		String fileExtension 
		= MimeTypeMap.getFileExtensionFromUrl(selectedUri.toString());
		temp.append(fileExtension);
		temp=(TextView) findViewById(R.id.size);
		float size=file.length()/1024;
    	if(file.length()<1024&size!=0){
    		temp.append(String.format("%d",file.length())+" B");
    	}
    	else if(size>=1024){
    		temp.append(String.format("%.0f", size/1024)+" MB");}
    	else if(size<1024)
    		{temp.append(String.format("%.2f",size)+" KB");}
    	else{
    		temp.append("-");
    	}
		//bikinen kali folder isa nampilno ada berapa file dan folder dalam'e ambe total keseluruhan size
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_file_property, menu);
        return true;
    }

    
}
