package com.poseghost.app.poses

import com.poseghost.app.model.PoseGuide
import com.poseghost.app.model.RankedPose
import com.poseghost.app.model.SceneAnalysisResult

/**
 * Ranks all available PoseGuides against the real-time SceneAnalysisResult
 * (lighting direction, background/horizon, foreground framing, and negative space).
 */
object PoseRecommendationEngine {

    fun rankPoses(scene: SceneAnalysisResult): List<RankedPose> {
        return PoseLibrary.allPoses.map { pose ->
            scorePose(pose, scene)
        }.sortedByDescending { it.matchScore }
    }

    private fun scorePose(pose: PoseGuide, scene: SceneAnalysisResult): RankedPose {
        var score = 30 // base score
        val reasonsVi = mutableListOf<String>()
        val reasonsEn = mutableListOf<String>()

        // 1. Lighting compatibility (up to +35)
        if (scene.lightingDirection in pose.idealLighting) {
            score += 35
            reasonsVi += "Hợp ${scene.lightingDirection.label(com.poseghost.app.model.AppLanguage.VI).lowercase()}"
            reasonsEn += "Fits ${scene.lightingDirection.label(com.poseghost.app.model.AppLanguage.EN).lowercase()}"
        }

        // 2. Background & Horizon compatibility (up to +22)
        if (scene.backgroundType in pose.idealBackgrounds) {
            score += 22
            reasonsVi += scene.backgroundType.label(com.poseghost.app.model.AppLanguage.VI).lowercase()
            reasonsEn += scene.backgroundType.label(com.poseghost.app.model.AppLanguage.EN).lowercase()
        }

        // 3. Foreground compatibility (up to +13)
        if (scene.foregroundType in pose.idealForegrounds) {
            score += 13
            if (scene.foregroundType != com.poseghost.app.model.ForegroundType.CLEAN_OPEN) {
                reasonsVi += scene.foregroundType.label(com.poseghost.app.model.AppLanguage.VI).lowercase()
                reasonsEn += scene.foregroundType.label(com.poseghost.app.model.AppLanguage.EN).lowercase()
            }
        }

        val finalScore = score.coerceIn(35, 99)
        val reasonVi = if (reasonsVi.isNotEmpty()) {
            reasonsVi.joinToString(" • ")
        } else {
            "Bố cục cân bằng đa dụng"
        }
        val reasonEn = if (reasonsEn.isNotEmpty()) {
            reasonsEn.joinToString(" • ")
        } else {
            "Versatile balanced framing"
        }

        return RankedPose(
            pose = pose,
            matchScore = finalScore,
            reasonVi = reasonVi,
            reasonEn = reasonEn,
            recommendedFlip = scene.suggestFlipHorizontal
        )
    }
}
