package com.poseghost.app.poses

import com.poseghost.app.model.BackgroundType
import com.poseghost.app.model.CompositionAnchor
import com.poseghost.app.model.ForegroundType
import com.poseghost.app.model.LightingDirection
import com.poseghost.app.model.NormPoint
import com.poseghost.app.model.PoseCategory
import com.poseghost.app.model.PoseGuide
import com.poseghost.app.model.PoseHead
import com.poseghost.app.model.PoseStroke

/**
 * Curated library of 12 scalable vector pose silhouettes designed around real photography
 * principles: Short Lighting, Rim/Backlight Silhouettes, Negative Space, Foreground Framing,
 * Leading Lines, and Harsh Overhead Light control.
 *
 * All coordinates are normalized to [0f..1f] (default facing RIGHT; engine flips horizontally
 * when light or negative space is on the LEFT).
 */
object PoseLibrary {

    val allPoses: List<PoseGuide> = listOf(
        // 1. Short Lighting 3/4 Portrait
        PoseGuide(
            id = "short_light_34",
            titleVi = "Chân dung 3/4 hướng sáng (Short Light)",
            titleEn = "3/4 Short Lighting Portrait",
            category = PoseCategory.PORTRAIT,
            tipVi = "Xoay vai 45° tránh trực diện, quay nhẹ cằm về phía nguồn sáng để tạo khối khuôn mặt thon gọn.",
            tipEn = "Turn shoulders 45° off-axis and angle chin toward the key light for sculpted short lighting.",
            idealLighting = setOf(LightingDirection.SIDE_LEFT, LightingDirection.SIDE_RIGHT, LightingDirection.FRONT_SOFT),
            idealBackgrounds = setOf(BackgroundType.MINIMAL_CLEAN, BackgroundType.BUSY_URBAN),
            idealForegrounds = setOf(ForegroundType.CLEAN_OPEN, ForegroundType.FRAMING_EDGES),
            preferAnchor = CompositionAnchor.LEFT_THIRD,
            heads = listOf(PoseHead(NormPoint(0.50f, 0.23f), 0.085f, gazeAngleDeg = 18f)),
            strokes = listOf(
                // Neck & angled shoulders
                PoseStroke(listOf(NormPoint(0.49f, 0.315f), NormPoint(0.48f, 0.37f))),
                PoseStroke(listOf(NormPoint(0.32f, 0.42f), NormPoint(0.48f, 0.37f), NormPoint(0.64f, 0.40f)), strokeWeight = 1.2f),
                // Torso contour
                PoseStroke(listOf(NormPoint(0.32f, 0.42f), NormPoint(0.35f, 0.68f), NormPoint(0.33f, 0.88f))),
                PoseStroke(listOf(NormPoint(0.64f, 0.40f), NormPoint(0.61f, 0.66f), NormPoint(0.63f, 0.88f))),
                // Left hand lightly near collarbone/chin
                PoseStroke(listOf(NormPoint(0.32f, 0.42f), NormPoint(0.27f, 0.57f), NormPoint(0.44f, 0.48f))),
                // Right arm relaxed
                PoseStroke(listOf(NormPoint(0.64f, 0.40f), NormPoint(0.69f, 0.62f), NormPoint(0.66f, 0.82f))),
                // Collarbone / center-line cue
                PoseStroke(listOf(NormPoint(0.48f, 0.37f), NormPoint(0.50f, 0.66f)), isDashed = true, strokeWeight = 0.7f)
            )
        ),

        // 2. Backlit Rim Profile / Silhouette
        PoseGuide(
            id = "backlit_rim_profile",
            titleVi = "Xoay nghiêng ngược sáng (Rim Light)",
            titleEn = "Backlit Rim Profile",
            category = PoseCategory.BACKLIT_MOOD,
            tipVi = "Đứng nghiêng 80–90° để viền sáng ôm dọc sống mũi, tóc và bờ vai. Hạ EV -0.7 để giữ chi tiết nền trời.",
            tipEn = "Turn 80–90° in profile so rim light outlines nose, hair, and shoulder. Dial -0.7 EV for sky detail.",
            idealLighting = setOf(LightingDirection.BACKLIT, LightingDirection.LOW_LIGHT),
            idealBackgrounds = setOf(BackgroundType.OPEN_LANDSCAPE, BackgroundType.MINIMAL_CLEAN),
            idealForegrounds = setOf(ForegroundType.CLEAN_OPEN, ForegroundType.LEADING_GROUND),
            preferAnchor = CompositionAnchor.LEFT_THIRD,
            heads = listOf(PoseHead(NormPoint(0.52f, 0.21f), 0.068f, gazeAngleDeg = 5f)),
            strokes = listOf(
                // Graceful neck & profile back
                PoseStroke(listOf(NormPoint(0.50f, 0.28f), NormPoint(0.47f, 0.34f))),
                // S-curve spine & waist
                PoseStroke(listOf(NormPoint(0.47f, 0.34f), NormPoint(0.44f, 0.48f), NormPoint(0.48f, 0.60f), NormPoint(0.45f, 0.88f)), strokeWeight = 1.2f),
                // Front chest & hip contour
                PoseStroke(listOf(NormPoint(0.52f, 0.34f), NormPoint(0.58f, 0.46f), NormPoint(0.54f, 0.60f), NormPoint(0.56f, 0.88f))),
                // Hand touching hair / catching rim light
                PoseStroke(listOf(NormPoint(0.48f, 0.36f), NormPoint(0.40f, 0.46f), NormPoint(0.47f, 0.27f))),
                // Forward stepped leg
                PoseStroke(listOf(NormPoint(0.54f, 0.60f), NormPoint(0.61f, 0.74f), NormPoint(0.63f, 0.89f)))
            )
        ),

        // 3. Foreground Frame Lean (Doorway / Wall / Foliage)
        PoseGuide(
            id = "foreground_frame_lean",
            titleVi = "Tựa khung tiền cảnh (Tường / Cửa / Lá)",
            titleEn = "Foreground Frame Lean",
            category = PoseCategory.FOREGROUND_FRAME,
            tipVi = "Đặt vai tựa nhẹ vào khung tường/cửa bên rìa khung hình, kéo tiền cảnh mờ gần ống kính để tăng chiều sâu.",
            tipEn = "Rest one shoulder against the side wall or doorway and let near-lens elements frame the shot.",
            idealLighting = setOf(LightingDirection.SIDE_LEFT, LightingDirection.SIDE_RIGHT, LightingDirection.FRONT_SOFT, LightingDirection.LOW_LIGHT),
            idealBackgrounds = setOf(BackgroundType.BUSY_URBAN, BackgroundType.MINIMAL_CLEAN, BackgroundType.HIGH_HORIZON),
            idealForegrounds = setOf(ForegroundType.FRAMING_EDGES),
            preferAnchor = CompositionAnchor.LEFT_THIRD,
            heads = listOf(PoseHead(NormPoint(0.44f, 0.22f), 0.068f, gazeAngleDeg = 22f)),
            strokes = listOf(
                // Reference vertical wall line
                PoseStroke(listOf(NormPoint(0.28f, 0.10f), NormPoint(0.28f, 0.92f)), isDashed = true, strokeWeight = 0.8f),
                // Leaning shoulder & torso
                PoseStroke(listOf(NormPoint(0.31f, 0.35f), NormPoint(0.45f, 0.32f), NormPoint(0.56f, 0.37f)), strokeWeight = 1.2f),
                PoseStroke(listOf(NormPoint(0.31f, 0.35f), NormPoint(0.36f, 0.58f))),
                PoseStroke(listOf(NormPoint(0.56f, 0.37f), NormPoint(0.52f, 0.58f))),
                // Arm raised resting on frame
                PoseStroke(listOf(NormPoint(0.31f, 0.35f), NormPoint(0.28f, 0.24f), NormPoint(0.30f, 0.16f))),
                // Other hand in pocket / hip
                PoseStroke(listOf(NormPoint(0.56f, 0.37f), NormPoint(0.61f, 0.49f), NormPoint(0.53f, 0.57f))),
                // Relaxed crossed legs
                PoseStroke(listOf(NormPoint(0.36f, 0.58f), NormPoint(0.41f, 0.75f), NormPoint(0.39f, 0.90f))),
                PoseStroke(listOf(NormPoint(0.52f, 0.58f), NormPoint(0.46f, 0.74f), NormPoint(0.53f, 0.89f)))
            )
        ),

        // 4. Full-Body S-Curve for Open Landscape
        PoseGuide(
            id = "landscape_full_scurve",
            titleVi = "Toàn thân chữ S (Phong cảnh mở)",
            titleEn = "Full-Body Landscape S-Curve",
            category = PoseCategory.LANDSCAPE_FULL,
            tipVi = "Đặt đường chân trời ở 1/3 dưới, dồn trọng tâm lên chân sau, chân trước bước nhẹ tạo dáng thanh thoát giữa nền trời.",
            tipEn = "Place horizon along lower third, shift weight to back foot, and extend front leg for airy landscape balance.",
            idealLighting = setOf(LightingDirection.FRONT_SOFT, LightingDirection.SIDE_LEFT, LightingDirection.SIDE_RIGHT, LightingDirection.BACKLIT),
            idealBackgrounds = setOf(BackgroundType.OPEN_LANDSCAPE, BackgroundType.MINIMAL_CLEAN),
            idealForegrounds = setOf(ForegroundType.CLEAN_OPEN, ForegroundType.LEADING_GROUND),
            preferAnchor = CompositionAnchor.LEFT_THIRD,
            heads = listOf(PoseHead(NormPoint(0.50f, 0.25f), 0.052f, gazeAngleDeg = 12f)),
            strokes = listOf(
                // Shoulders tilted counter to hips
                PoseStroke(listOf(NormPoint(0.41f, 0.35f), NormPoint(0.50f, 0.33f), NormPoint(0.59f, 0.37f)), strokeWeight = 1.2f),
                // Curved torso
                PoseStroke(listOf(NormPoint(0.41f, 0.35f), NormPoint(0.44f, 0.52f), NormPoint(0.41f, 0.60f))),
                PoseStroke(listOf(NormPoint(0.59f, 0.37f), NormPoint(0.55f, 0.51f), NormPoint(0.58f, 0.59f))),
                // Weight-bearing back leg
                PoseStroke(listOf(NormPoint(0.43f, 0.60f), NormPoint(0.44f, 0.75f), NormPoint(0.44f, 0.90f))),
                // Extended front leg
                PoseStroke(listOf(NormPoint(0.56f, 0.59f), NormPoint(0.59f, 0.74f), NormPoint(0.55f, 0.90f))),
                // Arms creating negative space at waist
                PoseStroke(listOf(NormPoint(0.41f, 0.35f), NormPoint(0.35f, 0.48f), NormPoint(0.42f, 0.56f))),
                PoseStroke(listOf(NormPoint(0.59f, 0.37f), NormPoint(0.64f, 0.53f), NormPoint(0.62f, 0.65f)))
            )
        ),

        // 5. Leading Line Walk & Look Back
        PoseGuide(
            id = "leading_line_walk",
            titleVi = "Bước đi theo đường dẫn & Ngoái nhìn",
            titleEn = "Leading-Line Walk & Look Back",
            category = PoseCategory.DYNAMIC_WALK,
            tipVi = "Di chuyển chậm dọc theo đường dẫn tiền cảnh (vỉa hè, lan can, lối đi) rồi ngoái nhẹ qua vai tạo chuyển động tự nhiên.",
            tipEn = "Walk slowly along foreground leading lines and glance back over the near shoulder for candid motion.",
            idealLighting = setOf(LightingDirection.FRONT_SOFT, LightingDirection.BACKLIT, LightingDirection.SIDE_LEFT, LightingDirection.SIDE_RIGHT),
            idealBackgrounds = setOf(BackgroundType.OPEN_LANDSCAPE, BackgroundType.BUSY_URBAN, BackgroundType.HIGH_HORIZON),
            idealForegrounds = setOf(ForegroundType.LEADING_GROUND),
            preferAnchor = CompositionAnchor.CENTER,
            heads = listOf(PoseHead(NormPoint(0.48f, 0.23f), 0.055f, gazeAngleDeg = 160f)),
            strokes = listOf(
                // Subtle perspective ground cue
                PoseStroke(listOf(NormPoint(0.15f, 0.92f), NormPoint(0.44f, 0.66f)), isDashed = true, strokeWeight = 0.7f),
                PoseStroke(listOf(NormPoint(0.85f, 0.92f), NormPoint(0.56f, 0.66f)), isDashed = true, strokeWeight = 0.7f),
                // Turned shoulders
                PoseStroke(listOf(NormPoint(0.39f, 0.33f), NormPoint(0.50f, 0.32f), NormPoint(0.60f, 0.36f)), strokeWeight = 1.2f),
                // Torso
                PoseStroke(listOf(NormPoint(0.39f, 0.33f), NormPoint(0.42f, 0.56f))),
                PoseStroke(listOf(NormPoint(0.60f, 0.36f), NormPoint(0.56f, 0.57f))),
                // Striding legs
                PoseStroke(listOf(NormPoint(0.42f, 0.56f), NormPoint(0.36f, 0.73f), NormPoint(0.32f, 0.89f))),
                PoseStroke(listOf(NormPoint(0.56f, 0.57f), NormPoint(0.61f, 0.73f), NormPoint(0.66f, 0.88f))),
                // Swinging arms
                PoseStroke(listOf(NormPoint(0.39f, 0.33f), NormPoint(0.33f, 0.49f), NormPoint(0.36f, 0.61f))),
                PoseStroke(listOf(NormPoint(0.60f, 0.36f), NormPoint(0.66f, 0.50f), NormPoint(0.62f, 0.62f)))
            )
        ),

        // 6. Close-Up Over-The-Shoulder (Busy Background Isolation)
        PoseGuide(
            id = "closeup_over_shoulder",
            titleVi = "Cận cảnh qua vai (Cô lập nền rối)",
            titleEn = "Over-the-Shoulder Close-Up",
            category = PoseCategory.PORTRAIT,
            tipVi = "Hậu cảnh đang nhiều chi tiết: tiến lại gần khung hình bán thân/cận mặt, đưa vai trước hướng ống kính để xóa phông tối đa.",
            tipEn = "Busy background detected: move closer for a tight over-the-shoulder portrait to maximize subject isolation.",
            idealLighting = setOf(LightingDirection.SIDE_LEFT, LightingDirection.SIDE_RIGHT, LightingDirection.FRONT_SOFT, LightingDirection.LOW_LIGHT),
            idealBackgrounds = setOf(BackgroundType.BUSY_URBAN),
            idealForegrounds = setOf(ForegroundType.CLEAN_OPEN, ForegroundType.FRAMING_EDGES),
            preferAnchor = CompositionAnchor.RIGHT_THIRD,
            heads = listOf(PoseHead(NormPoint(0.53f, 0.30f), 0.115f, gazeAngleDeg = 165f)),
            strokes = listOf(
                // Prominent foreground shoulder arc
                PoseStroke(listOf(NormPoint(0.24f, 0.62f), NormPoint(0.42f, 0.49f), NormPoint(0.62f, 0.51f), NormPoint(0.76f, 0.64f)), strokeWeight = 1.4f),
                // Upper back / side silhouette
                PoseStroke(listOf(NormPoint(0.24f, 0.62f), NormPoint(0.22f, 0.90f))),
                PoseStroke(listOf(NormPoint(0.76f, 0.64f), NormPoint(0.74f, 0.90f))),
                // Collar / jawline cue
                PoseStroke(listOf(NormPoint(0.46f, 0.42f), NormPoint(0.53f, 0.49f)), isDashed = true, strokeWeight = 0.8f)
            )
        ),

        // 7. Seated Low Foreground / Steps
        PoseGuide(
            id = "seated_steps_foreground",
            titleVi = "Ngồi bậc thềm (Chân trời cao)",
            titleEn = "Seated on Steps / Low Foreground",
            category = PoseCategory.SEATED_CAFE,
            tipVi = "Chụp từ góc hơi cao hoặc ngang tầm mắt khi mẫu ngồi bậc thềm, một chân duỗi chéo về góc khung hình để kéo dài dáng.",
            tipEn = "Seat subject on steps or curb with one leg extended diagonally toward the lower corner to elongate the frame.",
            idealLighting = setOf(LightingDirection.FRONT_SOFT, LightingDirection.TOP_HARSH, LightingDirection.SIDE_LEFT, LightingDirection.SIDE_RIGHT),
            idealBackgrounds = setOf(BackgroundType.HIGH_HORIZON, BackgroundType.BUSY_URBAN),
            idealForegrounds = setOf(ForegroundType.LEADING_GROUND, ForegroundType.CLEAN_OPEN),
            preferAnchor = CompositionAnchor.CENTER,
            heads = listOf(PoseHead(NormPoint(0.46f, 0.31f), 0.062f, gazeAngleDeg = 15f)),
            strokes = listOf(
                // Step horizontal reference
                PoseStroke(listOf(NormPoint(0.18f, 0.69f), NormPoint(0.82f, 0.69f)), isDashed = true, strokeWeight = 0.8f),
                // Upper body slightly leaning forward
                PoseStroke(listOf(NormPoint(0.37f, 0.42f), NormPoint(0.47f, 0.40f), NormPoint(0.57f, 0.43f)), strokeWeight = 1.2f),
                PoseStroke(listOf(NormPoint(0.37f, 0.42f), NormPoint(0.38f, 0.66f))),
                PoseStroke(listOf(NormPoint(0.57f, 0.43f), NormPoint(0.54f, 0.66f))),
                // Bent knee up
                PoseStroke(listOf(NormPoint(0.38f, 0.66f), NormPoint(0.47f, 0.56f), NormPoint(0.49f, 0.78f))),
                // Extended diagonal leg
                PoseStroke(listOf(NormPoint(0.54f, 0.66f), NormPoint(0.65f, 0.76f), NormPoint(0.73f, 0.90f))),
                // Forearm resting on knee
                PoseStroke(listOf(NormPoint(0.37f, 0.42f), NormPoint(0.41f, 0.56f), NormPoint(0.52f, 0.55f))),
                // Support hand on step
                PoseStroke(listOf(NormPoint(0.57f, 0.43f), NormPoint(0.62f, 0.58f), NormPoint(0.61f, 0.69f)))
            )
        ),

        // 8. Harsh Overhead Light Chin-Up / Hat-Shade Pose
        PoseGuide(
            id = "overhead_light_chinup",
            titleVi = "Ngước nhẹ hứng sáng (Chống bóng gắt)",
            titleEn = "Overhead Light Chin-Up",
            category = PoseCategory.PORTRAIT,
            tipVi = "Ánh sáng đang gắt từ trên xuống: nâng cằm nhẹ 10–15° hoặc đưa tay che nắng nhẹ để tránh bóng đen hốc mắt (raccoon eyes).",
            tipEn = "Harsh overhead light: lift chin 10–15° or raise a hand to soften shadows and eliminate dark eye-socket shadows.",
            idealLighting = setOf(LightingDirection.TOP_HARSH),
            idealBackgrounds = setOf(BackgroundType.OPEN_LANDSCAPE, BackgroundType.MINIMAL_CLEAN, BackgroundType.BUSY_URBAN),
            idealForegrounds = setOf(ForegroundType.CLEAN_OPEN, ForegroundType.FRAMING_EDGES),
            preferAnchor = CompositionAnchor.CENTER,
            heads = listOf(PoseHead(NormPoint(0.50f, 0.25f), 0.085f, gazeAngleDeg = -32f)),
            strokes = listOf(
                // Shoulders
                PoseStroke(listOf(NormPoint(0.32f, 0.43f), NormPoint(0.50f, 0.39f), NormPoint(0.68f, 0.43f)), strokeWeight = 1.2f),
                // Torso
                PoseStroke(listOf(NormPoint(0.32f, 0.43f), NormPoint(0.36f, 0.82f))),
                PoseStroke(listOf(NormPoint(0.68f, 0.43f), NormPoint(0.64f, 0.82f))),
                // Hand playfully shading brow / catching light
                PoseStroke(listOf(NormPoint(0.68f, 0.43f), NormPoint(0.73f, 0.30f), NormPoint(0.59f, 0.19f))),
                // Relaxed left arm
                PoseStroke(listOf(NormPoint(0.32f, 0.43f), NormPoint(0.29f, 0.66f), NormPoint(0.35f, 0.78f)))
            )
        ),

        // 9. Contemplative Back View (Vast Landscape / Sea / Mountain)
        PoseGuide(
            id = "back_view_landscape",
            titleVi = "Xoay lưng ngắm cảnh (Không gian rộng)",
            titleEn = "Contemplative Back View",
            category = PoseCategory.LANDSCAPE_FULL,
            tipVi = "Đặt chủ thể nhỏ ở 1/3 khung hình xoay lưng về ống kính để tôn vinh sự hùng vĩ của phong cảnh và đường chân trời.",
            tipEn = "Place subject smaller on a vertical third facing the scenery to emphasize scale and open horizon.",
            idealLighting = setOf(LightingDirection.BACKLIT, LightingDirection.TOP_HARSH, LightingDirection.FRONT_SOFT, LightingDirection.LOW_LIGHT),
            idealBackgrounds = setOf(BackgroundType.OPEN_LANDSCAPE),
            idealForegrounds = setOf(ForegroundType.CLEAN_OPEN, ForegroundType.LEADING_GROUND),
            preferAnchor = CompositionAnchor.RIGHT_THIRD,
            heads = listOf(PoseHead(NormPoint(0.50f, 0.36f), 0.044f, gazeAngleDeg = -90f)),
            strokes = listOf(
                // Compact environmental scale figure
                PoseStroke(listOf(NormPoint(0.43f, 0.44f), NormPoint(0.50f, 0.42f), NormPoint(0.57f, 0.44f)), strokeWeight = 1.2f),
                PoseStroke(listOf(NormPoint(0.43f, 0.44f), NormPoint(0.45f, 0.64f))),
                PoseStroke(listOf(NormPoint(0.57f, 0.44f), NormPoint(0.55f, 0.64f))),
                PoseStroke(listOf(NormPoint(0.45f, 0.64f), NormPoint(0.45f, 0.86f))),
                PoseStroke(listOf(NormPoint(0.55f, 0.64f), NormPoint(0.57f, 0.86f))),
                // One hand lightly raised / holding hat or camera
                PoseStroke(listOf(NormPoint(0.43f, 0.44f), NormPoint(0.39f, 0.56f), NormPoint(0.42f, 0.64f))),
                PoseStroke(listOf(NormPoint(0.57f, 0.44f), NormPoint(0.62f, 0.54f), NormPoint(0.60f, 0.63f)))
            )
        ),

        // 10. Cafe / Window Light Table Lean
        PoseGuide(
            id = "cafe_window_lean",
            titleVi = "Tựa bàn ánh sáng cửa sổ (Window Light)",
            titleEn = "Window-Light Table Lean",
            category = PoseCategory.SEATED_CAFE,
            tipVi = "Đặt khuỷu tay nhẹ lên mặt bàn tiền cảnh, nghiêng đầu về phía cửa sổ sáng để tạo ánh sáng mềm tự nhiên.",
            tipEn = "Rest elbows lightly on the foreground table and tilt toward the window side light for soft falloff.",
            idealLighting = setOf(LightingDirection.SIDE_LEFT, LightingDirection.SIDE_RIGHT, LightingDirection.LOW_LIGHT),
            idealBackgrounds = setOf(BackgroundType.MINIMAL_CLEAN, BackgroundType.BUSY_URBAN, BackgroundType.HIGH_HORIZON),
            idealForegrounds = setOf(ForegroundType.LEADING_GROUND, ForegroundType.FRAMING_EDGES, ForegroundType.CLEAN_OPEN),
            preferAnchor = CompositionAnchor.LEFT_THIRD,
            heads = listOf(PoseHead(NormPoint(0.49f, 0.28f), 0.088f, gazeAngleDeg = 20f)),
            strokes = listOf(
                // Table foreground line
                PoseStroke(listOf(NormPoint(0.14f, 0.72f), NormPoint(0.86f, 0.72f)), isDashed = true, strokeWeight = 0.9f),
                // Shoulders leaning gently forward
                PoseStroke(listOf(NormPoint(0.32f, 0.46f), NormPoint(0.49f, 0.42f), NormPoint(0.66f, 0.47f)), strokeWeight = 1.2f),
                PoseStroke(listOf(NormPoint(0.32f, 0.46f), NormPoint(0.35f, 0.72f))),
                PoseStroke(listOf(NormPoint(0.66f, 0.47f), NormPoint(0.63f, 0.72f))),
                // Left elbow on table supporting chin
                PoseStroke(listOf(NormPoint(0.32f, 0.46f), NormPoint(0.30f, 0.70f), NormPoint(0.43f, 0.39f))),
                // Right forearm relaxed across table
                PoseStroke(listOf(NormPoint(0.66f, 0.47f), NormPoint(0.69f, 0.69f), NormPoint(0.50f, 0.70f)))
            )
        ),

        // 11. Wide-Angle Dynamic Diagonal (Architecture / Street)
        PoseGuide(
            id = "wide_angle_dynamic",
            titleVi = "Góc rộng nghiêng động (Kiến trúc / Đường phố)",
            titleEn = "Wide-Angle Dynamic Stance",
            category = PoseCategory.DYNAMIC_WALK,
            tipVi = "Hạ thấp máy ảnh, đặt chân trước gần góc dưới để tạo hiệu ứng kéo dài tỉ lệ khi chụp tiêu cự rộng (24–35mm).",
            tipEn = "Lower the camera angle and step the front foot toward the bottom corner for dynamic wide-angle proportions.",
            idealLighting = setOf(LightingDirection.FRONT_SOFT, LightingDirection.TOP_HARSH, LightingDirection.SIDE_LEFT, LightingDirection.SIDE_RIGHT),
            idealBackgrounds = setOf(BackgroundType.OPEN_LANDSCAPE, BackgroundType.BUSY_URBAN, BackgroundType.HIGH_HORIZON),
            idealForegrounds = setOf(ForegroundType.LEADING_GROUND, ForegroundType.CLEAN_OPEN),
            preferAnchor = CompositionAnchor.CENTER,
            heads = listOf(PoseHead(NormPoint(0.50f, 0.19f), 0.052f, gazeAngleDeg = 0f)),
            strokes = listOf(
                // Confident wide stance
                PoseStroke(listOf(NormPoint(0.40f, 0.30f), NormPoint(0.50f, 0.28f), NormPoint(0.60f, 0.31f)), strokeWeight = 1.2f),
                PoseStroke(listOf(NormPoint(0.40f, 0.30f), NormPoint(0.43f, 0.52f))),
                PoseStroke(listOf(NormPoint(0.60f, 0.31f), NormPoint(0.57f, 0.52f))),
                // Wide dynamic legs
                PoseStroke(listOf(NormPoint(0.43f, 0.52f), NormPoint(0.39f, 0.72f), NormPoint(0.35f, 0.92f))),
                PoseStroke(listOf(NormPoint(0.57f, 0.52f), NormPoint(0.62f, 0.72f), NormPoint(0.67f, 0.92f))),
                // Arms relaxed / jacket hold
                PoseStroke(listOf(NormPoint(0.40f, 0.30f), NormPoint(0.34f, 0.45f), NormPoint(0.41f, 0.51f))),
                PoseStroke(listOf(NormPoint(0.60f, 0.31f), NormPoint(0.65f, 0.48f), NormPoint(0.62f, 0.59f)))
            )
        ),

        // 12. Couple Natural Interaction
        PoseGuide(
            id = "couple_natural_frame",
            titleVi = "Cặp đôi tương tác tự nhiên",
            titleEn = "Natural Couple Framing",
            category = PoseCategory.LANDSCAPE_FULL,
            tipVi = "Hai người đứng lệch độ cao nhẹ, hướng ánh mắt về nhau hoặc cùng nhìn về phía khoảng không gian âm của khung hình.",
            tipEn = "Stagger shoulder heights slightly and angle both faces toward each other or into the open negative space.",
            idealLighting = setOf(LightingDirection.FRONT_SOFT, LightingDirection.BACKLIT, LightingDirection.SIDE_LEFT, LightingDirection.SIDE_RIGHT),
            idealBackgrounds = setOf(BackgroundType.OPEN_LANDSCAPE, BackgroundType.MINIMAL_CLEAN),
            idealForegrounds = setOf(ForegroundType.CLEAN_OPEN, ForegroundType.FRAMING_EDGES),
            preferAnchor = CompositionAnchor.CENTER,
            heads = listOf(
                PoseHead(NormPoint(0.41f, 0.24f), 0.052f, gazeAngleDeg = 18f),
                PoseHead(NormPoint(0.58f, 0.28f), 0.050f, gazeAngleDeg = 162f)
            ),
            strokes = listOf(
                // Person A (left, taller)
                PoseStroke(listOf(NormPoint(0.32f, 0.34f), NormPoint(0.41f, 0.32f), NormPoint(0.49f, 0.35f)), strokeWeight = 1.2f),
                PoseStroke(listOf(NormPoint(0.32f, 0.34f), NormPoint(0.35f, 0.58f), NormPoint(0.34f, 0.88f))),
                PoseStroke(listOf(NormPoint(0.49f, 0.35f), NormPoint(0.47f, 0.58f), NormPoint(0.46f, 0.88f))),
                // Person B (right, leaning in)
                PoseStroke(listOf(NormPoint(0.51f, 0.38f), NormPoint(0.58f, 0.36f), NormPoint(0.66f, 0.39f)), strokeWeight = 1.2f),
                PoseStroke(listOf(NormPoint(0.51f, 0.38f), NormPoint(0.52f, 0.60f), NormPoint(0.53f, 0.88f))),
                PoseStroke(listOf(NormPoint(0.66f, 0.39f), NormPoint(0.64f, 0.60f), NormPoint(0.65f, 0.88f))),
                // Connected hands / waist embrace
                PoseStroke(listOf(NormPoint(0.49f, 0.35f), NormPoint(0.55f, 0.49f), NormPoint(0.62f, 0.51f)))
            )
        )
    )
}
