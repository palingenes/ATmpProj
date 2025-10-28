package com.cymf.keyshot.utils

import kotlin.math.acos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class TouchPoint(val x: Float, val y: Float, val timeMs: Long)
data class SampledStep(val x: Int, val y: Int, val delayAfterMs: Long)

object TouchSampling {

    fun optimizePath(
        sequence: OverlayManager.TouchSequence,
        epsilon: Float = 10f,
        maxPoints: Int = 10
    ): List<SampledStep> {
        val points = sequence.getPointList().map {
            OverlayManager.TouchSequence.Point(
                it.x,
                it.y,
                it.timeOffset, it.action
            )
        }
            .filter { it.x >= 0 && it.y >= 0 } // 过滤非法坐标
            .map { TouchPoint(it.x, it.y, it.timeOffset) }

        if (points.size <= 2) {
            return points.map { p -> SampledStep(p.x.toInt(), p.y.toInt(), 50) }
        }

        val keyIndices = douglasPeucker(points, 0, points.size - 1, epsilon)
            .toMutableSet().apply {
                add(0)
                add(points.size - 1)
            }

        if (keyIndices.size > maxPoints) {
            val sorted = keyIndices.toList().sortedByDescending { idx ->
                if (idx == 0 || idx == points.size - 1) Float.MAX_VALUE
                else curvatureScore(points, idx)
            }
            repeat(keyIndices.size - maxPoints) {
                keyIndices.remove(sorted.reversed()[it])
            }
        }

        val result = mutableListOf<SampledStep>()
        val sortedKeys = keyIndices.sorted()

        for (i in sortedKeys.indices) {
            val idx = sortedKeys[i]
            val p = points[idx]
            val delay = if (i < sortedKeys.size - 1) {
                val nextIdx = sortedKeys[i + 1]
                maxOf(30L, points[nextIdx].timeMs - p.timeMs)
            } else 0L

            // 裁剪坐标到常见屏幕范围（可动态获取）
            val clampedX = p.x.coerceIn(0f, 1440f).toInt()
            val clampedY = p.y.coerceIn(0f, 3120f).toInt()

            result.add(SampledStep(clampedX, clampedY, delay))
        }

        return result
    }

    private fun douglasPeucker(
        points: List<TouchPoint>,
        start: Int,
        end: Int,
        epsilon: Float
    ): Set<Int> {
        val result = mutableSetOf<Int>()
        if (end - start <= 1) return result

        var maxDistSq = 0f
        var maxIndex = start

        val a = points[start]
        val b = points[end]

        for (i in start + 1 until end) {
            val d = distanceToLineSquared(points[i], a, b)
            if (d > maxDistSq) {
                maxDistSq = d
                maxIndex = i
            }
        }

        if (maxDistSq > epsilon * epsilon) {
            result += maxIndex
            result += douglasPeucker(points, start, maxIndex, epsilon)
            result += douglasPeucker(points, maxIndex, end, epsilon)
        }

        return result
    }

    private fun distanceToLineSquared(p: TouchPoint, p1: TouchPoint, p2: TouchPoint): Float {
        val a = p.x - p1.x
        val b = p.y - p1.y
        val c = p2.x - p1.x
        val d = p2.y - p1.y

        val dot = a * c + b * d
        val lenSq = c * c + d * d
        val param = if (lenSq == 0f) -1f else dot / lenSq

        val xx = when {
            param < 0 -> p1.x
            param > 1 -> p2.x
            else -> p1.x + param * c
        }
        val yy = when {
            param < 0 -> p1.y
            param > 1 -> p2.y
            else -> p1.y + param * d
        }

        val dx = p.x - xx
        val dy = p.y - yy
        return dx * dx + dy * dy
    }

    private fun curvatureScore(points: List<TouchPoint>, idx: Int): Float {
        if (idx <= 1 || idx >= points.size - 1) return 0f
        val p0 = points[idx - 1]
        val p1 = points[idx]
        val p2 = points[idx + 1]

        val v1x = p1.x - p0.x
        val v1y = p1.y - p0.y
        val v2x = p2.x - p1.x
        val v2y = p2.y - p1.y

        val mag1 = sqrt(v1x * v1x + v1y * v1y)
        val mag2 = sqrt(v2x * v2x + v2y * v2y)
        if (mag1 == 0f || mag2 == 0f) return 0f

        val dot = v1x * v2x + v1y * v2y
        val cosTheta = dot / (mag1 * mag2)
        return acos(min(max(cosTheta, -1f), 1f))
    }
}