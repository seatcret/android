package com.example.seatcret

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject


class MyFirebaseMessagingService : FirebaseMessagingService() {
    companion object {
        private const val TAG = "MyFirebaseMessagingService"
    }

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")

        // Store token in shared preference
        val sharedPref = baseContext.getSharedPreferences("ephemeral", Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putString("fcm_token", token)
            commit()
        }

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(token)
    }

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private fun sendRegistrationToServer(token: String) {
        Log.d(TAG, "sendRegistrationTokenToServer($token)")

        val queue = Volley.newRequestQueue(this)
        val url = getString(R.string.server_url)

        val request = object: StringRequest(Method.POST, "$url/users/", Response.Listener { },
            Response.ErrorListener { error -> Log.d(TAG, error.toString())
        }) {
            override fun getBodyContentType(): String {
                return "application/json"
            }
            override fun getBody(): ByteArray {
                val body = HashMap<String, String>()
                body["token"] = token
                body["platform"] = "fcm"
                return JSONObject(body as Map<String, String>).toString().toByteArray()
            }
        }
        queue.add(request)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
        }

        val messageBody = remoteMessage.notification!!.body
        Log.d(TAG, "FCM Notification Message Body : $messageBody")

        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(
                applicationContext,
                messageBody,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
