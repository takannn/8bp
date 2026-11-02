package com.cancer666.predictor

data class TableGeometry(
    var left: Float,
    var top: Float,
    var right: Float,
    var bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    fun contains(px: Float, py: Float): Boolean =
        px in left..right && py in top..bottom
}