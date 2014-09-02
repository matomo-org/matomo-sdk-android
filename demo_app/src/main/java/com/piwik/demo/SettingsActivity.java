package com.piwik.demo;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import org.piwik.sdk.PiwikApplication;


public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        final Activity settingsActivity = this;

        // auto track button
        Button button = (Button) findViewById(R.id.manuallyTrackSettingsScreenViewButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((PiwikApplication) getApplication()).getTracker().activityStart(settingsActivity);
            }
        });

        // Dry run
        CheckBox dryRun = (CheckBox) findViewById(R.id.dryRunCheckbox);
        dryRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((PiwikApplication) getApplication()).getGlobalSettings().setDryRun(((CheckBox) v).isChecked());
            }
        });

        // out out
        CheckBox optOut = (CheckBox) findViewById(R.id.optOutCheckbox);
        optOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((PiwikApplication) getApplication()).getGlobalSettings().setAppOptOut(((CheckBox) v).isChecked());
            }
        });

        // dispatch interval
        EditText input = (EditText) findViewById(R.id.dispatchIntervallInput);
        input.setText(Integer.toString(
                ((PiwikApplication) getApplication()).getTracker().getDispatchInterval()
        ));
        input.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                        try {
                            int interval = Integer.valueOf(charSequence.toString().trim());
                            ((PiwikApplication) getApplication()).getTracker()
                                    .setDispatchInterval(interval);
                        } catch (NumberFormatException e) {
                            Log.d("not a number", charSequence.toString());
                        }
                    }

                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                    }
                }

        );

    }


}
