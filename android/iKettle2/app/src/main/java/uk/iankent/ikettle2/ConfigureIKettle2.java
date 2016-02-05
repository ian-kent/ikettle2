package uk.iankent.ikettle2;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import uk.iankent.ikettle2.client.IKettle2Client;
import uk.iankent.ikettle2.client.KettleAutoDacResponse;
import uk.iankent.ikettle2.client.KettleCommandAckResponse;
import uk.iankent.ikettle2.client.KettleError;
import uk.iankent.ikettle2.client.KettleResponse;
import uk.iankent.ikettle2.client.KettleStatusResponse;
import uk.iankent.ikettle2.client.KettleWifiNetworksResponse;
import uk.iankent.ikettle2.client.OnKettleResponse;

public class ConfigureIKettle2 extends AppCompatActivity {

    public String wifiSSID;
    public String wifiPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_ikettle2);

        final ConfigureIKettle2 activity = this;
        final Button btnNext = (Button)findViewById(R.id.btnNext);
        final TextView txtError = (TextView)findViewById(R.id.txtError);
        final TextView txtInstructions = (TextView)findViewById(R.id.txtInstructions);
        final ListView listWifiNetworks = (ListView)findViewById(R.id.listWifiNetworks);
        final ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBar);
        final EditText password = (EditText)findViewById(R.id.password);

        final IKettle2Client kettleClient = new IKettle2Client("192.168.4.1");

        final Timer t = new Timer();

        final TimerTask setWifiSSID = new TimerTask() {
            @Override
            public void run() {
                kettleClient.SetWifiSSID(activity.wifiSSID);
            }
        };
        final TimerTask setWifiPassword = new TimerTask() {
            @Override
            public void run() {
                kettleClient.SetWifiPassword(activity.wifiPassword);
            }
        };
        final TimerTask saveSettings = new TimerTask() {
            @Override
            public void run() {
                kettleClient.SaveWifiSettings();
            }
        };

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnNext.setEnabled(false);
                progressBar.setVisibility(View.VISIBLE);
                txtError.setVisibility(View.GONE);
                txtInstructions.setText("Connecting to iKettle, please wait...");
                kettleClient.Connect();
            }
        });

        kettleClient.setKettleListener(new IKettle2Client.KettleListener() {
            @Override
            public void onError(final KettleError error) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnNext.setText("Try again");
                        btnNext.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                        txtError.setVisibility(View.VISIBLE);
                        if (error.getException() != null) {
                            txtError.setText(error.getException().getMessage());
                            error.getException().printStackTrace();
                        } else {
                            txtError.setText("Unknown error");
                        }
                    }
                });
            }

            @Override
            public void onConnected() {
                kettleClient.Process();
                kettleClient.GetWifiConnections();
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txtInstructions.setText("iKettle is scanning for wifi networks, please wait...");
                        Log.d("ConfigureIKettle", "calling GetWifiConnections");
                    }
                });
            }

            @Override
            public void onDisconnected() {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txtInstructions.setText("Kettle wifi configuration complete!");
                        btnNext.setText("Finish");
                        btnNext.setEnabled(true);
                        btnNext.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                activity.finish();
                            }
                        });
                    }
                });
            }

            @Override
            public void onKettleStatus(KettleStatusResponse response) {

            }

            @Override
            public void onKettleCommandAck(KettleCommandAckResponse response) {

            }

            @Override
            public void onKettleAutoDacResponse(KettleAutoDacResponse response) {

            }
        });

        kettleClient.onKettleWifiNetworksResponse = new OnKettleResponse<KettleWifiNetworksResponse>() {
            @Override
            public void onKettleResponse(final KettleWifiNetworksResponse response) {
                Log.d("ConfigureIKettle", "onKettleResponse: hi");
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ArrayList<String> list = response.getSSIDs();
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity, android.R.layout.simple_list_item_1,list);
                        listWifiNetworks.setAdapter(adapter);
                        listWifiNetworks.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                        txtInstructions.setText("Select a wifi network");
                    }
                });
            }
        };

        listWifiNetworks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                btnNext.setEnabled(true);
                activity.wifiSSID = parent.getAdapter().getItem(position).toString();
                Log.d("ConfigureIKettle", "onItemClick: " + activity.wifiSSID);
                btnNext.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listWifiNetworks.setVisibility(View.GONE);
                        txtInstructions.setText("Enter wifi password for " + activity.wifiSSID);
                        btnNext.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                password.setVisibility(View.GONE);
                                btnNext.setEnabled(false);
                                // FIXME kettle setup works, but not always first time!
                                // FIXME commands get no ack, or kettle doesn't save wifi settings.
                                // FIXME but works fine from the Go client, so maybe an Android problem?
                                txtInstructions.setText("Saving wifi settings, please wait... If this takes a long time, go back and try again (maybe 3 times!)");

                                txtError.setVisibility(View.GONE);
                                setWifiSSID.run();
                                setWifiPassword.run();
                                saveSettings.run();
                            }
                        });
                        password.setVisibility(View.VISIBLE);
                        password.addTextChangedListener(new TextWatcher() {
                            @Override
                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                            }

                            @Override
                            public void onTextChanged(CharSequence s, int start, int before, int count) {

                            }

                            @Override
                            public void afterTextChanged(Editable s) {
                                btnNext.setEnabled(s.toString().length() > 0);
                                activity.wifiPassword = s.toString();
                            }
                        });
                    }
                });
            }
        });


    }
}
