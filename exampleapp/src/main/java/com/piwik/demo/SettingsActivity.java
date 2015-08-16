/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package com.piwik.demo;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import org.piwik.sdk.PiwikApplication;
import org.piwik.sdk.QuickTrack;
import org.piwik.sdk.tools.Logy;


public class SettingsActivity extends Activity {

    private void refreshUI(final Activity settingsActivity) {
        // auto track button
        Button button = (Button) findViewById(R.id.manuallyTrackSettingsScreenViewButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                QuickTrack.track((PiwikApplication) getApplication(), settingsActivity);
            }
        });

        // Dry run
        CheckBox dryRun = (CheckBox) findViewById(R.id.dryRunCheckbox);
        dryRun.setChecked(((PiwikApplication) getApplication()).getPiwik().isDryRun());
        dryRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((PiwikApplication) getApplication()).getPiwik().setDryRun(((CheckBox) v).isChecked());
            }
        });

        // out out
        CheckBox optOut = (CheckBox) findViewById(R.id.optOutCheckbox);
        optOut.setChecked(((PiwikApplication) getApplication()).getPiwik().isOptOut());
        optOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((PiwikApplication) getApplication()).getPiwik().setOptOut(((CheckBox) v).isChecked());
            }
        });

        // dispatch interval
        EditText input = (EditText) findViewById(R.id.dispatchIntervallInput);
        input.setText(Long.toString(
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
                            Logy.d("not a number", charSequence.toString());
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

        //session Timeout Input
        input = (EditText) findViewById(R.id.sessionTimeoutInput);
        input.setText(Long.toString(
                (((PiwikApplication) getApplication()).getTracker().getSessionTimeout() / 60000)
        ));
        input.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                        try {
                            int timeoutMin = Integer.valueOf(charSequence.toString().trim());
                            timeoutMin = Math.abs(timeoutMin);
                            ((PiwikApplication) getApplication()).getTracker()
                                    .setSessionTimeout(timeoutMin * 60);
                        } catch (NumberFormatException e) {
                            ((EditText) settingsActivity.findViewById(R.id.sessionTimeoutInput)).setText("30");
                            Logy.d("not a number", charSequence.toString());
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        refreshUI(this);
    }

}
