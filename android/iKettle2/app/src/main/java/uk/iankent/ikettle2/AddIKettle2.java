package uk.iankent.ikettle2;

import android.app.Activity;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import uk.iankent.ikettle2.client.IKettle2Client;
import uk.iankent.ikettle2.client.KettleError;
import uk.iankent.ikettle2.client.KettleResponse;
import uk.iankent.ikettle2.client.OnKettleResponse;
import uk.iankent.ikettle2.data.Kettle;
import uk.iankent.ikettle2.data.Kettles;

public class AddIKettle2 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final Activity activity = this;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_ikettle2);

        final EditText txtHostIP = (EditText)findViewById(R.id.inputHostIP);
        final TextView textError = (TextView)findViewById(R.id.textError);
        final Button btnSave = (Button)findViewById(R.id.btnSave);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            btnSave.setEnabled(false);
            textError.setVisibility(View.GONE);
            final IKettle2Client kettleClient = new IKettle2Client(txtHostIP.getText().toString());
            kettleClient.onConnected = new OnKettleResponse() {
                @Override
                public void onKettleResponse(KettleResponse response) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                kettleClient.Disconnect();
                            } catch (IOException e) {}

                            Kettle k = new Kettle(txtHostIP.getText().toString(), 2081);
                            Kettles.Get().add(k);
                            activity.finish();
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
                        }
                    });
                }
            };
            kettleClient.Connect();
            }
        });
    }
}
