package uk.iankent.ikettle2;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class ChooseNewExistingKettle extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_new_existing_kettle);

        final Context ctx = this;
        final Button btnNewKettle = (Button)findViewById(R.id.btnNewKettle);
        final Button btnExistingKettle = (Button)findViewById(R.id.btnExistingKettle);

        btnNewKettle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(ctx, ConfigureIKettle2.class);
                startActivity(i);
            }
        });
        btnExistingKettle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(ctx, AddIKettle2.class);
                startActivity(i);
            }
        });
    }
}
