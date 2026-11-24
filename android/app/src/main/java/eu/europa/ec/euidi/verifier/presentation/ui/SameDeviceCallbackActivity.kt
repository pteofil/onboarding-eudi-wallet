package eu.europa.ec.euidi.verifier.presentation.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import ro.banking40.team01.MainActivity

/**
 * The event wallet returns only to this exact class name inside the team's package (the one the
 * Tremend verifier SDK uses). We don't use the SDK, so this stand-in forwards the return URI,
 * `ro.banking40.team01://same-device#response_code=...`, to MainActivity and closes.
 */
class SameDeviceCallbackActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(
            Intent(this, MainActivity::class.java)
                .setAction(Intent.ACTION_VIEW)
                .setData(intent?.data),
        )
        finish()
    }
}
