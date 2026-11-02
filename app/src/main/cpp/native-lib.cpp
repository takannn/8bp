#include <jni.h>
#include "predictor.h"

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_cancer666_predictor_NativeBridge_predictTrajectory(
    JNIEnv* env,
    jobject /* this */,
    jfloat cueX,
    jfloat cueY,
    jfloat aimX,
    jfloat aimY,
    jfloatArray ballsFlat,
    jfloat tableLeft,
    jfloat tableTop,
    jfloat tableRight,
    jfloat tableBottom,
    jfloat cueRadius,
    jint maxBounces
) {
    std::vector<BallState> balls;
    if (ballsFlat != nullptr) {
        jsize len = env->GetArrayLength(ballsFlat);
        jfloat* data = env->GetFloatArrayElements(ballsFlat, nullptr);
        for (jsize i = 0; i + 3 < len; i += 4) {
            BallState b;
            b.pos = {data[i], data[i + 1]};
            b.radius = data[i + 2];
            b.active = data[i + 3] > 0.5f;
            balls.push_back(b);
        }
        env->ReleaseFloatArrayElements(ballsFlat, data, JNI_ABORT);
    }

    TableBounds table{tableLeft, tableTop, tableRight, tableBottom};
    Vec2 cue{cueX, cueY};
    Vec2 aim{aimX, aimY};

    std::vector<float> result = predictTrajectory(cue, aim, balls, table, cueRadius, maxBounces);

    jfloatArray out = env->NewFloatArray(static_cast<jsize>(result.size()));
    if (out != nullptr && !result.empty()) {
        env->SetFloatArrayRegion(out, 0, static_cast<jsize>(result.size()), result.data());
    }
    return out;
}