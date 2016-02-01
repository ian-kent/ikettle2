package uk.iankent.ikettle2;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.triggertrap.seekarc.SeekArc;

import uk.iankent.ikettle2.client.IKettle2Client;
import uk.iankent.ikettle2.client.KettleAutoDacResponse;
import uk.iankent.ikettle2.client.KettleCommandAckResponse;
import uk.iankent.ikettle2.client.KettleError;
import uk.iankent.ikettle2.client.KettleStatusResponse;
import uk.iankent.ikettle2.data.Kettle;
import uk.iankent.ikettle2.services.UseKettle2Service;

public class UseIKettle2 extends AppCompatActivity implements IKettle2Client.KettleListener {

    Kettle kettle;
    KettleStatusResponse lastStatus = null;
    UseKettle2Service.LocalBinder mBinder = null;

    protected int targetTemp = 0;
    protected ToggleButton btnStartStop;
    protected SeekArc mSeekArc;

    private TextView txtTemp;
    private TextView txtError;
    private TextView txtStatus;
    private TextView txtWaterlevel;
    private TextView txtTargetTemp;

    protected boolean isBusy = false;
    protected CompoundButton.OnCheckedChangeListener btnStartStopCheckedChangeListener;

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            mBinder = (UseKettle2Service.LocalBinder) service;
            mBinder.setKettleListener(UseIKettle2.this);
            if(mBinder.getState() == UseKettle2Service.ServiceState.CONNECTED) {
                txtError.setVisibility(View.GONE);
                btnStartStop.setEnabled(true);
                txtTargetTemp.setEnabled(true);
                mSeekArc.setEnabled(true);
                mSeekArc.setProgress(Integer.valueOf(txtTargetTemp.getText().toString()));
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBinder = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_use_ikettle2);

        // Get all the ui elements we need
        final TextView txtName = (TextView)findViewById(R.id.txtName);
        final Button btnCalibrate = (Button)findViewById(R.id.btnCalibrate);
        txtError = (TextView)findViewById(R.id.txtError);
        txtTemp = (TextView)findViewById(R.id.txtTemp);
        txtStatus = (TextView)findViewById(R.id.txtStatus);
        txtWaterlevel = (TextView)findViewById(R.id.txtWaterlevel);
        txtTargetTemp = (TextView)findViewById(R.id.txtTargetTemp);
        btnStartStop = (ToggleButton)findViewById(R.id.btnStartStop);
        mSeekArc = (SeekArc)findViewById(R.id.seekArc);

        kettle = getIntent().getParcelableExtra("kettle");

        txtName.setText(kettle.Name);
        txtError.setVisibility(View.GONE);

        btnCalibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(UseIKettle2.this, CalibrateIKettle.class);
                i.putExtra("kettle", kettle);
                startActivity(i);
            }
        });

        mSeekArc.setOnSeekArcChangeListener(new SeekArc.OnSeekArcChangeListener() {
            @Override
            public void onProgressChanged(SeekArc seekArc, int i, boolean b) {
                targetTemp = i;
                txtTargetTemp.setText(Integer.toString(i));
                updateUIStatus();
            }

            @Override
            public void onStartTrackingTouch(SeekArc seekArc) {

            }

            @Override
            public void onStopTrackingTouch(SeekArc seekArc) {

            }
        });
        mSeekArc.setProgress(Integer.valueOf(txtTargetTemp.getText().toString()));

        txtTargetTemp.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                int i = 0;
                try {
                    i = Integer.valueOf(s.toString());
                } catch (RuntimeException e) {} // do nothing

                if(i == 0 && s.length() > 1) {
                    s.clear();
                    s.append("0");
                } else if(i < 0) {
                    s.clear();
                    s.append("0");
                } else if (i > 100) {
                    s.clear();
                    s.append("100");
                }
            }
        });

        btnStartStopCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                btnStartStop.setEnabled(false);

                if(isChecked) {
                    int temp = Integer.parseInt(txtTargetTemp.getText().toString(), 10);
                    isBusy = true;

                    Intent startIntent = new Intent(UseIKettle2.this.getApplicationContext(), UseKettle2Service.class);
                    startIntent.setAction(Constants.START_BOILING_ACTION);
                    startIntent.putExtra("temp", temp);
                    startService(startIntent);
                } else {
                    isBusy = true;
                    Intent startIntent = new Intent(UseIKettle2.this.getApplicationContext(), UseKettle2Service.class);
                    startIntent.setAction(Constants.STOP_BOILING_ACTION);
                    startService(startIntent);
                }

                updateUIStatus();
            }
        };
        btnStartStop.setOnCheckedChangeListener(btnStartStopCheckedChangeListener);
    }

    protected void updateUIStatus() {
        if(lastStatus != null) {
            if(targetTemp <= lastStatus.getTemperature()) {
                btnStartStop.setEnabled(false);
            } else {
                btnStartStop.setEnabled(true);
            }

            btnStartStop.setOnCheckedChangeListener(null);
            if(lastStatus.getStatus() == KettleStatusResponse.State.Boiling) {
                btnStartStop.setChecked(true);
            } else {
                btnStartStop.setChecked(false);
            }
            btnStartStop.setOnCheckedChangeListener(btnStartStopCheckedChangeListener);

            if(isBusy || lastStatus.getStatus() == KettleStatusResponse.State.Boiling) {
                mSeekArc.setEnabled(false);
            } else {
                mSeekArc.setEnabled(true);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if(mBinder != null) {
            mBinder.removeKettleListener(UseIKettle2.this);
            unbindService(mConnection);
            lastStatus = null;
            mBinder = null;
        }

        Intent startIntent = new Intent(UseIKettle2.this.getApplicationContext(), UseKettle2Service.class);
        startIntent.setAction(Constants.STOP_ACTION);
        startService(startIntent);

        super.onBackPressed();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("useikettle2", "onStart");

        Intent startIntent = new Intent(UseIKettle2.this.getApplicationContext(), UseKettle2Service.class);
        startIntent.setAction(Constants.START_ACTION);
        startIntent.putExtra("kettle", kettle);
        bindService(startIntent, mConnection, Context.BIND_AUTO_CREATE);
        startService(startIntent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("useikettle2", "onStop");
        // Unbind from the service
        if(mBinder != null) {
            mBinder.removeKettleListener(UseIKettle2.this);
            unbindService(mConnection);
            lastStatus = null;
            mBinder = null;
        }
    }

    @Override
    public void onError(final KettleError error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtError.setText(error.getException().getMessage());
                txtError.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("UseIKettle2", "onConnected");
                txtError.setVisibility(View.GONE);
                btnStartStop.setEnabled(true);
                txtTargetTemp.setEnabled(true);
                mSeekArc.setEnabled(true);
                mSeekArc.setProgress(Integer.valueOf(txtTargetTemp.getText().toString()));

            }
        });
    }

    @Override
    public void onDisconnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtError.setVisibility(View.VISIBLE);
                txtError.setText("Disconnected");
                txtStatus.setText("Disconnected");
                btnStartStop.setEnabled(false);
                mSeekArc.setEnabled(false);
                txtTargetTemp.setEnabled(false);
                // TODO retry
            }
        });
    }

    @Override
    public void onKettleStatus(final KettleStatusResponse response) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch(response.getStatus()) {
                    case Ready:
                        if(btnStartStop.isChecked()) {
                            btnStartStop.setEnabled(true);
                            btnStartStop.setOnCheckedChangeListener(null);
                            btnStartStop.setChecked(false);
                            btnStartStop.setOnCheckedChangeListener(btnStartStopCheckedChangeListener);
                        }
                        break;
                }
                if(response.getTemperature() == 127) {
                    txtStatus.setText("Kettle off base");
                    txtTemp.setText("?");
                    txtWaterlevel.setText("Unknown");
                } else {
                    int t = response.getTemperature();

                    // make 40 degrees the cold setting
                    int c;
                    if(t < 40) {
                        c = 0;
                    } else {
                        c = t - (t/100*40);
                    }

                    // FIXME mid-range colour (~70deg) is a horrible purple
                    txtTemp.setText(((Integer)t).toString());// + (char) 0x00B0);
                    int r = Color.rgb((255*c)/100, 0, (255*(100-c))/100);
                    txtTemp.setTextColor(r);

                    txtStatus.setText(response.getStatus().name());
                    txtWaterlevel.setText(response.getWaterLevelStatus().name());
                }
                lastStatus = response;
                updateUIStatus();
            }
        });
    }

    @Override
    public void onKettleCommandAck(KettleCommandAckResponse response) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnStartStop.setEnabled(true);
                isBusy = false;
            }
        });
    }

    @Override
    public void onKettleAutoDacResponse(KettleAutoDacResponse response) {

    }
}
