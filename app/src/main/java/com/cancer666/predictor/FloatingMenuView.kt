package com.cancer666.predictor

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

class FloatingMenuView(context: Context) : LinearLayout(context) {

    var onToggleOverlay: ((Boolean) -> Unit)? = null
    var onToggleEditMode: ((Boolean) -> Unit)? = null
    var onClearBalls: (() -> Unit)? = null
    var onBouncesChanged: ((Int) -> Unit)? = null
    var onMenuDrag: ((Float, Float) -> Unit)? = null

    private var overlayEnabled = true
    private var editMode = true

    private val toggleBtn: Button
    private val editBtn: Button
    private val clearBtn: Button
    private val bouncesLabel: TextView
    private val bouncesSeek: SeekBar

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(24, 16, 24, 16)
        background = GradientDrawable().apply {
            setColor(Color.argb(230, 20, 20, 24))
            cornerRadius = 24f
            setStroke(2, Color.argb(180, 0, 255, 180))
        }

        val title = TextView(context).apply {
            text = "8BP PREDICTOR"
            setTextColor(Color.argb(255, 0, 255, 180))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 8)
        }
        addView(title)

        toggleBtn = Button(context).apply {
            text = "Overlay: ON"
            textSize = 11f
            setOnClickListener {
                overlayEnabled = !overlayEnabled
                text = if (overlayEnabled) "Overlay: ON" else "Overlay: OFF"
                onToggleOverlay?.invoke(overlayEnabled)
            }
        }
        addView(toggleBtn)

        editBtn = Button(context).apply {
            text = "Edit Balls: ON"
            textSize = 11f
            setOnClickListener {
                editMode = !editMode
                text = if (editMode) "Edit Balls: ON" else "Edit Balls: OFF"
                onToggleEditMode?.invoke(editMode)
            }
        }
        addView(editBtn)

        bouncesLabel = TextView(context).apply {
            text = "Bounces: 3"
            setTextColor(Color.WHITE)
            textSize = 11f
            gravity = Gravity.CENTER
        }
        addView(bouncesLabel)

        bouncesSeek = SeekBar(context).apply {
            max = 6
            progress = 3
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    bouncesLabel.text = "Bounces: $progress"
                    onBouncesChanged?.invoke(progress)
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        addView(bouncesSeek)

        clearBtn = Button(context).apply {
            text = "Clear Balls"
            textSize = 11f
            setOnClickListener { onClearBalls?.invoke() }
        }
        addView(clearBtn)

        setOnTouchListener(object : OnTouchListener {
            var lastX = 0f
            var lastY = 0f
            var dragging = false
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        lastX = event.rawX
                        lastY = event.rawY
                        dragging = true
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (dragging) {
                            val dx = event.rawX - lastX
                            val dy = event.rawY - lastY
                            lastX = event.rawX
                            lastY = event.rawY
                            onMenuDrag?.invoke(dx, dy)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        dragging =