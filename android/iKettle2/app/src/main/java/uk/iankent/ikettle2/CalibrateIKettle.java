package uk.iankent.ikettle2;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import uk.iankent.ikettle2.client.IKettle2Client;
import uk.iankent.ikettle2.client.KettleAutoDacResponse;
import uk.iankent.ikettle2.client.KettleError;
import uk.iankent.ikettle2.client.OnKettleResponse;
import uk.iankent.ikettle2.data.Kettle;
import uk.iankent.ikettle2.data.Kettles;

public class CalibrateIKettle extends AppCompatActivity {

    public static IKettle2Client client;
    public static Kettle kettle;

    protected OnKettleResponse<KettleError> onErrorHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibrate_ikettle);

        final TextView txtError = (TextView)findViewById(R.id.txtError);
        final Button btnContinue = (Button)findViewById(R.id.btnContinue);

        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(client==null||kettle==null)return; // shouldn't happen

                client.onKettleAutoDacResponse = new OnKettleResponse<KettleAutoDacResponse>() {
                    @Override
                    public void onKettleResponse(KettleAutoDacResponse response) {
                        Log.d("CalibrateIKettle", "onKettleResponse: autoDac");
                        kettle.OffBaseWeight = response.getOffBaseWeight();
                        Kettles.Save(CalibrateIKettle.this);
                        client.onError = onErrorHandler;
                        finish();
                    }
                };


                onErrorHandler = client.onError;
                client.onError = new OnKettleResponse<KettleError>() {
                    @Override
                    public void onKettleResponse(final KettleError response) {
                        Log.d("CalibrateIKettle", "onKettleResponse: error");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                btnContinue.setEnabled(true);
                                txtError.setText(response.getException().getMessage());
                                txtError.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                };

                btnContinue.setEnabled(false);
                txtError.setVisibility(View.INVISIBLE);
                client.Calibrate();
            }
        });
    }

    @Override
    public void onBackPressed() {
        client.onError = onErrorHandler;
    }
}
