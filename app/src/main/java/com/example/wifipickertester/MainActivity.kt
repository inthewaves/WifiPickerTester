package com.example.wifipickertester

import android.app.Activity
import android.content.Intent
import android.net.wifi.WifiManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button).setOnClickListener {
            val intent = Intent(WifiManager.ACTION_PICK_WIFI_NETWORK)
                    .putExtra("extra_prefs_show_button_bar", true)
            startActivityForResult(intent, WIFI_PICKER_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == WIFI_PICKER_REQUEST_CODE) {
            findViewById<TextView>(R.id.resultCodeTextView).text = resultCodeMap[resultCode]

            findViewById<TextView>(R.id.intentTextView).text = intent.toString()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        private const val WIFI_PICKER_REQUEST_CODE = 0

        private val resultCodeMap = mapOf(
                Activity.RESULT_CANCELED to "RESULT_CANCELED",
                Activity.RESULT_OK to "RESULT_OK",
                Activity.RESULT_FIRST_USER to "RESULT_FIRST_USER"
        )
    }
}
