package uk.iankent.ikettle2.data;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import uk.iankent.ikettle2.R;

/**
 * Created by iankent on 27/12/2015.
 */
public class KettleAdapter extends ArrayAdapter<Kettle> {
    public KettleAdapter(Context context, ArrayList<Kettle> users) {
        super(context, 0, users);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Kettle device = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.device_list_view, parent, false);
        }
        // Lookup view for data population
        TextView tvName = (TextView) convertView.findViewById(R.id.txtName);
        TextView tvHost = (TextView) convertView.findViewById(R.id.txtHost);
        //TextView tvHome = (TextView) convertView.findViewById(R.id.txtPort);
        // Populate the data into the template view using the data object
        tvName.setText(device.Name);
        tvHost.setText(device.Host);
        //tvHome.setText(device.Port.toString());
        // Return the completed view to render on screen
        return convertView;
    }
}
