package com.poseghost.app.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.poseghost.app.analyzer.LivePoseDetector
import com.poseghost.app.model.DetectedSkeleton
import com.poseghost.app.model.GridMode
import com.poseghost.app.model.LightingDirection
import com.poseghost.app.model.NormPoint
import com.poseghost.app.model.PoseGuide
import com.poseghost.app.model.SceneAnalysisResult
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Full-screen semi-transparent composition, ghost pose silhouette (built-in vector OR custom PNG/JPG),
 * and live ML Kit skeleton overlay rendered directly over CameraX preview.
 */
@Composable
fun PoseOverlayCanvas(
    pose: PoseGuide?,
    scene: SceneAnalysisResult,
    detectedSkeleton: DetectedSkeleton,
    showSkeleton: Boolean,
    alignmentScore: Int,
    opacity: Float,
    tintColor: Color,
    gridMode: GridMode,
    flipHorizontal: Boolean,
    scale: Float,
    offset: Offset,
    showHorizonCue: Boolean,
    onTransformChange: (newScale: Float, newOffset: Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val customBitmap: ImageBitmap? = remember(pose?.customImagePath) {
        val path = pose?.customImagePath ?: return@remember null
        try {
            BitmapFactory.decodeFile(path)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(scale, offset) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val updatedScale = (scale * zoom).coerceIn(0.45f, 2.1f)
                    val updatedOffset = Offset(
                        x = (offset.x + pan.x).coerceIn(-size.width * 0.42f, size.width * 0.42f),
                        y = (offset.y + pan.y).coerceIn(-size.height * 0.38f, size.height * 0.38f)
                    )
                    onTransformChange(updatedScale, updatedOffset)
                }
            }
    ) {
        // 1. Composition Grid (Rule of Thirds / Golden Ratio / Diagonals)
        drawCompositionGrid(gridMode)

        // 2. Subtle Horizon & Light Direction Indicators
        if (showHorizonCue) {
            drawHorizonIndicator(scene.horizonRatio)
            drawLightDirectionCue(scene.lightingDirection)
        }

        // 3. Semi-Transparent Ghost Pose Silhouette (Custom PNG/JPG or Built-in Vector)
        if (pose != null && opacity > 0.01f) {
            val anchorShiftX = if (pose.isCustom) {
                0f
            } else {
                (scene.suggestedAnchor.normalizedX() - 0.5f) * size.width * 0.45f
            }

            if (customBitmap != null) {
                drawCustomSilhouetteBitmap(
                    bitmap = customBitmap,
                    opacity = opacity,
                    tintColor = if (pose.isEdgeExtracted) tintColor else null,
                    flipHorizontal = flipHorizontal,
                    scale = scale,
                    offset = Offset(offset.x + anchorShiftX, offset.y)
                )
            } else {
                drawPoseSilhouette(
                    pose = pose,
                    opacity = opacity,
                    tintColor = tintColor,
                    flipHorizontal = flipHorizontal,
                    scale = scale * pose.defaultScale,
                    offset = Offset(offset.x + anchorShiftX, offset.y)
                )
            }
        }

        // 4. Real-Time ML Kit Detected Human Skeleton
        if (showSkeleton && detectedSkeleton.hasPerson) {
            drawLiveSkeleton(detectedSkeleton, alignmentScore)
        }
    }
}

@Composable
fun PoseMiniPreview(
    pose: PoseGuide,
    tintColor: Color,
    flipHorizontal: Boolean,
    modifier: Modifier = Modifier
) {
    val customBitmap: ImageBitmap? = remember(pose.customImagePath) {
        val path = pose.customImagePath ?: return@remember null
        try {
            BitmapFactory.decodeFile(path)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    if (customBitmap != null) {
        Image(
            bitmap = customBitmap,
            contentDescription = pose.titleVi,
            contentScale = ContentScale.Fit,
            colorFilter = if (pose.isEdgeExtracted) ColorFilter.tint(tintColor) else null,
            modifier = modifier
        )
    } else {
        Canvas(modifier = modifier) {
            drawPoseSilhouette(
                pose = pose,
                opacity = 0.9f,
                tintColor = tintColor,
                flipHorizontal = flipHorizontal,
                scale = 1.0f,
                offset = Offset.Zero,
                isThumbnail = true
            )
        }
    }
}

private fun DrawScope.drawCustomSilhouetteBitmap(
    bitmap: ImageBitmap,
    opacity: Float,
    tintColor: Color?,
    flipHorizontal: Boolean,
    scale: Float,
    offset: Offset
) {
    val maxW = size.width * 0.88f * scale
    val maxH = size.height * 0.72f * scale
    val aspect = bitmap.width.toFloat() / bitmap.height.toFloat().coerceAtLeast(1f)

    val drawW: Float
    val drawH: Float
    if (maxW / maxH > aspect) {
        drawH = maxH
        drawW = maxH * aspect
    } else {
        drawW = maxW
        drawH = maxW / aspect
    }

    val centerX = size.width * 0.5f + offset.x
    val centerY = size.height * 0.5f + offset.y
    val left = (centerX - drawW * 0.5f).roundToInt()
    val top = (centerY - drawH * 0.5f).roundToInt()

    withTransform({
        if (flipHorizontal) {
            scale(scaleX = -1f, scaleY = 1f, pivot = Offset(centerX, centerY))
        }
    }) {
        drawImage(
            image = bitmap,
            dstOffset = IntOffset(left, top),
            dstSize = IntSize(drawW.roundToInt().coerceAtLeast(1), drawH.roundToInt().coerceAtLeast(1)),
            alpha = opacity,
            colorFilter = tintColor?.let { ColorFilter.tint(it) }
        )
    }
}

private fun DrawScope.drawLiveSkeleton(
    skeleton: DetectedSkeleton,
    alignmentScore: Int
) {
    val isMatched = alignmentScore >= 80
    val boneColor = if (isMatched) {
        Color(0xFF00E676).copy(alpha = 0.85f)
    } else {
        Color(0xFF40C4FF).copy(alpha = 0.78f)
    }
    val jointColor = if (isMatched) Color(0xFFB9F6CA) else Color.White

    LivePoseDetector.BONE_CONNECTIONS.forEach { (startType, endType) ->
        val a = skeleton.landmarks[startType] ?: return@forEach
        val b = skeleton.landmarks[endType] ?: return@forEach
        drawLine(
            color = boneColor,
            start = Offset(a.x * size.width, a.y * size.height),
            end = Offset(b.x * size.width, b.y * size.height),
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )
    }

    skeleton.landmarks.values.forEach { lm ->
        val pt = Offset(lm.x * size.width, lm.y * size.height)
        drawCircle(color = boneColor, radius = 9f, center = pt)
        drawCircle(color = jointColor, radius = 4.5f, center = pt)
    }
}

private fun DrawScope.drawCompositionGrid(mode: GridMode) {
    if (mode == GridMode.NONE) return
    val gridColor = Color.White.copy(alpha = 0.22f)
    val strokePx = 1.5f

    when (mode) {
        GridMode.RULE_OF_THIRDS -> {
            val x1 = size.width / 3f
            val x2 = size.width * 2f / 3f
            val y1 = size.height / 3f
            val y2 = size.height * 2f / 3f
            drawLine(gridColor, Offset(x1, 0f), Offset(x1, size.height), strokePx)
            drawLine(gridColor, Offset(x2, 0f), Offset(x2, size.height), strokePx)
            drawLine(gridColor, Offset(0f, y1), Offset(size.width, y1), strokePx)
            drawLine(gridColor, Offset(0f, y2), Offset(size.width, y2), strokePx)

            val dotColor = Color.White.copy(alpha = 0.38f)
            listOf(Offset(x1, y1), Offset(x2, y1), Offset(x1, y2), Offset(x2, y2)).forEach { pt ->
                drawCircle(dotColor, radius = 5f, center = pt)
            }
        }
        GridMode.GOLDEN_RATIO -> {
            val x1 = size.width * 0.382f
            val x2 = size.width * 0.618f
            val y1 = size.height * 0.382f
            val y2 = size.height * 0.618f
            drawLine(gridColor, Offset(x1, 0f), Offset(x1, size.height), strokePx)
            drawLine(gridColor, Offset(x2, 0f), Offset(x2, size.height), strokePx)
            drawLine(gridColor, Offset(0f, y1), Offset(size.width, y1), strokePx)
            drawLine(gridColor, Offset(0f, y2), Offset(size.width, y2), strokePx)
        }
        GridMode.DIAGONALS -> {
            drawLine(gridColor, Offset(0f, 0f), Offset(size.width, size.height), strokePx)
            drawLine(gridColor, Offset(size.width, 0f), Offset(0f, size.height), strokePx)
            drawLine(
                gridColor.copy(alpha = 0.14f),
                Offset(size.width / 2f, 0f),
                Offset(size.width / 2f, size.height),
                strokePx
            )
        }
        GridMode.NONE -> Unit
    }
}

private fun DrawScope.drawHorizonIndicator(horizonRatio: Float) {
    val y = (size.height * horizonRatio).coerceIn(size.height * 0.18f, size.height * 0.82f)
    val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
    drawLine(
        color = Color(0xFFFFD54F).copy(alpha = 0.32f),
        start = Offset(size.width * 0.08f, y),
        end = Offset(size.width * 0.92f, y),
        strokeWidth = 2f,
        pathEffect = dash
    )
}

private fun DrawScope.drawLightDirectionCue(direction: LightingDirection) {
    val sunColor = Color(0xFFFFD54F).copy(alpha = 0.55f)
    val cx = size.width * 0.5f
    val cy = size.height * 0.22f
    val r = 14f

    val origin = when (direction) {
        LightingDirection.SIDE_LEFT -> Offset(size.width * 0.08f, size.height * 0.34f)
        LightingDirection.SIDE_RIGHT -> Offset(size.width * 0.92f, size.height * 0.34f)
        LightingDirection.TOP_HARSH -> Offset(cx, size.height * 0.14f)
        LightingDirection.BACKLIT -> Offset(cx, size.height * 0.18f)
        LightingDirection.FRONT_SOFT, LightingDirection.LOW_LIGHT -> return
    }

    drawCircle(color = sunColor, radius = r, center = origin, style = Stroke(width = 3f))
    drawCircle(color = sunColor.copy(alpha = 0.25f), radius = r * 1.75f, center = origin)

    val target = Offset(cx, cy + size.height * 0.14f)
    val dx = target.x - origin.x
    val dy = target.y - origin.y
    val dist = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
    val rayEnd = Offset(origin.x + (dx / dist) * 56f, origin.y + (dy / dist) * 56f)
    drawLine(
        color = sunColor,
        start = Offset(origin.x + (dx / dist) * (r + 8f), origin.y + (dy / dist) * (r + 8f)),
        end = rayEnd,
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawPoseSilhouette(
    pose: PoseGuide,
    opacity: Float,
    tintColor: Color,
    flipHorizontal: Boolean,
    scale: Float,
    offset: Offset,
    isThumbnail: Boolean = false
) {
    val boxSize = if (isThumbnail) {
        min(size.width, size.height) * 0.92f
    } else {
        min(size.width * 0.92f, size.height * 0.68f) * scale
    }
    val originX = (size.width - boxSize) / 2f + offset.x
    val originY = (size.height - boxSize) / 2f + offset.y

    fun mapPoint(p: NormPoint): Offset {
        val nx = if (flipHorizontal) 1f - p.x else p.x
        return Offset(originX + nx * boxSize, originY + p.y * boxSize)
    }

    val baseStroke = if (isThumbnail) 2.5f else 5.5f
    val haloStroke = if (isThumbnail) 0f else 13f
    val mainColor = tintColor.copy(alpha = opacity)
    val haloColor = Color.Black.copy(alpha = (opacity * 0.42f).coerceIn(0f, 0.35f))
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)

    pose.strokes.forEach { stroke ->
        if (stroke.points.size < 2) return@forEach
        val path = Path().apply {
            val first = mapPoint(stroke.points.first())
            moveTo(first.x, first.y)
            for (i in 1 until stroke.points.size) {
                val pt = mapPoint(stroke.points[i])
                lineTo(pt.x, pt.y)
            }
        }
        val width = baseStroke * stroke.strokeWeight
        val effect = if (stroke.isDashed) dashEffect else null

        if (!isThumbnail && !stroke.isDashed) {
            drawPath(
                path = path,
                color = haloColor,
                style = Stroke(
                    width = haloStroke * stroke.strokeWeight,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
        drawPath(
            path = path,
            color = if (stroke.isDashed) mainColor.copy(alpha = opacity * 0.6f) else mainColor,
            style = Stroke(
                width = width,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                pathEffect = effect
            )
        )
    }

    pose.heads.forEach { head ->
        val c = mapPoint(head.center)
        val r = head.radius * boxSize

        if (!isThumbnail) {
            drawCircle(color = haloColor, radius = r, center = c, style = Stroke(width = haloStroke))
        }
        drawCircle(color = mainColor.copy(alpha = opacity * 0.14f), radius = r, center = c)
        drawCircle(color = mainColor, radius = r, center = c, style = Stroke(width = baseStroke))

        if (!isThumbnail) {
            val angleDeg = if (flipHorizontal) 180f - head.gazeAngleDeg else head.gazeAngleDeg
            val rad = Math.toRadians(angleDeg.toDouble())
            val start = Offset(
                c.x + (cos(rad) * (r * 0.65f)).toFloat(),
                c.y + (sin(rad) * (r * 0.65f)).toFloat()
            )
            val end = Offset(
                c.x + (cos(rad) * (r * 1.55f)).toFloat(),
                c.y + (sin(rad) * (r * 1.55f)).toFloat()
            )
            drawLine(
                color = Color(0xFFFFD54F).copy(alpha = (opacity * 1.15f).coerceAtMost(0.95f)),
                start = start,
                end = end,
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
        }
    }
}
