package uk.iankent.ikettle2;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.triggertrap.seekarc.SeekArc;

import uk.iankent.ikettle2.client.IKettle2Client;
import uk.iankent.ikettle2.client.KettleCommandAckResponse;
import uk.iankent.ikettle2.client.KettleError;
import uk.iankent.ikettle2.client.KettleResponse;
import uk.iankent.ikettle2.client.KettleStatusResponse;
import uk.iankent.ikettle2.client.OnKettleResponse;
import uk.iankent.ikettle2.data.Kettle;

public class UseIKettle2 extends AppCompatActivity {

    Kettle kettle;
    IKettle2Client client;

    protected int targetTemp = 0;
    protected ToggleButton btnStartStop;
    protected SeekArc mSeekArc;

    protected boolean isBusy = false;
    protected CompoundButton.OnCheckedChangeListener btnStartStopCheckedChangeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final Activity activity = this;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_use_ikettle2);

        Intent i = getIntent();
        kettle = (Kettle) i.getParcelableExtra("kettle");
        client = new IKettle2Client(kettle.Host, kettle.Port);

        final TextView txtName = (TextView)findViewById(R.id.txtName);
        final TextView txtError = (TextView)findViewById(R.id.txtError);

        final TextView txtTemp = (TextView)findViewById(R.id.txtTemp);
        final TextView txtStatus = (TextView)findViewById(R.id.txtStatus);
        final TextView txtWaterlevel = (TextView)findViewById(R.id.txtWaterlevel);

        final TextView txtTargetTemp = (TextView)findViewById(R.id.txtTargetTemp);
        btnStartStop = (ToggleButton)findViewById(R.id.btnStartStop);

        mSeekArc = (SeekArc)findViewById(R.id.seekArc);
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

        btnStartStopCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                btnStartStop.setEnabled(false);

                if (isChecked) {
                    int temp = Integer.parseInt(txtTargetTemp.getText().toString(), 10);
                    isBusy = true;
                    client.Start(temp);
                } else {
                    isBusy = true;
                    client.Stop();
                }

                updateUIStatus();
            }
        };
        btnStartStop.setOnCheckedChangeListener(btnStartStopCheckedChangeListener);

        client.onError = new OnKettleResponse<KettleError>() {
            @Override
            public void onKettleResponse(final KettleError response) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txtError.setText(response.getException().getMessage());
                        txtError.setVisibility(View.VISIBLE);
                    }
                });
            }
        };
        client.onKettleStatus = new OnKettleResponse<KettleStatusResponse>() {
            @Override
            public void onKettleResponse(final KettleStatusResponse response) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switch(response.getStatus()) {
                            case Ready:
                                if(btnStartStop.isChecked()) {
                                    btnStartStop.setEnabled(true);
                                    btnStartStop.setChecked(false);
                                }
                                break;
                        }
                        if(response.getTemperature() == 127) {
                            txtTemp.setText("Kettle off base");
                        } else {
                            int t = response.getTemperature();

                            // make 40 degrees the cold setting
                            int c;
                            if(t < 40) {
                                c = 0;
                            } else {
                                c = t - (t/100*40);
                            }

                            txtTemp.setText(((Integer)t).toString() + (char) 0x00B0);
                            int r = Color.rgb((255*c)/100, 0, (255*(100-c))/100);
                            txtTemp.setTextColor(r);
                        }
                        txtStatus.setText(response.getStatus().name());
                        txtWaterlevel.setText(response.getWaterLevelStatus().name());

                        updateUIStatus();
                    }
                });
            }
        };
        client.onKettleCommandAck = new OnKettleResponse<KettleCommandAckResponse>() {
            @Override
            public void onKettleResponse(KettleCommandAckResponse response) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnStartStop.setEnabled(true);
                        isBusy = false;
                    }
                });
            }
        };

        txtName.setText(kettle.Name);
        txtError.setVisibility(View.GONE);
        client.Connect();
        client.onConnected = new OnKettleResponse() {
            @Override
            public void onKettleResponse(KettleResponse response) {
                client.Process();
            }
        };
        client.onDisconnected = new OnKettleResponse() {
            @Override
            public void onKettleResponse(KettleResponse response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txtError.setVisibility(View.VISIBLE);
                        txtError.setText("Disconnected");
                        // TODO retry
                    }
                });
            }
        };
    }

    protected void updateUIStatus() {
        KettleStatusResponse s = client.getLastStatus();
        if(targetTemp <= s.getTemperature()) {
            btnStartStop.setEnabled(false);
        } else {
            btnStartStop.setEnabled(true);
        }

        btnStartStop.setOnCheckedChangeListener(null);
        if (s.getStatus() == KettleStatusResponse.State.Boiling) {
            btnStartStop.setChecked(true);
        } else {
            btnStartStop.setChecked(false);
        }
        btnStartStop.setOnCheckedChangeListener(btnStartStopCheckedChangeListener);

        if(isBusy || s.getStatus() == KettleStatusResponse.State.Boiling) {
            mSeekArc.setEnabled(false);
        } else {
            mSeekArc.setEnabled(true);
        }
    }
}
