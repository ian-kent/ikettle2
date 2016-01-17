package uk.iankent.ikettle2;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.tv.TvContract;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.apache.commons.net.util.SubnetUtils;

import java.io.IOException;
import java.io.LineNumberReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import uk.iankent.ikettle2.client.IKettle2Client;
import uk.iankent.ikettle2.client.KettleDeviceInfoResponse;
import uk.iankent.ikettle2.client.KettleError;
import uk.iankent.ikettle2.client.KettleResponse;
import uk.iankent.ikettle2.client.NetworkScanner;
import uk.iankent.ikettle2.client.OnKettleResponse;
import uk.iankent.ikettle2.data.Kettle;
import uk.iankent.ikettle2.data.KettleAdapter;
import uk.iankent.ikettle2.data.Kettles;

public class AddIKettle2 extends AppCompatActivity {

    protected ArrayList<Kettle> kettlesFound = new ArrayList<Kettle>();
    protected NetworkScanner scanner = new NetworkScanner();

    protected View.OnClickListener startListener;
    protected View.OnClickListener stopListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_ikettle2);

        final EditText txtHostIP = (EditText)findViewById(R.id.inputHostIP);
        final TextView textStatus = (TextView)findViewById(R.id.textHostIP);
        final TextView textError = (TextView)findViewById(R.id.textError);
        final Button btnSave = (Button)findViewById(R.id.btnSave);
        final KettleAdapter adapter = new KettleAdapter(this, kettlesFound);
        final Button btnStop = (Button)findViewById(R.id.btnStop);
        final Button btnAdvanced = (Button)findViewById(R.id.btnAdvanced);
        final ProgressBar progressScanning = (ProgressBar)findViewById(R.id.progressScanning);
        final ListView listDevices = (ListView)findViewById(R.id.listViewDevices);
        final GridLayout layoutAdvanced = (GridLayout)findViewById(R.id.layoutAdvanced);
        final EditText txtTimeout = (EditText)findViewById(R.id.inputTimeout);
        final EditText txtCIDR = (EditText)findViewById(R.id.inputCIDR);

        txtTimeout.setText("250");
        txtCIDR.setText(NetworkScanner.getSubnet(this));

        stopListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanner.Stop();
                btnStop.setEnabled(false);
            }
        };
        startListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                kettlesFound.clear();
                adapter.notifyDataSetChanged();
                scanner.Start(getApplicationContext(), txtCIDR.getText().toString(), Integer.valueOf(txtTimeout.getText().toString()));
                progressScanning.setVisibility(View.VISIBLE);
                btnStop.setText("Stop");
                btnStop.setOnClickListener(stopListener);
                textStatus.setText("Scanning for your iKettle 2.0, please wait...");
            }
        };

        btnStop.setOnClickListener(stopListener);

        btnAdvanced.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnAdvanced.setVisibility(View.GONE);
                layoutAdvanced.setVisibility(View.VISIBLE);
            }
        });

        scanner.onKettleFound = new OnKettleResponse<KettleDeviceInfoResponse>() {
            @Override
            public void onKettleResponse(KettleDeviceInfoResponse response) {
                kettlesFound.add(new Kettle(response.getHostname(), response.getPort()));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listDevices.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        };
        scanner.onScanFinished = new OnKettleResponse() {
            @Override
            public void onKettleResponse(KettleResponse response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressScanning.setVisibility(View.GONE);
                        if(kettlesFound.size() == 0) {
                            textStatus.setText("Finished scanning, no kettles found");
                            listDevices.setVisibility(View.INVISIBLE);
                        } else if(kettlesFound.size() == 1) {
                            textStatus.setText("Finished scanning, found 1 kettle");
                        } else {
                            textStatus.setText("Finished scanning, found " + String.valueOf(kettlesFound.size()) + " kettles");
                        }
                        btnStop.setText("Try again");
                        btnStop.setEnabled(true);
                        btnStop.setOnClickListener(startListener);
                    }
                });
            }
        };

        kettlesFound.clear();
        scanner.Start(getApplicationContext(), txtCIDR.getText().toString(), Integer.valueOf(txtTimeout.getText().toString()));

        ListView lv = (ListView)findViewById(R.id.listViewDevices);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Kettle chosen = adapter.getItem(position);
                txtHostIP.setText(chosen.Host);
            }
        });

        btnSave.setVisibility(View.GONE);

        txtHostIP.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    btnSave.setVisibility(View.VISIBLE);
                } else {
                    btnSave.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnSave.setEnabled(false);
                btnStop.setEnabled(false);
                textError.setVisibility(View.GONE);
                scanner.Stop();
                progressScanning.setVisibility(View.VISIBLE);
                textStatus.setText("Adding your iKettle 2.0, please wait...");
                final IKettle2Client kettleClient = new IKettle2Client(txtHostIP.getText().toString());
                kettleClient.onConnected = new OnKettleResponse() {
                    @Override
                    public void onKettleResponse(KettleResponse response) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    kettleClient.Disconnect();
                                } catch (IOException e) {
                                }

                                Kettle k = new Kettle(txtHostIP.getText().toString(), 2081);
                                Kettles.Get().add(k);
                                Kettles.Save(AddIKettle2.this);

                                Intent intent = new Intent(AddIKettle2.this, Home.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
                    }
                };
                kettleClient.onError = new OnKettleResponse<KettleError>() {
                    @Override
                    public void onKettleResponse(final KettleError response) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textError.setText(response.getException().getMessage());
                                textError.setVisibility(View.VISIBLE);
                                btnSave.setEnabled(true);
                                btnStop.setEnabled(true);
                            }
                        });
                    }
                };
                kettleClient.Connect();
            }
        });
    }

    @Override
    public void onBackPressed() {
        scanner.Stop();
        super.onBackPressed();
    }
}
