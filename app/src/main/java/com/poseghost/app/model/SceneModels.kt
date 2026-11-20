package com.poseghost.app.model

enum class AppLanguage { VI, EN }

enum class LightingDirection {
    SIDE_LEFT,
    SIDE_RIGHT,
    BACKLIT,
    TOP_HARSH,
    FRONT_SOFT,
    LOW_LIGHT;

    fun label(lang: AppLanguage): String = when (this) {
        SIDE_LEFT -> if (lang == AppLanguage.VI) "Sáng nghiêng trái" else "Left Side Light"
        SIDE_RIGHT -> if (lang == AppLanguage.VI) "Sáng nghiêng phải" else "Right Side Light"
        BACKLIT -> if (lang == AppLanguage.VI) "Ngược sáng (Rim)" else "Backlit / Rim Light"
        TOP_HARSH -> if (lang == AppLanguage.VI) "Sáng gắt từ trên" else "Harsh Overhead"
        FRONT_SOFT -> if (lang == AppLanguage.VI) "Sáng thuận dịu" else "Soft Front Light"
        LOW_LIGHT -> if (lang == AppLanguage.VI) "Thiếu sáng / Hoàng hôn" else "Low Light / Dusk"
    }
}

enum class BackgroundType {
    OPEN_LANDSCAPE,
    HIGH_HORIZON,
    MINIMAL_CLEAN,
    BUSY_URBAN;

    fun label(lang: AppLanguage): String = when (this) {
        OPEN_LANDSCAPE -> if (lang == AppLanguage.VI) "Phong cảnh rộng (Trời thoáng)" else "Open Landscape"
        HIGH_HORIZON -> if (lang == AppLanguage.VI) "Chân trời cao / Hậu cảnh đầy" else "High Horizon"
        MINIMAL_CLEAN -> if (lang == AppLanguage.VI) "Nền tối giản / Không gian âm" else "Minimal / Negative Space"
        BUSY_URBAN -> if (lang == AppLanguage.VI) "Hậu cảnh nhiều chi tiết" else "Busy / Urban Background"
    }
}

enum class ForegroundType {
    CLEAN_OPEN,
    FRAMING_EDGES,
    LEADING_GROUND;

    fun label(lang: AppLanguage): String = when (this) {
        CLEAN_OPEN -> if (lang == AppLanguage.VI) "Tiền cảnh thoáng" else "Open Foreground"
        FRAMING_EDGES -> if (lang == AppLanguage.VI) "Có khung tiền cảnh hai bên" else "Side Foreground Frame"
        LEADING_GROUND -> if (lang == AppLanguage.VI) "Tiền cảnh có đường dẫn / bậc" else "Leading Ground Plane"
    }
}

enum class CompositionAnchor {
    LEFT_THIRD,
    CENTER,
    RIGHT_THIRD;

    fun normalizedX(): Float = when (this) {
        LEFT_THIRD -> 0.34f
        CENTER -> 0.50f
        RIGHT_THIRD -> 0.66f
    }

    fun label(lang: AppLanguage): String = when (this) {
        LEFT_THIRD -> if (lang == AppLanguage.VI) "1/3 Trái" else "Left Third"
        CENTER -> if (lang == AppLanguage.VI) "Trung tâm" else "Center"
        RIGHT_THIRD -> if (lang == AppLanguage.VI) "1/3 Phải" else "Right Third"
    }
}

enum class GridMode {
    NONE,
    RULE_OF_THIRDS,
    GOLDEN_RATIO,
    DIAGONALS;

    fun label(lang: AppLanguage): String = when (this) {
        NONE -> if (lang == AppLanguage.VI) "Tắt lưới" else "No Grid"
        RULE_OF_THIRDS -> if (lang == AppLanguage.VI) "Lưới 1/3" else "Rule of 3rds"
        GOLDEN_RATIO -> if (lang == AppLanguage.VI) "Tỉ lệ vàng" else "Golden Ratio"
        DIAGONALS -> if (lang == AppLanguage.VI) "Đường chéo" else "Diagonals"
    }
}

enum class PoseCategory {
    PORTRAIT,
    LANDSCAPE_FULL,
    BACKLIT_MOOD,
    FOREGROUND_FRAME,
    SEATED_CAFE,
    DYNAMIC_WALK,
    CUSTOM;

    fun label(lang: AppLanguage): String = when (this) {
        PORTRAIT -> if (lang == AppLanguage.VI) "Chân dung" else "Portrait"
        LANDSCAPE_FULL -> if (lang == AppLanguage.VI) "Phong cảnh" else "Landscape"
        BACKLIT_MOOD -> if (lang == AppLanguage.VI) "Ngược sáng" else "Backlit"
        FOREGROUND_FRAME -> if (lang == AppLanguage.VI) "Khung tiền cảnh" else "Framing"
        SEATED_CAFE -> if (lang == AppLanguage.VI) "Ngồi / Bậc thềm" else "Seated"
        DYNAMIC_WALK -> if (lang == AppLanguage.VI) "Chuyển động" else "Motion"
        CUSTOM -> if (lang == AppLanguage.VI) "Tùy chỉnh" else "Custom"
    }
}

data class SceneAnalysisResult(
    val lightingDirection: LightingDirection = LightingDirection.FRONT_SOFT,
    val backgroundType: BackgroundType = BackgroundType.MINIMAL_CLEAN,
    val foregroundType: ForegroundType = ForegroundType.CLEAN_OPEN,
    val suggestedAnchor: CompositionAnchor = CompositionAnchor.CENTER,
    val horizonRatio: Float = 0.5f,
    val averageLuma: Float = 128f,
    val contrastRatio: Float = 0.35f,
    val negativeSpaceLeft: Float = 0.5f,
    val negativeSpaceRight: Float = 0.5f,
    val suggestFlipHorizontal: Boolean = false
)

/**
 * Normalized point in pose canvas coordinates: x in [0f..1f], y in [0f..1f].
 */
data class NormPoint(val x: Float, val y: Float)

data class PoseStroke(
    val points: List<NormPoint>,
    val isDashed: Boolean = false,
    val strokeWeight: Float = 1.0f
)

data class PoseHead(
    val center: NormPoint,
    val radius: Float,
    val gazeAngleDeg: Float = 0f
)

/**
 * Represents either a built-in scalable vector pose silhouette OR a user-imported custom PNG/JPG silhouette.
 */
data class PoseGuide(
    val id: String,
    val titleVi: String,
    val titleEn: String,
    val category: PoseCategory,
    val tipVi: String,
    val tipEn: String,
    val idealLighting: Set<LightingDirection>,
    val idealBackgrounds: Set<BackgroundType>,
    val idealForegrounds: Set<ForegroundType>,
    val preferAnchor: CompositionAnchor,
    val defaultScale: Float = 1.0f,
    val heads: List<PoseHead> = emptyList(),
    val strokes: List<PoseStroke> = emptyList(),
    val customImagePath: String? = null,
    val isEdgeExtracted: Boolean = false
) {
    val isCustom: Boolean get() = customImagePath != null
    fun title(lang: AppLanguage): String = if (lang == AppLanguage.VI) titleVi else titleEn
    fun tip(lang: AppLanguage): String = if (lang == AppLanguage.VI) tipVi else tipEn
}

data class RankedPose(
    val pose: PoseGuide,
    val matchScore: Int,
    val reasonVi: String,
    val reasonEn: String,
    val recommendedFlip: Boolean
) {
    fun reason(lang: AppLanguage): String = if (lang == AppLanguage.VI) reasonVi else reasonEn
}

/**
 * Real-time body landmark detected by ML Kit Pose Detection (normalized [0f..1f] in portrait view).
 */
data class DetectedLandmark(
    val x: Float,
    val y: Float,
    val likelihood: Float
)

data class DetectedSkeleton(
    val landmarks: Map<Int, DetectedLandmark> = emptyMap(),
    val hasPerson: Boolean = false
)
