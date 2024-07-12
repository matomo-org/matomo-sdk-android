package testhelpers

import android.util.Log
import timber.log.Timber

class JUnitTree : Timber.DebugTree() {
    private val minLogLevel = Log.VERBOSE

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority < minLogLevel) return
        println(System.currentTimeMillis().toString() + " " + priorityToString(priority) + "/" + tag + ": " + message)
    }

    companion object {
        private fun priorityToString(priority: Int): String {
            return when (priority) {
                Log.ERROR -> "E"
                Log.WARN -> "W"
                Log.INFO -> "I"
                Log.DEBUG -> "D"
                Log.VERBOSE -> "V"
                else -> priority.toString()
            }
        }
    }
}
