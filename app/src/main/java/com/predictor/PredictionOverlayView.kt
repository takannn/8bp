package com.cancer666.predictor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import android.view.View

class PredictionOverlayView(context: Context) : View(context) {

    private val segmentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 0, 255, 180)
        strokeWidth = 3f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val cuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 255, 255, 255)
        style = Paint.Style.FILL
    }

    private val objectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 255, 120, 60)
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val path = Path()

    var table: TableGeometry = TableGeometry(0f, 0f, 1080f, 1920f)
    var cueBall: Ball? = null
    var objectBalls: MutableList<Ball> = mutableListOf()
    var aimPoint: Pair<Float, Float>? = null
    var predictedSegments: FloatArray = floatArrayOf()

    var cueRadius: Float = 18f
    var maxBounces: Int = 3

    var onBallPlaced: ((Float, Float) -> Unit)? = null

    private var dragMode = DragMode.NONE
    private var dragBall: Ball? = null

    enum class DragMode { NONE, CUE, OBJECT, AIM }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (predictedSegments.size >= 4) {
            path.reset()
            var i = 0
            while (i + 3 < predictedSegments.size) {
                path.moveTo(predictedSegments[i], predictedSegments[i + 1])
                path.lineTo(predictedSegments[i + 2], predictedSegments[i + 3])
                i += 4
            }
            canvas.drawPath(path, segmentPaint)
        }

        cueBall?.let {
            canvas.drawCircle(it.x, it.y, it.radius, cuePaint)
            canvas.drawCircle(it.x, it.y, it.radius, objectPaint)
        }

        objectBalls.forEach {
            if (it.active) canvas.drawCircle(it.x, it.y, it.radius, objectPaint)
        }

        aimPoint?.let { (ax, ay) ->
            canvas.drawCircle(ax, ay, 8f, cuePaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val cue = cueBall
                if (cue != null && dist(x, y, cue.x, cue.y) < cue.radius * 2f) {
                    dragMode = DragMode.CUE
                    dragBall = cue
                    return true
                }
                val hit = objectBalls.firstOrNull { it.active && dist(x, y, it.x, it.y) < it.radius * 2f }
                if (hit != null) {
                    dragMode = DragMode.OBJECT
                    dragBall = hit
                    return true
                }
                if (table.contains(x, y)) {
                    dragMode = DragMode.AIM
                    aimPoint = x to y
                    onBallPlaced?.invoke(x, y)
                    recalcPrediction()
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                when (dragMode) {
                    DragMode.CUE -> dragBall?.let { it.x = x; it.y = y; recalcPrediction(); invalidate() }
                    DragMode.OBJECT -> dragBall?.let { it.x = x; it.y = y; recalcPrediction(); invalidate() }
                    DragMode.AIM -> {
                        aimPoint = x to y
                        recalcPrediction()
                        invalidate()
                    }
                    else -> {}
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragMode = DragMode.NONE
                dragBall = null
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun recalcPrediction() {
        val cue = cueBall ?: return
        val aim = aimPoint ?: return
        val aimDirX = aim.first - cue.x
        val aimDirY = aim.second - cue.y
        val ballsFlat = ArrayList<Float>()
        objectBalls.forEach { if (it.active) ballsFlat.addAll(it.toFlat().toList()) }
        predictedSegments = NativeBridge.predictTrajectory(
            cue.x, cue.y, aimDirX, aimDirY,
            ballsFlat.toFloatArray(),
            table.left, table.top, table.right, table.bottom,
            cueRadius, maxBounces
        )
    }

    private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}