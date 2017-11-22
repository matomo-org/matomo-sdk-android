/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package com.piwik.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import org.piwik.sdk.Tracker;
import org.piwik.sdk.extra.EcommerceItems;
import org.piwik.sdk.extra.PiwikApplication;
import org.piwik.sdk.extra.TrackHelper;

import java.util.Arrays;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.OnClick;


public class DemoActivity extends AppCompatActivity {
    int cartItems = 0;
    private EcommerceItems items;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        ButterKnife.bind(this);
        items = new EcommerceItems();
    }

    private Tracker getTracker() {
        return ((PiwikApplication) getApplication()).getTracker();
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
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.trackMainScreenViewButton)
    void onTrackMainScreenClicked(View view) {
        TrackHelper.track().screen("/").title("Main screen").with(getTracker());
    }

    @OnClick(R.id.trackCustomVarsButton)
    void onTrackCustomVarsClicked(View view) {
        TrackHelper.track()
                .screen("/custom_vars")
                .title("Custom Vars")
                .variable(1, "first", "var")
                .variable(2, "second", "long value")
                .with(getTracker());
    }

    @OnClick(R.id.raiseExceptionButton)
    void onRaiseExceptionClicked(View view) {
        TrackHelper.track().exception(new Exception("OnPurposeException")).description("Crash button").fatal(false).with(getTracker());
    }

    @OnClick(R.id.trackGoalButton)
    void onTrackGoalClicked(View view) {
        float revenue;
        try {
            revenue = Integer.valueOf(
                    ((EditText) findViewById(R.id.goalTextEditView)).getText().toString()
            );
        } catch (Exception e) {
            TrackHelper.track().exception(e).description("wrong revenue").with(getTracker());
            revenue = 0;
        }
        TrackHelper.track().goal(1).revenue(revenue).with(getTracker());
    }

    @OnClick(R.id.addEcommerceItemButton)
    void onAddEcommerceItemClicked(View view) {
        List<String> skus = Arrays.asList("00001", "00002", "00003", "00004");
        List<String> names = Arrays.asList("Silly Putty", "Fishing Rod", "Rubber Boots", "Cool Ranch Doritos");
        List<String> categories = Arrays.asList("Toys & Games", "Hunting & Fishing", "Footwear", "Grocery");
        List<Integer> prices = Arrays.asList(449, 3495, 2450, 250);

        int index = cartItems % 4;
        int quantity = (cartItems / 4) + 1;

        items.addItem(new EcommerceItems.Item(skus.get(index))
                .name(names.get(index))
                .category(categories.get(index))
                .price(prices.get(index))
                .quantity(quantity));
        cartItems++;
    }

    @OnClick(R.id.trackEcommerceCartUpdateButton)
    void onTrackEcommerceCartUpdateClicked(View view) {
        TrackHelper.track().cartUpdate(8600).items(items).with(getTracker());
    }

    @OnClick(R.id.completeEcommerceOrderButton)
    void onCompleteEcommerceOrderClicked(View view) {
        TrackHelper.track()
                .order(String.valueOf(10000 * Math.random()), 10000)
                .subTotal(1000)
                .tax(2000)
                .shipping(3000)
                .discount(500)
                .items(items)
                .with(getTracker());
    }

}
