package uk.iankent.ikettle2;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.IOException;

import uk.iankent.ikettle2.client.IKettle2Client;
import uk.iankent.ikettle2.client.KettleCommandAckResponse;
import uk.iankent.ikettle2.client.KettleStatusResponse;
import uk.iankent.ikettle2.client.OnKettleResponse;
import uk.iankent.ikettle2.data.Kettle;

public class UseIKettle2 extends AppCompatActivity {

    Kettle kettle;
    IKettle2Client client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final Activity activity = this;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_use_ikettle2);

        Intent i = getIntent();
        kettle = (Kettle) i.getParcelableExtra("kettle");
        client = new IKettle2Client(kettle.Host, kettle.Port);

        final TextView txtHost = (TextView)findViewById(R.id.txtHost);
        final TextView txtError = (TextView)findViewById(R.id.txtError);

        final TextView txtTemp = (TextView)findViewById(R.id.txtTemp);
        final TextView txtStatus = (TextView)findViewById(R.id.txtStatus);

        final TextView txtTargetTemp = (TextView)findViewById(R.id.txtTargetTemp);
        final ToggleButton btnStartStop = (ToggleButton)findViewById(R.id.btnStartStop);

        btnStartStop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                btnStartStop.setEnabled(false);

                if (isChecked) {
                    int temp = Integer.parseInt(txtTargetTemp.getText().toString(), 10);
                    client.Start(temp);
                } else {
                    client.Stop();
                }
            }
        });

        txtHost.setText(kettle.Host);

        new Thread(new Runnable() {
            public void run() {
                try{
                    client.Connect();
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
                                    txtTemp.setText(((Integer)response.getTemperature()).toString() + (char)0x00B0);
                                    txtStatus.setText(response.getStatus().name());
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
                                }
                            });
                        }
                    };
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtError.setVisibility(View.GONE);
                        }
                    });
                    client.Process();
                } catch (IOException e) {
                    final IOException ex = e;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtError.setText(ex.getMessage());
                            txtError.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        }).start();
    }
}
