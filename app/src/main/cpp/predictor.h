#ifndef PREDICTOR_H
#define PREDICTOR_H

#include <vector>

struct Vec2 {
    float x;
    float y;
};

struct BallState {
    Vec2 pos;
    float radius;
    bool active;
};

struct TableBounds {
    float left;
    float top;
    float right;
    float bottom;
};

std::vector<float> predictTrajectory(
    Vec2 cuePos,
    Vec2 aimDir,
    const std::vector<BallState>& balls,
    TableBounds table,
    float cueRadius,
    int maxBounces
);

#endif