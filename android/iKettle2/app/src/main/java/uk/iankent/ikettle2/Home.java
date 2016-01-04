package uk.iankent.ikettle2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.net.InetAddress;
import java.net.UnknownHostException;

import uk.iankent.ikettle2.data.Kettle;
import uk.iankent.ikettle2.data.KettleAdapter;
import uk.iankent.ikettle2.data.Kettles;

public class Home extends AppCompatActivity {

    KettleAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Kettles.Load(this);
        adapter = new KettleAdapter(this, Kettles.Get());

        final Context ctx = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(ctx, ChooseNewExistingKettle.class);
                startActivityForResult(i, 1);
            }
        });

        ListView lv = (ListView)findViewById(R.id.listViewDevices);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Kettle chosen = adapter.getItem(position);
                Intent i = new Intent(ctx, UseIKettle2.class);
                i.putExtra("kettle", chosen);
                startActivity(i);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            adapter.notifyDataSetChanged();
            Kettles.Save(this);
        }
    }
}
