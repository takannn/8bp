#include "predictor.h"
#include <limits>
#include <algorithm>
#include <cmath>

static float dot(Vec2 a, Vec2 b) { return a.x * b.x + a.y * b.y; }
static Vec2 sub(Vec2 a, Vec2 b) { return {a.x - b.x, a.y - b.y}; }
static Vec2 add(Vec2 a, Vec2 b) { return {a.x + b.x, a.y + b.y}; }
static Vec2 mul(Vec2 a, float s) { return {a.x * s, a.y * s}; }
static float length(Vec2 a) { return std::sqrt(a.x * a.x + a.y * a.y); }
static Vec2 normalize(Vec2 a) {
    float l = length(a);
    if (l < 1e-6f) return {0.0f, 0.0f};
    return {a.x / l, a.y / l};
}

static float rayCircle(Vec2 origin, Vec2 dir, Vec2 center, float radius) {
    Vec2 oc = sub(origin, center);
    float b = dot(oc, dir);
    float c = dot(oc, oc) - radius * radius;
    float disc = b * b - c;
    if (disc < 0.0f) return -1.0f;
    float sqrtDisc = std::sqrt(disc);
    float t1 = -b - sqrtDisc;
    float t2 = -b + sqrtDisc;
    if (t1 > 1e-4f) return t1;
    if (t2 > 1e-4f) return t2;
    return -1.0f;
}

static Vec2 reflectWall(Vec2 dir, int wall) {
    switch (wall) {
        case 0:
        case 1:
            return {-dir.x, dir.y};
        case 2:
        case 3:
            return {dir.x, -dir.y};
        default:
            return dir;
    }
}

static float rayWall(Vec2 origin, Vec2 dir, TableBounds t) {
    float tx = std::numeric_limits<float>::infinity();
    float ty = std::numeric_limits<float>::infinity();

    if (dir.x > 1e-6f) tx = (t.right - origin.x) / dir.x;
    else if (dir.x < -1e-6f) tx = (t.left - origin.x) / dir.x;

    if (dir.y > 1e-6f) ty = (t.bottom - origin.y) / dir.y;
    else if (dir.y < -1e-6f) ty = (t.top - origin.y) / dir.y;

    return std::min(tx, ty);
}

std::vector<float> predictTrajectory(
    Vec2 cuePos,
    Vec2 aimDir,
    const std::vector<BallState>& balls,
    TableBounds table,
    float cueRadius,
    int maxBounces
) {
    std::vector<float> segments;
    Vec2 dir = normalize(aimDir);
    if (length(dir) < 1e-6f) return segments;

    Vec2 origin = cuePos;
    Vec2 currentDir = dir;

    for (int bounce = 0; bounce <= maxBounces; ++bounce) {
        float nearestBallT = std::numeric_limits<float>::infinity();
        int hitBallIdx = -1;

        for (size_t i = 0; i < balls.size(); ++i) {
            if (!balls[i].active) continue;
            float t = rayCircle(origin, currentDir, balls[i].pos, balls[i].radius + cueRadius);
            if (t > 1e-4f && t < nearestBallT) {
                nearestBallT = t;
                hitBallIdx = static_cast<int>(i);
            }
        }

        float wallT = rayWall(origin, currentDir, table);

        if (hitBallIdx >= 0 && nearestBallT < wallT) {
            Vec2 hitPoint = add(origin, mul(currentDir, nearestBallT));
            segments.push_back(origin.x);
            segments.push_back(origin.y);
            segments.push_back(hitPoint.x);
            segments.push_back(hitPoint.y);

            Vec2 objCenter = balls[hitBallIdx].pos;
            Vec2 objDir = normalize(sub(objCenter, hitPoint));

            Vec2 objOrigin = objCenter;
            float objLen = 0.0f;
            float maxObjLen = balls[hitBallIdx].radius * 12.0f;
            while (objLen < maxObjLen) {
                float tw = rayWall(objOrigin, objDir, table);
                if (tw < 1e-3f) break;
                float step = std::min(tw, maxObjLen - objLen);
                Vec2 next = add(objOrigin, mul(objDir, step));
                segments.push_back(objOrigin.x);
                segments.push_back(objOrigin.y);
                segments.push_back(next.x);
                segments.push_back(next.y);
                objOrigin = next;
                objLen += step;
                if (step >= tw - 1e-3f) {
                    if (objOrigin.x <= table.left + 1.0f) objDir = {std::abs(objDir.x), objDir.y};
                    else if (objOrigin.x >= table.right - 1.0f) objDir = {-std::abs(objDir.x), objDir.y};
                    if (objOrigin.y <= table.top + 1.0f) objDir = {objDir.x, std::abs(objDir.y)};
                    else if (objOrigin.y >= table.bottom - 1.0f) objDir = {objDir.x, -std::abs(objDir.y)};
                    objDir = normalize(objDir);
                }
            }
            return segments;
        }

        if (wallT < 1e-3f) {
            return segments;
        }

        Vec2 wallHit = add(origin, mul(currentDir, wallT));
        segments.push_back(origin.x);
        segments.push_back(origin.y);
        segments.push_back(wallHit.x);
        segments.push_back(wallHit.y);

        int wall = -1;
        if (wallHit.x <= table.left + 1.0f) wall = 0;
        else if (wallHit.x >= table.right - 1.0f) wall = 1;
        else if (wallHit.y <= table.top + 1.0f) wall = 2;
        else if (wallHit.y >= table.bottom - 1.0f) wall = 3;

        if (wall < 0) break;
        currentDir = normalize(reflectWall(currentDir, wall));
        origin = wallHit;
    }

    return segments;
}