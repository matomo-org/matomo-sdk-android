/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.matomo.demo

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import org.matomo.sdk.QueryParams
import org.matomo.sdk.TrackMe
import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.EcommerceItems
import org.matomo.sdk.extra.MatomoApplication
import org.matomo.sdk.extra.TrackHelper

class DemoActivity : AppCompatActivity() {
    private var cartItems: Int = 0
    private var items: EcommerceItems? = null

    private val tracker: Tracker
        get() = (application as MatomoApplication).tracker


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo)
        items = EcommerceItems()

        findViewById<View>(R.id.trackMainScreenViewButton).setOnClickListener { v: View? ->
            TrackHelper.track(TrackMe().set(QueryParams.SESSION_START, 1))
                .screen("/")
                .title("Main screen")
                .with(tracker)
        }

        findViewById<View>(R.id.trackDispatchNow).setOnClickListener { v: View? -> tracker.dispatch() }

        findViewById<View>(R.id.trackCustomVarsButton).setOnClickListener { v: View? ->
            TrackHelper.track()
                .screen("/custom_vars")
                .title("Custom Vars")
                .variable(1, "first", "var")
                .variable(2, "second", "long value")
                .with(tracker)
        }

        findViewById<View>(R.id.raiseExceptionButton).setOnClickListener { v: View? ->
            TrackHelper.track()
                .exception(Exception("OnPurposeException"))
                .description("Crash button")
                .fatal(false)
                .with(tracker)
        }

        findViewById<View>(R.id.trackGoalButton).setOnClickListener { v: View? ->
            var revenue: Float
            try {
                revenue = (findViewById<View>(R.id.goalTextEditView) as EditText).text.toString().toInt().toFloat()
            } catch (e: Exception) {
                TrackHelper.track().exception(e).description("wrong revenue").with(tracker)
                revenue = 0f
            }
            TrackHelper.track().goal(1).revenue(revenue).with(tracker)
        }

        findViewById<View>(R.id.addEcommerceItemButton).setOnClickListener { v: View? ->
            val skus: List<String> = mutableListOf("00001", "00002", "00003", "00004")
            val names: List<String> = mutableListOf("Silly Putty", "Fishing Rod", "Rubber Boots", "Cool Ranch Doritos")
            val categories: List<String> = mutableListOf("Toys & Games", "Hunting & Fishing", "Footwear", "Grocery")
            val prices: List<Int> = mutableListOf(449, 3495, 2450, 250)

            val index = cartItems % 4
            val quantity = (cartItems / 4) + 1

            items!!.addItem(
                EcommerceItems.Item(skus[index])
                    .name(names[index])
                    .category(categories[index])
                    .price(prices[index])
                    .quantity(quantity)
            )
            cartItems++
        }

        findViewById<View>(R.id.trackEcommerceCartUpdateButton).setOnClickListener { v: View? ->
            TrackHelper.track()
                .cartUpdate(8600)
                .items(items)
                .with(tracker)
        }

        findViewById<View>(R.id.completeEcommerceOrderButton).setOnClickListener { v: View? ->
            TrackHelper.track()
                .order((10000 * Math.random()).toString(), 10000)
                .subTotal(1000)
                .tax(2000)
                .shipping(3000)
                .discount(500)
                .items(items)
                .with(tracker)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.demo, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_settings) {
            val intent = Intent(this, SettingsActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
