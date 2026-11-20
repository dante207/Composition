package com.poseghost.app.analyzer

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import com.poseghost.app.model.DetectedLandmark
import com.poseghost.app.model.DetectedSkeleton
import com.poseghost.app.model.PoseGuide
import kotlin.math.hypot

/**
 * On-device real-time human skeleton detector powered by Google ML Kit Pose Detection (STREAM_MODE).
 * Extracts normalized [0f..1f] portrait landmarks and computes live alignment percentage against
 * the active ghost pose silhouette.
 */
class LivePoseDetector {

    private val detector: PoseDetector = PoseDetection.getClient(
        PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
    )

    companion object {
        /**
         * Standard skeleton bone connections for rendering on Canvas.
         */
        val BONE_CONNECTIONS: List<Pair<Int, Int>> = listOf(
            // Shoulders & Torso
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_HIP to PoseLandmark.RIGHT_HIP,
            // Left Arm
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW,
            PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
            // Right Arm
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW,
            PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST,
            // Left Leg
            PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE,
            PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE,
            // Right Leg
            PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE,
            PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE,
            // Face / Neck anchor
            PoseLandmark.NOSE to PoseLandmark.LEFT_EYE,
            PoseLandmark.NOSE to PoseLandmark.RIGHT_EYE
        )

        /**
         * Computes a 0..100% alignment score comparing the live detected person's head & shoulder
         * geometry against the currently selected PoseGuide.
         */
        fun computeAlignmentScore(
            skeleton: DetectedSkeleton,
            pose: PoseGuide?,
            flipHorizontal: Boolean
        ): Int {
            if (!skeleton.hasPerson || pose == null) return 0
            val nose = skeleton.landmarks[PoseLandmark.NOSE] ?: return 0
            val lShoulder = skeleton.landmarks[PoseLandmark.LEFT_SHOULDER]
            val rShoulder = skeleton.landmarks[PoseLandmark.RIGHT_SHOULDER]

            // If custom image pose without vector head coordinates, evaluate general framing balance
            val targetHead = pose.heads.firstOrNull()?.center
            if (targetHead == null) {
                val distFromCenter = hypot((nose.x - 0.5f).toDouble(), (nose.y - 0.30f).toDouble()).toFloat()
                return ((1f - (distFromCenter / 0.45f)).coerceIn(0.25f, 0.98f) * 100f).toInt()
            }

            // Map target pose head (in box coordinates centered on screen) to screen-like normalized space
            val targetX = if (flipHorizontal) 1f - targetHead.x else targetHead.x
            val targetY = 0.16f + targetHead.y * 0.68f

            val headDist = hypot((nose.x - targetX).toDouble(), (nose.y - targetY).toDouble()).toFloat()
            val headScore = (1f - (headDist / 0.32f)).coerceIn(0f, 1f)

            val shoulderScore = if (lShoulder != null && rShoulder != null) {
                val midX = (lShoulder.x + rShoulder.x) * 0.5f
                val midY = (lShoulder.y + rShoulder.y) * 0.5f
                val expectedMidY = targetY + 0.12f
                val sDist = hypot((midX - targetX).toDouble(), (midY - expectedMidY).toDouble()).toFloat()
                (1f - (sDist / 0.36f)).coerceIn(0f, 1f)
            } else {
                headScore
            }

            return ((headScore * 0.55f + shoulderScore * 0.45f) * 100f).toInt().coerceIn(0, 100)
        }
    }

    @OptIn(ExperimentalGetImage::class)
    fun processImageProxy(
        imageProxy: ImageProxy,
        isFrontCamera: Boolean,
        onResult: (DetectedSkeleton) -> Unit,
        onComplete: () -> Unit
    ) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            onComplete()
            return
        }

        val rotation = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)

        // Upright coordinate space dimensions after rotation
        val uprightWidth = if (rotation == 90 || rotation == 270) imageProxy.height else imageProxy.width
        val uprightHeight = if (rotation == 90 || rotation == 270) imageProxy.width else imageProxy.height

        detector.process(inputImage)
            .addOnSuccessListener { pose ->
                onResult(extractSkeleton(pose, uprightWidth, uprightHeight, isFrontCamera))
            }
            .addOnFailureListener {
                onResult(DetectedSkeleton(emptyMap(), false))
            }
            .addOnCompleteListener {
                onComplete()
            }
    }

    private fun extractSkeleton(
        pose: Pose,
        width: Int,
        height: Int,
        isFrontCamera: Boolean
    ): DetectedSkeleton {
        val allLandmarks = pose.allPoseLandmarks
        if (allLandmarks.isEmpty() || width <= 0 || height <= 0) {
            return DetectedSkeleton(emptyMap(), false)
        }

        val map = HashMap<Int, DetectedLandmark>(allLandmarks.size)
        for (lm in allLandmarks) {
            if (lm.inFrameLikelihood < 0.45f) continue
            val rawX = (lm.position.x / width.toFloat()).coerceIn(0f, 1f)
            val normX = if (isFrontCamera) 1f - rawX else rawX
            val normY = (lm.position.y / height.toFloat()).coerceIn(0f, 1f)
            map[lm.landmarkType] = DetectedLandmark(normX, normY, lm.inFrameLikelihood)
        }

        val hasCoreBody = map.containsKey(PoseLandmark.NOSE) ||
            (map.containsKey(PoseLandmark.LEFT_SHOULDER) && map.containsKey(PoseLandmark.RIGHT_SHOULDER))

        return DetectedSkeleton(landmarks = map, hasPerson = hasCoreBody)
    }

    fun close() {
        detector.close()
    }
}
