package uk.iankent.ikettle2;

import android.app.Activity;
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
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.apache.commons.net.util.SubnetUtils;

import java.io.IOException;
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
import uk.iankent.ikettle2.client.KettleAutoDacResponse;
import uk.iankent.ikettle2.client.KettleCommandAckResponse;
import uk.iankent.ikettle2.client.KettleDeviceInfoResponse;
import uk.iankent.ikettle2.client.KettleError;
import uk.iankent.ikettle2.client.KettleResponse;
import uk.iankent.ikettle2.client.KettleStatusResponse;
import uk.iankent.ikettle2.client.NetworkScanner;
import uk.iankent.ikettle2.client.OnKettleResponse;
import uk.iankent.ikettle2.data.Kettle;
import uk.iankent.ikettle2.data.KettleAdapter;
import uk.iankent.ikettle2.data.Kettles;

public class AddIKettle2 extends AppCompatActivity {

    protected ArrayList<Kettle> kettlesFound = new ArrayList<Kettle>();
    protected NetworkScanner scanner = new NetworkScanner();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final Activity activity = this;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_ikettle2);

        final EditText txtHostIP = (EditText)findViewById(R.id.inputHostIP);
        final TextView textError = (TextView)findViewById(R.id.textError);
        final Button btnSave = (Button)findViewById(R.id.btnSave);
        final KettleAdapter adapter = new KettleAdapter(this, kettlesFound);
        final TextView txtScanning = (TextView)findViewById(R.id.txtScanning);

        final ProgressBar progressScanning = (ProgressBar)findViewById(R.id.progressScanning);
        scanner.onKettleFound = new OnKettleResponse<KettleDeviceInfoResponse>() {
            @Override
            public void onKettleResponse(KettleDeviceInfoResponse response) {
                kettlesFound.add(new Kettle(response.getHostname(), response.getPort()));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
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
                        progressScanning.setVisibility(View.INVISIBLE);
                        txtScanning.setVisibility(View.INVISIBLE);
                    }
                });
            }
        };
        kettlesFound.clear();
        scanner.Start(getApplicationContext());
        progressScanning.setVisibility(View.VISIBLE);
        txtScanning.setVisibility(View.VISIBLE);

        ListView lv = (ListView)findViewById(R.id.listViewDevices);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Kettle chosen = adapter.getItem(position);
                txtHostIP.setText(chosen.Host);
            }
        });

        btnSave.setEnabled(false);

        txtHostIP.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    btnSave.setEnabled(true);
                } else {
                    btnSave.setEnabled(false);
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
                textError.setVisibility(View.GONE);
                scanner.Stop();
                progressScanning.setVisibility(View.VISIBLE);
                txtScanning.setText("Saving");
                final IKettle2Client kettleClient = new IKettle2Client(txtHostIP.getText().toString());
                kettleClient.setKettleListener(new IKettle2Client.KettleListener() {
                    @Override
                    public void onError(final KettleError error) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textError.setText(error.getException().getMessage());
                                textError.setVisibility(View.VISIBLE);
                                btnSave.setEnabled(true);
                            }
                        });
                    }

                    @Override
                    public void onConnected() {
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

                    @Override
                    public void onDisconnected() {

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
