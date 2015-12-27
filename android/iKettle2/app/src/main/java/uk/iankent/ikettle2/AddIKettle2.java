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

        class CheckKettleConnection extends AsyncTask<String, Void, String> {
            // FIXME no need to use strings
            @Override
            protected String doInBackground(String... params) {
                IKettle2Client kettleClient = new IKettle2Client(params[0]);
                try {
                    kettleClient.Test();
                    return "OK";
                } catch (IOException e) {
                    return e.getMessage();
                }
            }

            @Override
            protected void onPostExecute(String s) {
                switch(s) {
                    case "OK":
                        activity.finish();
                        Kettle k = new Kettle(txtHostIP.getText().toString(), 2081);
                        Kettles.Get().add(k);
                        break;
                    default:
                        textError.setText(s);
                        textError.setVisibility(View.VISIBLE);
                        btnSave.setEnabled(true);
                        break;
                }
            }
        }

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnSave.setEnabled(false);
                textError.setVisibility(View.GONE);
                CheckKettleConnection c = new CheckKettleConnection();
                c.execute(txtHostIP.getText().toString());
            }
        });
    }
}
