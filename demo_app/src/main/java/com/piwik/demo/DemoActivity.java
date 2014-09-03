package com.piwik.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import org.piwik.sdk.PiwikApplication;


public class DemoActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        initPiwik();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.demo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initPiwik() {
        // do not send http requests
        ((PiwikApplication) getApplication()).getGlobalSettings().setDryRun(false);

        ((PiwikApplication) getApplication()).getTracker()
                .setDispatchInterval(5)
                .trackAppDownload()
                .reportUncaughtExceptions(true);

        initTrackViewListeners();
    }

    protected void initTrackViewListeners() {
        // simple track view
        Button button = (Button) findViewById(R.id.trackMainScreenViewButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((PiwikApplication) getApplication()).getTracker().trackScreenView("/", "Main screen");
            }
        });

        // custom vars track view
        button = (Button) findViewById(R.id.trackCustomVarsButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((PiwikApplication) getApplication()).getTracker()
                        .setScreenCustomVariable(1, "first", "var")
                        .setScreenCustomVariable(2, "second", "long value")
                        .trackScreenView("/custom_vars", "Custom Vars");
            }
        });

        // exception tracking
        button = (Button) findViewById(R.id.raiseExceptionButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int a = 1 / 0;
            }
        });

        // goal tracking
        button = (Button) findViewById(R.id.trackGoalButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int revenue;
                try {
                    revenue = Integer.valueOf(
                            ((EditText) findViewById(R.id.goalTextEditView)).getText().toString()
                    );
                } catch (Exception e) {
                    ((PiwikApplication) getApplication()).getTracker().trackException("DemoActivity", "wrong revenue", false);
                    revenue = 0;
                }
                ((PiwikApplication) getApplication()).getTracker().trackGoal(1, revenue);
            }
        });

    }
}
