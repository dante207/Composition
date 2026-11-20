package com.poseghost.app.poses

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import com.poseghost.app.model.BackgroundType
import com.poseghost.app.model.CompositionAnchor
import com.poseghost.app.model.ForegroundType
import com.poseghost.app.model.LightingDirection
import com.poseghost.app.model.PoseCategory
import com.poseghost.app.model.PoseGuide
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Manages user-imported custom PNG & JPG silhouette poses stored in app-private storage.
 *
 * Supports two import modes:
 * 1. Auto-Outline Extraction (Sobel edge contour -> white-on-transparent ghost outline): ideal for
 *    regular JPG/PNG reference photos with solid backgrounds.
 * 2. Direct Translucent Overlay: preserves transparent PNG silhouettes or overlays reference photos as-is.
 */
object CustomPoseRepository {

    private const val DIR_NAME = "custom_poses"
    private const val META_FILE = "custom_poses.json"
    private const val MAX_DIM = 960

    private fun poseDir(context: Context): File =
        File(context.filesDir, DIR_NAME).apply { if (!exists()) mkdirs() }

    private fun metaFile(context: Context): File = File(poseDir(context), META_FILE)

    fun loadCustomPoses(context: Context): List<PoseGuide> {
        val meta = metaFile(context)
        if (!meta.exists()) return emptyList()

        return try {
            val arr = JSONArray(meta.readText())
            val list = mutableListOf<PoseGuide>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val path = obj.getString("path")
                if (!File(path).exists()) continue
                val id = obj.getString("id")
                val name = obj.getString("name")
                val edgeExtracted = obj.optBoolean("edgeExtracted", false)
                list += buildCustomPoseGuide(id, name, path, edgeExtracted)
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun importFromUri(
        context: Context,
        uri: Uri,
        displayName: String,
        extractOutline: Boolean
    ): PoseGuide? {
        val rawBitmap = decodeScaledBitmap(context, uri) ?: return null
        val processed = if (extractOutline) {
            extractWhiteOutlineBitmap(rawBitmap)
        } else {
            rawBitmap
        }

        val id = "custom_${System.currentTimeMillis()}"
        val outFile = File(poseDir(context), "$id.png")
        FileOutputStream(outFile).use { fos ->
            processed.compress(Bitmap.CompressFormat.PNG, 95, fos)
        }
        if (processed !== rawBitmap) {
            rawBitmap.recycle()
        }
        processed.recycle()

        val newGuide = buildCustomPoseGuide(id, displayName, outFile.absolutePath, extractOutline)
        val existing = loadCustomPoses(context)
        saveMetadata(context, listOf(newGuide) + existing)
        return newGuide
    }

    fun deleteCustomPose(context: Context, poseId: String): List<PoseGuide> {
        val existing = loadCustomPoses(context)
        val target = existing.firstOrNull { it.id == poseId }
        target?.customImagePath?.let { File(it).delete() }
        val remaining = existing.filterNot { it.id == poseId }
        saveMetadata(context, remaining)
        return remaining
    }

    private fun saveMetadata(context: Context, poses: List<PoseGuide>) {
        val arr = JSONArray()
        poses.forEach { p ->
            val path = p.customImagePath ?: return@forEach
            arr.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("name", p.titleVi)
                    put("path", path)
                    put("edgeExtracted", p.isEdgeExtracted)
                }
            )
        }
        metaFile(context).writeText(arr.toString())
    }

    private fun buildCustomPoseGuide(
        id: String,
        name: String,
        path: String,
        edgeExtracted: Boolean
    ): PoseGuide {
        return PoseGuide(
            id = id,
            titleVi = name,
            titleEn = name,
            category = PoseCategory.CUSTOM,
            tipVi = if (edgeExtracted) {
                "Tư thế tùy chỉnh (đã tách viền silhouette). Dùng 2 ngón phóng to/thu nhỏ hoặc kéo vị trí."
            } else {
                "Tư thế ảnh tùy chỉnh (PNG/JPG). Điều chỉnh thanh độ mờ để căn chỉnh theo ảnh mẫu."
            },
            tipEn = if (edgeExtracted) {
                "Custom silhouette outline. Pinch to zoom or drag to align with your subject."
            } else {
                "Custom PNG/JPG overlay. Adjust opacity slider to match your reference pose."
            },
            idealLighting = LightingDirection.entries.toSet(),
            idealBackgrounds = BackgroundType.entries.toSet(),
            idealForegrounds = ForegroundType.entries.toSet(),
            preferAnchor = CompositionAnchor.CENTER,
            customImagePath = path,
            isEdgeExtracted = edgeExtracted
        )
    }

    private fun decodeScaledBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sampleSize = 1
            while (bounds.outWidth / sampleSize > MAX_DIM * 2 || bounds.outHeight / sampleSize > MAX_DIM * 2) {
                sampleSize *= 2
            }

            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return null

            val maxSide = max(decoded.width, decoded.height)
            if (maxSide <= MAX_DIM) {
                decoded
            } else {
                val scale = MAX_DIM.toFloat() / maxSide
                val scaled = Bitmap.createScaledBitmap(
                    decoded,
                    max(1, (decoded.width * scale).toInt()),
                    max(1, (decoded.height * scale).toInt()),
                    true
                )
                if (scaled !== decoded) decoded.recycle()
                scaled
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Converts an opaque JPG/PNG reference photo into a clean white-on-transparent contour outline
     * using fast 3x3 Sobel gradient magnitude. If the input already has transparency, combines
     * alpha edges with luminance edges.
     */
    private fun extractWhiteOutlineBitmap(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        val luma = IntArray(w * h)
        val alpha = IntArray(w * h)
        for (i in pixels.indices) {
            val c = pixels[i]
            val a = Color.alpha(c)
            alpha[i] = a
            val r = Color.red(c)
            val g = Color.green(c)
            val b = Color.blue(c)
            luma[i] = ((r * 299 + g * 587 + b * 114) / 1000)
        }

        val outPixels = IntArray(w * h)
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val idx = y * w + x

                val gxL = -luma[(y - 1) * w + (x - 1)] + luma[(y - 1) * w + (x + 1)] -
                    2 * luma[y * w + (x - 1)] + 2 * luma[y * w + (x + 1)] -
                    luma[(y + 1) * w + (x - 1)] + luma[(y + 1) * w + (x + 1)]

                val gyL = -luma[(y - 1) * w + (x - 1)] - 2 * luma[(y - 1) * w + x] - luma[(y - 1) * w + (x + 1)] +
                    luma[(y + 1) * w + (x - 1)] + 2 * luma[(y + 1) * w + x] + luma[(y + 1) * w + (x + 1)]

                val gxA = abs(alpha[y * w + (x + 1)] - alpha[y * w + (x - 1)])
                val gyA = abs(alpha[(y + 1) * w + x] - alpha[(y - 1) * w + x])

                val mag = max(abs(gxL) + abs(gyL), (gxA + gyA) * 2)
                if (mag > 95) {
                    val outAlpha = min(255, ((mag - 95) * 1.4f).toInt())
                    outPixels[idx] = Color.argb(outAlpha, 255, 255, 255)
                } else {
                    outPixels[idx] = Color.TRANSPARENT
                }
            }
        }

        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            setPixels(outPixels, 0, w, 0, 0, w, h)
        }
    }
}
