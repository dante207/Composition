package com.poseghost.app.analyzer

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.poseghost.app.model.BackgroundType
import com.poseghost.app.model.CompositionAnchor
import com.poseghost.app.model.DetectedSkeleton
import com.poseghost.app.model.ForegroundType
import com.poseghost.app.model.LightingDirection
import com.poseghost.app.model.SceneAnalysisResult
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Dual-pipeline real-time CameraX analyzer:
 * 1. Fast Y-plane luminance analysis (~3 FPS) for lighting direction, horizon, background negative space,
 *    and foreground framing.
 * 2. Optional on-device ML Kit Pose Detection (~10 FPS) for real-time skeleton tracking & alignment feedback.
 */
class SceneCompositionAnalyzer(
    private val isFrontCamera: () -> Boolean,
    private val isSkeletonEnabled: () -> Boolean,
    private val poseDetector: LivePoseDetector,
    private val onSceneResult: (SceneAnalysisResult) -> Unit,
    private val onSkeletonResult: (DetectedSkeleton) -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        private const val SCENE_INTERVAL_MS = 350L
        private const val POSE_INTERVAL_MS = 90L
        private const val GRID_W = 36
        private const val GRID_H = 48
        private const val EMA_ALPHA = 0.35f
    }

    private var lastSceneTimestamp = 0L
    private var lastPoseTimestamp = 0L

    private var smoothAvgLuma = 128f
    private var smoothLeftLuma = 128f
    private var smoothRightLuma = 128f
    private var smoothTopLuma = 128f
    private var smoothBottomLuma = 128f
    private var smoothCenterLuma = 128f
    private var smoothRimLuma = 128f
    private var smoothLeftEdge = 0.2f
    private var smoothRightEdge = 0.2f
    private var smoothUpperEdge = 0.2f
    private var smoothLowerEdge = 0.2f
    private var smoothSideFrameEdge = 0.2f
    private var smoothHorizonRatio = 0.5f

    private val grid = FloatArray(GRID_W * GRID_H)

    override fun analyze(image: ImageProxy) {
        val now = System.currentTimeMillis()
        val runScene = (now - lastSceneTimestamp) >= SCENE_INTERVAL_MS
        val runPose = isSkeletonEnabled() && (now - lastPoseTimestamp) >= POSE_INTERVAL_MS

        if (!runScene && !runPose) {
            image.close()
            return
        }

        if (runScene) {
            lastSceneTimestamp = now
            analyzeSceneLuminance(image)
        }

        if (runPose) {
            lastPoseTimestamp = now
            poseDetector.processImageProxy(
                imageProxy = image,
                isFrontCamera = isFrontCamera(),
                onResult = onSkeletonResult,
                onComplete = { image.close() }
            )
        } else {
            image.close()
        }
    }

    private fun analyzeSceneLuminance(image: ImageProxy) {
        val yPlane = image.planes[0]
        val buffer = yPlane.buffer
        val rowStride = yPlane.rowStride
        val pixelStride = yPlane.pixelStride
        val srcW = image.width
        val srcH = image.height
        val rotation = image.imageInfo.rotationDegrees
        val mirrorX = isFrontCamera()

        for (gy in 0 until GRID_H) {
            val ny = (gy + 0.5f) / GRID_H
            for (gx in 0 until GRID_W) {
                val nxRaw = (gx + 0.5f) / GRID_W
                val nx = if (mirrorX) 1f - nxRaw else nxRaw

                val (sxNorm, syNorm) = when (rotation) {
                    90 -> ny to (1f - nx)
                    180 -> (1f - nx) to (1f - ny)
                    270 -> (1f - ny) to nx
                    else -> nx to ny
                }

                val sx = min(srcW - 1, max(0, (sxNorm * srcW).toInt()))
                val sy = min(srcH - 1, max(0, (syNorm * srcH).toInt()))
                val index = sy * rowStride + sx * pixelStride
                val luma = if (index in 0 until buffer.limit()) {
                    (buffer.get(index).toInt() and 0xFF).toFloat()
                } else {
                    128f
                }
                grid[gy * GRID_W + gx] = luma
            }
        }

        onSceneResult(computeCompositionMetrics())
    }

    private fun computeCompositionMetrics(): SceneAnalysisResult {
        var totalLuma = 0f
        var leftLuma = 0f; var leftCount = 0
        var rightLuma = 0f; var rightCount = 0
        var topLuma = 0f; var topCount = 0
        var bottomLuma = 0f; var bottomCount = 0
        var centerLuma = 0f; var centerCount = 0
        var rimLuma = 0f; var rimCount = 0
        var minLuma = 255f
        var maxLuma = 0f

        var leftEdgeSum = 0f; var leftEdgeCount = 0
        var rightEdgeSum = 0f; var rightEdgeCount = 0
        var upperEdgeSum = 0f; var upperEdgeCount = 0
        var lowerEdgeSum = 0f; var lowerEdgeCount = 0
        var sideFrameDiffSum = 0f; var sideFrameCount = 0

        val rowAvgLuma = FloatArray(GRID_H)
        val rowEdgeAvg = FloatArray(GRID_H)

        for (y in 0 until GRID_H) {
            var rowSum = 0f
            var rowEdge = 0f
            for (x in 0 until GRID_W) {
                val v = grid[y * GRID_W + x]
                totalLuma += v
                rowSum += v
                if (v < minLuma) minLuma = v
                if (v > maxLuma) maxLuma = v

                val thirdX = x * 3 / GRID_W
                val thirdY = y * 3 / GRID_H

                if (thirdX == 0) { leftLuma += v; leftCount++ }
                if (thirdX == 2) { rightLuma += v; rightCount++ }
                if (thirdY == 0) { topLuma += v; topCount++ }
                if (thirdY == 2) { bottomLuma += v; bottomCount++ }

                val inCenter = x in (GRID_W / 4)..(GRID_W * 3 / 4) && y in (GRID_H / 4)..(GRID_H * 3 / 4)
                if (inCenter) {
                    centerLuma += v; centerCount++
                } else {
                    rimLuma += v; rimCount++
                }

                if (x in 1 until (GRID_W - 1) && y in 1 until (GRID_H - 1)) {
                    val dx = abs(grid[y * GRID_W + (x + 1)] - grid[y * GRID_W + (x - 1)])
                    val dy = abs(grid[(y + 1) * GRID_W + x] - grid[(y - 1) * GRID_W + x])
                    val grad = min(1f, (dx + dy) / 140f)
                    rowEdge += grad

                    if (x < GRID_W / 2) {
                        leftEdgeSum += grad; leftEdgeCount++
                    } else {
                        rightEdgeSum += grad; rightEdgeCount++
                    }

                    if (y < GRID_H / 2) {
                        upperEdgeSum += grad; upperEdgeCount++
                    } else {
                        lowerEdgeSum += grad; lowerEdgeCount++
                    }

                    if (x < GRID_W * 0.16f || x > GRID_W * 0.84f) {
                        sideFrameDiffSum += grad
                        sideFrameCount++
                    }
                }
            }
            rowAvgLuma[y] = rowSum / GRID_W
            rowEdgeAvg[y] = rowEdge / GRID_W
        }

        val count = (GRID_W * GRID_H).toFloat()
        val avgLuma = totalLuma / count
        val lLuma = leftLuma / max(1, leftCount)
        val rLuma = rightLuma / max(1, rightCount)
        val tLuma = topLuma / max(1, topCount)
        val bLuma = bottomLuma / max(1, bottomCount)
        val cLuma = centerLuma / max(1, centerCount)
        val oLuma = rimLuma / max(1, rimCount)

        val lEdge = leftEdgeSum / max(1, leftEdgeCount)
        val rEdge = rightEdgeSum / max(1, rightEdgeCount)
        val uEdge = upperEdgeSum / max(1, upperEdgeCount)
        val dEdge = lowerEdgeSum / max(1, lowerEdgeCount)
        val frameEdge = sideFrameDiffSum / max(1, sideFrameCount)

        var bestHorizonRow = GRID_H / 2
        var bestTransition = 0f
        for (y in (GRID_H * 0.2f).toInt()..(GRID_H * 0.8f).toInt()) {
            val above = (rowAvgLuma[y - 2] + rowAvgLuma[y - 1]) * 0.5f
            val below = (rowAvgLuma[y + 1] + rowAvgLuma[y + 2]) * 0.5f
            val lumaJump = abs(above - below) / 255f
            val score = lumaJump * 0.7f + rowEdgeAvg[y] * 0.3f
            if (score > bestTransition) {
                bestTransition = score
                bestHorizonRow = y
            }
        }
        val rawHorizonRatio = bestHorizonRow.toFloat() / GRID_H

        smoothAvgLuma = ema(smoothAvgLuma, avgLuma)
        smoothLeftLuma = ema(smoothLeftLuma, lLuma)
        smoothRightLuma = ema(smoothRightLuma, rLuma)
        smoothTopLuma = ema(smoothTopLuma, tLuma)
        smoothBottomLuma = ema(smoothBottomLuma, bLuma)
        smoothCenterLuma = ema(smoothCenterLuma, cLuma)
        smoothRimLuma = ema(smoothRimLuma, oLuma)
        smoothLeftEdge = ema(smoothLeftEdge, lEdge)
        smoothRightEdge = ema(smoothRightEdge, rEdge)
        smoothUpperEdge = ema(smoothUpperEdge, uEdge)
        smoothLowerEdge = ema(smoothLowerEdge, dEdge)
        smoothSideFrameEdge = ema(smoothSideFrameEdge, frameEdge)
        smoothHorizonRatio = ema(smoothHorizonRatio, rawHorizonRatio)

        val contrastRatio = ((maxLuma - minLuma) / 255f).coerceIn(0f, 1f)

        val sideDelta = smoothLeftLuma - smoothRightLuma
        val topBottomDelta = smoothTopLuma - smoothBottomLuma
        val rimCenterDelta = smoothRimLuma - smoothCenterLuma

        val lighting = when {
            smoothAvgLuma < 58f -> LightingDirection.LOW_LIGHT
            (rimCenterDelta > 22f && smoothTopLuma > 145f) ||
                (smoothTopLuma > 185f && smoothCenterLuma < 115f) -> LightingDirection.BACKLIT
            sideDelta > 18f -> LightingDirection.SIDE_LEFT
            sideDelta < -18f -> LightingDirection.SIDE_RIGHT
            topBottomDelta > 36f && smoothTopLuma > 165f -> LightingDirection.TOP_HARSH
            else -> LightingDirection.FRONT_SOFT
        }

        val totalEdge = (smoothUpperEdge + smoothLowerEdge) * 0.5f
        val background = when {
            smoothUpperEdge < 0.16f && smoothTopLuma > smoothBottomLuma + 14f && smoothHorizonRatio >= 0.42f ->
                BackgroundType.OPEN_LANDSCAPE
            smoothHorizonRatio < 0.38f && smoothLowerEdge > 0.18f ->
                BackgroundType.HIGH_HORIZON
            totalEdge < 0.19f ->
                BackgroundType.MINIMAL_CLEAN
            else ->
                BackgroundType.BUSY_URBAN
        }

        val outerDarkFrame = (smoothLeftLuma < smoothCenterLuma - 18f || smoothRightLuma < smoothCenterLuma - 18f)
        val foreground = when {
            outerDarkFrame || smoothSideFrameEdge > (totalEdge + 0.08f) ->
                ForegroundType.FRAMING_EDGES
            smoothLowerEdge > smoothUpperEdge + 0.09f ->
                ForegroundType.LEADING_GROUND
            else ->
                ForegroundType.CLEAN_OPEN
        }

        val negLeft = (1f - smoothLeftEdge * 2.2f).coerceIn(0f, 1f)
        val negRight = (1f - smoothRightEdge * 2.2f).coerceIn(0f, 1f)

        val anchor = when {
            negRight - negLeft > 0.12f -> CompositionAnchor.LEFT_THIRD
            negLeft - negRight > 0.12f -> CompositionAnchor.RIGHT_THIRD
            lighting == LightingDirection.SIDE_LEFT -> CompositionAnchor.RIGHT_THIRD
            lighting == LightingDirection.SIDE_RIGHT -> CompositionAnchor.LEFT_THIRD
            else -> CompositionAnchor.CENTER
        }

        val suggestFlip = when (lighting) {
            LightingDirection.SIDE_LEFT -> true
            LightingDirection.SIDE_RIGHT -> false
            else -> anchor == CompositionAnchor.RIGHT_THIRD
        }

        return SceneAnalysisResult(
            lightingDirection = lighting,
            backgroundType = background,
            foregroundType = foreground,
            suggestedAnchor = anchor,
            horizonRatio = smoothHorizonRatio,
            averageLuma = smoothAvgLuma,
            contrastRatio = contrastRatio,
            negativeSpaceLeft = negLeft,
            negativeSpaceRight = negRight,
            suggestFlipHorizontal = suggestFlip
        )
    }

    private fun ema(prev: Float, current: Float): Float = prev * (1f - EMA_ALPHA) + current * EMA_ALPHA
}
