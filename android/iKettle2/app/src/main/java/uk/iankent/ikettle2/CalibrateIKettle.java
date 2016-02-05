package uk.iankent.ikettle2;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import uk.iankent.ikettle2.client.IKettle2Client;
import uk.iankent.ikettle2.client.KettleAutoDacResponse;
import uk.iankent.ikettle2.client.KettleCommandAckResponse;
import uk.iankent.ikettle2.client.KettleError;
import uk.iankent.ikettle2.client.KettleStatusResponse;
import uk.iankent.ikettle2.client.OnKettleResponse;
import uk.iankent.ikettle2.data.Kettle;
import uk.iankent.ikettle2.data.Kettles;
import uk.iankent.ikettle2.services.UseKettle2Service;

public class CalibrateIKettle extends AppCompatActivity implements IKettle2Client.KettleListener {

    //public static IKettle2Client client;
    private Kettle kettle;
    UseKettle2Service.LocalBinder mBinder = null;
    Button btnContinue;
    TextView txtError;

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            mBinder = (UseKettle2Service.LocalBinder) service;
            mBinder.setKettleListener(CalibrateIKettle.this);
            //client = binder.getClient();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBinder = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibrate_ikettle);

        kettle = getIntent().getParcelableExtra("kettle");

        txtError = (TextView)findViewById(R.id.txtError);
        btnContinue = (Button)findViewById(R.id.btnContinue);

        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mBinder==null||kettle==null)return; // shouldn't happen

                btnContinue.setEnabled(false);
                txtError.setVisibility(View.INVISIBLE);

                Intent startIntent = new Intent(CalibrateIKettle.this.getApplicationContext(), UseKettle2Service.class);
                startIntent.setAction(Constants.START_CALIBRATE);
                startService(startIntent);
            }
        });
    }

    protected void onStart() {
        super.onStart();

        Intent startIntent = new Intent(CalibrateIKettle.this.getApplicationContext(), UseKettle2Service.class);
        startIntent.setAction(Constants.START_ACTION);
        startIntent.putExtra("kettle", kettle);
        bindService(startIntent, mConnection, Context.BIND_AUTO_CREATE);
        startService(startIntent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBinder != null) {
            mBinder.removeKettleListener(CalibrateIKettle.this);
            unbindService(mConnection);
            mBinder = null;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mBinder != null) {
            mBinder.removeKettleListener(CalibrateIKettle.this);
            unbindService(mConnection);
            mBinder = null;
        }
    }

    @Override
    public void onError(final KettleError error) {
        Log.d("CalibrateIKettle", "onKettleResponse: error");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnContinue.setEnabled(true);
                txtError.setText(error.getException().getMessage());
                txtError.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onConnected() {

    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onKettleStatus(final KettleStatusResponse response) {

    }

    @Override
    public void onKettleCommandAck(KettleCommandAckResponse response) {

    }

    @Override
    public void onKettleAutoDacResponse(KettleAutoDacResponse response) {
        Log.d("CalibrateIKettle", "onKettleResponse: autoDac");
        kettle.OffBaseWeight = response.getOffBaseWeight();
        Kettles.Save(CalibrateIKettle.this);
        finish();
    }

}
