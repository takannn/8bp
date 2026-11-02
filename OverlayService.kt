package com.cancer666.predictor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var predictionView: PredictionOverlayView? = null
    private var menuView: FloatingMenuView? = null

    private var predictionParams: WindowManager.LayoutParams? = null
    private var menuParams: WindowManager.LayoutParams? = null

    private var editMode = true

    private val balls = mutableListOf<Ball>()
    private var cueBall: Ball = Ball(540f, 1400f, 18f)
    private var table = TableGeometry(60f, 200f, 1020f, 1700f)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        addPredictionOverlay()
        addFloatingMenu()
    }

    private fun addPredictionOverlay() {
        val view = PredictionOverlayView(this).apply {
            this.table = this@OverlayService.table
            this.cueBall = this@OverlayService.cueBall
            this.objectBalls = this@OverlayService.balls
            cueRadius = 18f
            onBallPlaced = { x, y ->
                if (editMode) {
                    val nearest = balls.minByOrNull {
                        val dx = it.x - x
                        val dy = it.y - y
                        kotlin.math.sqrt(dx * dx + dy * dy)
                    }
                    if (nearest != null) {
                        val dx = nearest.x - x
                        val dy = nearest.y - y
                        val d = kotlin.math.sqrt(dx * dx + dy * dy)
                        if (d < nearest.radius * 3f) {
                            nearest.x = x
                            nearest.y = y
                        } else {
                            balls.add(Ball(x, y, 18f))
                        }
                    } else {
                        balls.add(Ball(x, y, 18f))
                    }
                    view.invalidate()
                }
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        windowManager.addView(view, params)
        predictionView = view
        predictionParams = params
    }

    private fun addFloatingMenu() {
        val menu = FloatingMenuView(this).apply {
            onToggleOverlay = { enabled ->
                predictionView?.visibility = if (enabled) android.view.View.VISIBLE else android.view.View.GONE
            }
            onToggleEditMode = { enabled ->
                editMode = enabled
            }
            onClearBalls = {
                balls.clear()
                predictionView?.predictedSegments = floatArrayOf()
                predictionView?.invalidate()
            }
            onBouncesChanged = { n ->
                predictionView?.maxBounces = n
                predictionView?.invalidate()
            }
            onMenuDrag = { dx, dy ->
                menuParams?.let { p ->
                    p.x += dx.toInt()
                    p.y += dy.toInt()
                    menuView?.let { windowManager.updateViewLayout(it, p) }
                }
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 120
        }
        windowManager.addView(menu, params)
        menuView = menu
        menuParams = params
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "8BP Predictor",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Overlay active" }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("8BP Predictor")
                .setContentText("Trajectory overlay running")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("8BP Predictor")
                .setContentText("Trajectory overlay running")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .build()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        predictionView?.let { windowManager.removeView(it) }
        menuView?.let { windowManager.removeView(it) }
        predictionView = null
        menuView = null
    }

    companion object {
        private const val CHANNEL_ID = "8bp_predictor_channel"
        private const val NOTIFICATION_ID = 6661
    }
}