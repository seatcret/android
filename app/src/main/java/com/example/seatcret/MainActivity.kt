package com.example.seatcret

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject


class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val baseUrl = getString(R.string.server_url)
        val sharedPref = baseContext.getSharedPreferences("ephemeral", Context.MODE_PRIVATE)

        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener {task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "getInstanceId failed", task.exception)
                    return@OnCompleteListener
                }

                // Get new Instance ID token
                val token = task.result?.token

                // Store token in shared preference
                with (sharedPref.edit()) {
                    putString("fcm_token", token)
                    commit()
                }

                // Log and toast
                val msg = getString(R.string.msg_token_fmt, token)
                Log.d(TAG, msg)
                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()

                val queue = Volley.newRequestQueue(this)

                val request = object : StringRequest(Method.POST, "$baseUrl/users/", Response.Listener {
                    Toast.makeText(baseContext, "Successfully registered device!", Toast.LENGTH_SHORT).show()
                }, Response.ErrorListener { error ->
                    Log.d(TAG, error.toString())
                }) {
                    override fun getBodyContentType(): String {
                        return "application/json"
                    }
                    override fun getBody(): ByteArray {
                        val body = HashMap<String, String>()
                        body["token"] = token.toString()
                        body["platform"] = "fcm"
                        return JSONObject(body as Map<String, String>).toString().toByteArray()
                    }
                }
                queue.add(request)
            })

        val webView: WebView = findViewById(R.id.webview)
        webView.webViewClient = WebViewClient()

        val storedToken = sharedPref.getString("fcm_token", null)
        CookieManager.getInstance().setCookie(baseUrl, "token=fcm:$storedToken")

        if (intent.action == Intent.ACTION_MAIN) {
            webView.loadUrl(baseUrl)
        } else {
            webView.loadUrl(intent?.data.toString())
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Check if the key event was the Back button and if there's history
        if (keyCode == KeyEvent.KEYCODE_BACK && webview.canGoBack()) {
            webview.goBack()
            return true
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event)
    }
}
