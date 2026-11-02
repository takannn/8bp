package com.cancer666.predictor

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 128, 64, 64)
            gravity = android.view.Gravity.CENTER_HORIZONTAL
        }

        status = TextView(this).apply {
            textSize = 16f
            setPadding(0, 0, 0, 32)
        }
        layout.addView(status)

        val grantBtn = Button(this).apply {
            text = "1. Grant Overlay Permission"
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                    )
                }
            }
        }
        layout.addView(grantBtn)

        val startBtn = Button(this).apply {
            text = "2. Start Overlay"
            setOnClickListener {
                if (canDrawOverlay()) {
                    startForegroundService(Intent(this@MainActivity, OverlayService::class.java))
                    status.text = "Overlay started. Buka 8 Ball Pool."
                } else {
                    status.text = "Grant overlay permission dulu (tombol 1)."
                }
            }
        }
        layout.addView(startBtn)

        val stopBtn = Button(this).apply {
            text = "3. Stop Overlay"
            setOnClickListener {
                stopService(Intent(this@MainActivity, OverlayService::class.java))
                status.text = "Overlay stopped."
            }
        }
        layout.addView(stopBtn)

        setContentView(layout)
        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        status.text = if (canDrawOverlay()) {
            "Overlay permission: GRANTED"
        } else {
            "Overlay permission: NOT GRANTED"
        }
    }

    private fun canDrawOverlay(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }
}