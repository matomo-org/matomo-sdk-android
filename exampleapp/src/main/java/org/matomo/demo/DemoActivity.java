/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.matomo.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import org.matomo.sdk.QueryParams;
import org.matomo.sdk.TrackMe;
import org.matomo.sdk.Tracker;
import org.matomo.sdk.extra.EcommerceItems;
import org.matomo.sdk.extra.MatomoApplication;
import org.matomo.sdk.extra.TrackHelper;

import java.util.Arrays;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;


public class DemoActivity extends AppCompatActivity {
    int cartItems = 0;
    private EcommerceItems items;

    private Tracker getTracker() {
        return ((MatomoApplication) getApplication()).getTracker();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        items = new EcommerceItems();

        findViewById(R.id.trackMainScreenViewButton).setOnClickListener(v ->
                TrackHelper.track(new TrackMe().set(QueryParams.SESSION_START, 1))
                        .screen("/")
                        .title("Main screen")
                        .with(getTracker())
        );

        findViewById(R.id.trackDispatchNow).setOnClickListener(v ->
                getTracker().dispatch()
        );

        findViewById(R.id.trackCustomVarsButton).setOnClickListener(v ->
                TrackHelper.track()
                        .screen("/custom_vars")
                        .title("Custom Vars")
                        .variable(1, "first", "var")
                        .variable(2, "second", "long value")
                        .with(getTracker())
        );

        findViewById(R.id.raiseExceptionButton).setOnClickListener(v -> TrackHelper.track()
                .exception(new Exception("OnPurposeException"))
                .description("Crash button")
                .fatal(false)
                .with(getTracker())
        );

        findViewById(R.id.trackGoalButton).setOnClickListener(v -> {
            float revenue;
            try {
                revenue = Integer.valueOf(((EditText) findViewById(R.id.goalTextEditView)).getText().toString()
                );
            } catch (Exception e) {
                TrackHelper.track().exception(e).description("wrong revenue").with(getTracker());
                revenue = 0;
            }
            TrackHelper.track().goal(1).revenue(revenue).with(getTracker());
        });

        findViewById(R.id.addEcommerceItemButton).setOnClickListener(v -> {
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
        });

        findViewById(R.id.trackEcommerceCartUpdateButton).setOnClickListener(v ->
                TrackHelper.track()
                        .cartUpdate(8600)
                        .items(items)
                        .with(getTracker())
        );

        findViewById(R.id.completeEcommerceOrderButton).setOnClickListener(v ->
                TrackHelper.track()
                        .order(String.valueOf(10000 * Math.random()), 10000)
                        .subTotal(1000)
                        .tax(2000)
                        .shipping(3000)
                        .discount(500)
                        .items(items)
                        .with(getTracker())
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.demo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
