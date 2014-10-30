package kevin.thesis.drivenote;


import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.google.api.services.drive.model.Permission;
 
@SuppressLint("ParserError")
public class PermissionAdapter extends ArrayAdapter<Permission> {
	private final Context context;
	private final List<Permission> values;

	public PermissionAdapter(Context context, List<Permission> plist) {
		super(context, R.layout.row, plist);
		this.context = context;
		this.values = plist;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
			.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		//System.out.println(kevin.thesis.drivenote.LocalList.getstatus());
		View rowView = inflater.inflate(R.layout.rowpermission, parent, false);
		TextView nameview=(TextView) rowView.findViewById(R.id.nameText);
		TextView roleview = (Button) rowView.findViewById(R.id.roleText);
		nameview.setText(values.get(position).getName());
		if(values.get(position).getRole().equalsIgnoreCase("owner")){
			roleview.setClickable(false);
		}
		roleview.setText(values.get(position).getRole());
		roleview.setTag(values.get(position).getId());
		return rowView;
	}
	
}
