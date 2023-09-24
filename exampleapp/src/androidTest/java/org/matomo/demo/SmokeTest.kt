package org.matomo.demo

import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.core.graphics.writeToTestStorage
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.screenshot.captureToBitmap
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.matomo.demo.DemoActivity

@RunWith(AndroidJUnit4::class)
class SmokeTest {

    private val WAIT_SLIDER = 600L

    @get:Rule
    val activityScenarioRule = activityScenarioRule<DemoActivity>()

    @get:Rule
    var nameRule = TestName()

    @Test
    fun testExpand() {
        onView(withId(R.id.trackMainScreenViewButton)).perform(click())
        onView(withId(R.id.trackDispatchNow)).perform(click())
        onView(withId(R.id.trackCustomVarsButton)).perform(click())
        onView(withId(R.id.raiseExceptionButton)).perform(click())
        onView(withId(R.id.addEcommerceItemButton)).perform(click())
        onView(withId(R.id.trackEcommerceCartUpdateButton)).perform(click())
        onView(withId(R.id.completeEcommerceOrderButton)).perform(click())
        onView(withId(R.id.trackGoalButton)).perform(click())
        onView(withId(R.id.goalTextEditView)).perform(click())
    }

}
