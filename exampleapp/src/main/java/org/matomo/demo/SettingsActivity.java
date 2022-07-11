/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.matomo.demo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import org.matomo.sdk.extra.MatomoApplication;
import org.matomo.sdk.extra.TrackHelper;

import java.util.ArrayList;
import java.util.Collections;

import timber.log.Timber;


public class SettingsActivity extends Activity {

    private void refreshUI(final Activity settingsActivity) {
        // auto track button
        Button button = findViewById(R.id.bindtoapp);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TrackHelper.track().screens(getApplication()).with(((MatomoApplication) getApplication()).getTracker());
            }
        });

        // Dry run
        CheckBox dryRun = findViewById(R.id.dryRunCheckbox);
        dryRun.setChecked(((MatomoApplication) getApplication()).getTracker().getDryRunTarget() != null);
        dryRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MatomoApplication) getApplication()).getTracker().setDryRunTarget(((CheckBox) v).isChecked() ? Collections.synchronizedList(new ArrayList<>()) : null);
            }
        });

        // out out
        CheckBox optOut = findViewById(R.id.optOutCheckbox);
        optOut.setChecked(((MatomoApplication) getApplication()).getTracker().isOptOut());
        optOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MatomoApplication) getApplication()).getTracker().setOptOut(((CheckBox) v).isChecked());
            }
        });

        // dispatch interval
        EditText input = findViewById(R.id.dispatchIntervallInput);
        input.setText(Long.toString(
                ((MatomoApplication) getApplication()).getTracker().getDispatchInterval()
        ));
        input.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                        try {
                            int interval = Integer.parseInt(charSequence.toString().trim());
                            ((MatomoApplication) getApplication()).getTracker()
                                    .setDispatchInterval(interval);
                        } catch (NumberFormatException e) {
                            Timber.d("not a number: %s", charSequence.toString());
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
        input = findViewById(R.id.sessionTimeoutInput);
        input.setText(Long.toString(
                (((MatomoApplication) getApplication()).getTracker().getSessionTimeout() / 60000)
        ));
        input.addTextChangedListener(
                new TextWatcher() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                        try {
                            int timeoutMin = Integer.parseInt(charSequence.toString().trim());
                            timeoutMin = Math.abs(timeoutMin);
                            ((MatomoApplication) getApplication()).getTracker()
                                    .setSessionTimeout(timeoutMin * 60);
                        } catch (NumberFormatException e) {
                            ((EditText) settingsActivity.findViewById(R.id.sessionTimeoutInput)).setText("30");
                            Timber.d("not a number: %s", charSequence.toString());
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
