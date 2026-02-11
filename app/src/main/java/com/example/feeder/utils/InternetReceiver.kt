import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.view.View
import com.example.feeder.utils.CheckInternetConnection
import com.google.android.material.snackbar.Snackbar

class InternetReceiver(
    private val context: Context,
    private val rootView: View,
    private val onInternetRestored: () -> Unit
) : BroadcastReceiver() {

    private var snackbar: Snackbar? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        if (CheckInternetConnection.isNetworkAvailable(this.context)) {
            snackbar?.dismiss()
            onInternetRestored()
            unregister()
        }
    }

    fun register() {
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        context.registerReceiver(this, filter)

        showSnackbar()
    }

    private fun showSnackbar() {
        try {
            snackbar = Snackbar.make(
                rootView,
                "No Internet Connection! Retry or wait...",
                Snackbar.LENGTH_INDEFINITE
            ).setAction("Retry") {
                if (CheckInternetConnection.isNetworkAvailable(context)) {
                    snackbar?.dismiss()
                    onInternetRestored()
                    unregister()
                } else {
                    snackbar?.dismiss()
                    showSnackbar()
                }
            }
            snackbar?.show()
        } catch (e: Exception) {
            e.localizedMessage
        }

    }

    fun unregister() {
        try {
            context.unregisterReceiver(this)
        } catch (e: IllegalArgumentException) {
            // Receiver was not registered or already unregistered
        }
    }
}
