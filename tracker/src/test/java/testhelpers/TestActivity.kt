package testhelpers

import android.app.Activity
import android.os.Bundle

class TestActivity : Activity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = testTitle
    }

    companion object {
        @JvmStatic
        val testTitle: String
            get() = "Test Activity"
    }
}
