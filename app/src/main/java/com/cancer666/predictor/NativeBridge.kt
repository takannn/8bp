package com.cancer666.predictor

object NativeBridge {
    init {
        System.loadLibrary("predictor")
    }

    external fun predictTrajectory(
        cueX: Float,
        cueY: Float,
        aimX: Float,
        aimY: Float,
        ballsFlat: FloatArray,
        tableLeft: Float,
        tableTop: Float,
        tableRight: Float,
        tableBottom: Float,
        cueRadius: Float,
        maxBounces: Int
    ): FloatArray
}