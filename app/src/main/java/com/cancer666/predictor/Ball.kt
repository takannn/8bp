package com.cancer666.predictor

data class Ball(
    var x: Float,
    var y: Float,
    var radius: Float,
    var active: Boolean = true
) {
    fun toFlat(): FloatArray = floatArrayOf(x, y, radius, if (active) 1f else 0f)
}