package com.poseghost.app.ui

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.poseghost.app.R
import com.poseghost.app.analyzer.LivePoseDetector
import com.poseghost.app.analyzer.SceneCompositionAnalyzer
import com.poseghost.app.model.AppLanguage
import com.poseghost.app.model.DetectedSkeleton
import com.poseghost.app.model.GridMode
import com.poseghost.app.model.PoseGuide
import com.poseghost.app.model.RankedPose
import com.poseghost.app.model.SceneAnalysisResult
import com.poseghost.app.poses.CustomPoseRepository
import com.poseghost.app.poses.PoseLibrary
import com.poseghost.app.poses.PoseRecommendationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors

private val TintPalette = listOf(
    Color.White,
    Color(0xFFFFE082), // Warm Gold
    Color(0xFF80DEEA)  // Crisp Cyan
)

@Composable
fun CameraPoseScreen(
    onLaunchFloatingOverlay: (poseId: String, opacity: Float, flipped: Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var language by remember { mutableStateOf(AppLanguage.VI) }
    var useFrontCamera by remember { mutableStateOf(false) }
    var gridMode by remember { mutableStateOf(GridMode.RULE_OF_THIRDS) }
    var ghostOpacity by remember { mutableFloatStateOf(0.45f) }
    var ghostVisible by remember { mutableStateOf(true) }
    var skeletonEnabled by remember { mutableStateOf(true) }
    var tintIndex by remember { mutableIntStateOf(0) }
    var showTipBanner by remember { mutableStateOf(true) }
    var autoRecommend by remember { mutableStateOf(true) }
    var manualFlipOverride by remember { mutableStateOf<Boolean?>(null) }

    var poseScale by remember { mutableFloatStateOf(1.0f) }
    var poseOffset by remember { mutableStateOf(Offset.Zero) }
    var shutterFlash by remember { mutableStateOf(false) }

    // Custom imported PNG/JPG silhouettes
    var customPoses by remember { mutableStateOf(CustomPoseRepository.loadCustomPoses(context)) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var importPoseName by remember { mutableStateOf("") }
    var importExtractOutline by remember { mutableStateOf(true) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingImportUri = uri
            importPoseName = if (language == AppLanguage.VI) {
                "Tư thế #${customPoses.size + 1}"
            } else {
                "Custom #${customPoses.size + 1}"
            }
            importExtractOutline = true
        }
    }

    var sceneResult by remember { mutableStateOf(SceneAnalysisResult()) }
    var detectedSkeleton by remember { mutableStateOf(DetectedSkeleton()) }

    val rankedBuiltIn = remember(sceneResult) { PoseRecommendationEngine.rankPoses(sceneResult) }
    val allDisplayPoses: List<RankedPose> = remember(customPoses, rankedBuiltIn) {
        val customWrapped = customPoses.map { custom ->
            RankedPose(
                pose = custom,
                matchScore = 100,
                reasonVi = "Ảnh silhouette tùy chỉnh",
                reasonEn = "Custom imported silhouette",
                recommendedFlip = false
            )
        }
        customWrapped + rankedBuiltIn
    }

    var selectedPoseId by remember { mutableStateOf(PoseLibrary.allPoses.first().id) }

    LaunchedEffect(rankedBuiltIn, autoRecommend) {
        if (autoRecommend && rankedBuiltIn.isNotEmpty()) {
            selectedPoseId = rankedBuiltIn.first().pose.id
        }
    }

    val activeRanked = allDisplayPoses.firstOrNull { it.pose.id == selectedPoseId } ?: rankedBuiltIn.first()
    val activePose: PoseGuide = activeRanked.pose
    val effectiveFlip = manualFlipOverride ?: activeRanked.recommendedFlip

    val alignmentScore = remember(detectedSkeleton, activePose, effectiveFlip) {
        LivePoseDetector.computeAlignmentScore(detectedSkeleton, activePose, effectiveFlip)
    }

    val previewView = remember { PreviewView(context) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    val livePoseDetector = remember { LivePoseDetector() }

    DisposableEffect(Unit) {
        onDispose {
            analyzerExecutor.shutdown()
            livePoseDetector.close()
        }
    }

    LaunchedEffect(useFrontCamera) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val analyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysisUseCase ->
                    analysisUseCase.setAnalyzer(
                        analyzerExecutor,
                        SceneCompositionAnalyzer(
                            isFrontCamera = { useFrontCamera },
                            isSkeletonEnabled = { skeletonEnabled },
                            poseDetector = livePoseDetector,
                            onSceneResult = { res -> sceneResult = res },
                            onSkeletonResult = { skel -> detectedSkeleton = skel }
                        )
                    )
                }

            val selector = if (useFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    imageCapture,
                    analyzer
                )
            } catch (e: Exception) {
                Toast.makeText(context, "Camera error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Live CameraX Preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Semi-Transparent Ghost Pose + Live ML Kit Skeleton Overlay
        PoseOverlayCanvas(
            pose = activePose,
            scene = sceneResult,
            detectedSkeleton = detectedSkeleton,
            showSkeleton = skeletonEnabled,
            alignmentScore = alignmentScore,
            opacity = if (ghostVisible) ghostOpacity else 0f,
            tintColor = TintPalette[tintIndex],
            gridMode = gridMode,
            flipHorizontal = effectiveFlip,
            scale = poseScale,
            offset = poseOffset,
            showHorizonCue = ghostVisible,
            onTransformChange = { newScale, newOffset ->
                poseScale = newScale
                poseOffset = newOffset
            }
        )

        AnimatedVisibility(
            visible = shutterFlash,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.55f))
            )
        }

        // 3. Top Controls & Real-Time Scene + Skeleton Alignment HUD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.78f), Color.Transparent)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Brand Emblem + Name + Auto/Lock Pill
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_composition_emblem),
                        contentDescription = "Composition Logo",
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Composition",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        color = if (autoRecommend) Color(0xFF00C853).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.clickable { autoRecommend = !autoRecommend }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (autoRecommend) Icons.Rounded.AutoAwesome else Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = if (autoRecommend) Color(0xFF69F0AE) else Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = if (autoRecommend) {
                                    if (language == AppLanguage.VI) "Auto" else "Auto"
                                } else {
                                    if (language == AppLanguage.VI) "Khóa" else "Lock"
                                },
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Right icons: VI/EN, Skeleton ML Kit toggle, Grid, Floating Overlay, Switch Camera
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color.White.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(7.dp),
                        modifier = Modifier.clickable {
                            language = if (language == AppLanguage.VI) AppLanguage.EN else AppLanguage.VI
                        }
                    ) {
                        Text(
                            text = if (language == AppLanguage.VI) "VI" else "EN",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = { skeletonEnabled = !skeletonEnabled },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccessibilityNew,
                            contentDescription = "ML Kit Skeleton",
                            tint = if (skeletonEnabled) Color(0xFF40C4FF) else Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val modes = GridMode.entries
                            gridMode = modes[(modes.indexOf(gridMode) + 1) % modes.size]
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GridOn,
                            contentDescription = "Grid",
                            tint = if (gridMode == GridMode.NONE) Color.White.copy(alpha = 0.45f) else Color(0xFFFFD54F),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            onLaunchFloatingOverlay(activePose.id, ghostOpacity, effectiveFlip)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Layers,
                            contentDescription = "Floating Overlay",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { useFrontCamera = !useFrontCamera },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Cameraswitch,
                            contentDescription = "Switch Camera",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Live Scene Analysis Pills + Live ML Kit Pose Match Pill
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                if (skeletonEnabled && detectedSkeleton.hasPerson) {
                    item {
                        val matched = alignmentScore >= 80
                        ScenePill(
                            label = if (language == AppLanguage.VI) {
                                if (matched) "✓ Khớp dáng $alignmentScore% • Chụp ngay!" else "◎ Khớp dáng: $alignmentScore%"
                            } else {
                                if (matched) "✓ Pose Match $alignmentScore% • Shoot!" else "◎ Pose Match: $alignmentScore%"
                            },
                            accent = if (matched) Color(0xFF00E676) else Color(0xFF40C4FF)
                        )
                    }
                }
                item {
                    ScenePill(
                        label = "☀ ${sceneResult.lightingDirection.label(language)}",
                        accent = Color(0xFFFFD54F)
                    )
                }
                item {
                    ScenePill(
                        label = "⛰ ${sceneResult.backgroundType.label(language)}",
                        accent = Color(0xFF80DEEA)
                    )
                }
                item {
                    ScenePill(
                        label = "◫ ${sceneResult.foregroundType.label(language)}",
                        accent = Color(0xFFB39DDB)
                    )
                }
                item {
                    ScenePill(
                        label = "⌖ ${sceneResult.suggestedAnchor.label(language)}",
                        accent = Color(0xFF81C784)
                    )
                }
            }

            if (showTipBanner) {
                Surface(
                    color = Color.Black.copy(alpha = 0.56f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clickable { showTipBanner = false }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (activePose.isCustom) {
                                    activePose.title(language)
                                } else {
                                    "${activePose.title(language)} (${activeRanked.matchScore}%)"
                                },
                                color = Color(0xFFFFE082),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = activePose.tip(language),
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 4. Bottom Dock: Opacity & Layer Controls + Pose Carousel (with Import PNG/JPG) + Shutter
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f), Color.Black.copy(alpha = 0.96f))
                    )
                )
                .navigationBarsPadding()
                .padding(bottom = 10.dp, top = 14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Opacity,
                    contentDescription = "Opacity",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
                Slider(
                    value = ghostOpacity,
                    onValueChange = {
                        ghostOpacity = it
                        if (!ghostVisible && it > 0.05f) ghostVisible = true
                    },
                    valueRange = 0.08f..0.85f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFFD54F),
                        activeTrackColor = Color(0xFFFFD54F),
                        inactiveTrackColor = Color.White.copy(alpha = 0.28f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                )

                IconButton(
                    onClick = { ghostVisible = !ghostVisible },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (ghostVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        contentDescription = "Toggle Ghost",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = { manualFlipOverride = !effectiveFlip },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Flip,
                        contentDescription = "Flip Pose",
                        tint = if (effectiveFlip) Color(0xFF80DEEA) else Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(TintPalette[tintIndex])
                        .border(1.5f.dp, Color.White, CircleShape)
                        .clickable { tintIndex = (tintIndex + 1) % TintPalette.size }
                )

                IconButton(
                    onClick = {
                        poseScale = 1.0f
                        poseOffset = Offset.Zero
                        manualFlipOverride = null
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Reset Transform",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // Pose Carousel: [Import PNG/JPG] + [Custom Silhouettes] + [12 Ranked Built-in Poses]
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "import_custom_pose") {
                    Surface(
                        color = Color(0xFFE53935).copy(alpha = 0.20f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .width(86.dp)
                            .border(1.dp, Color(0xFFEF5350).copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                            .clickable {
                                imagePickerLauncher.launch(arrayOf("image/png", "image/jpeg"))
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .height(76.dp)
                                .padding(6.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AddPhotoAlternate,
                                contentDescription = "Import PNG/JPG",
                                tint = Color(0xFFFF8A80),
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (language == AppLanguage.VI) "+ PNG/JPG" else "+ Import",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }

                items(allDisplayPoses, key = { it.pose.id }) { item ->
                    val isSelected = item.pose.id == activePose.id
                    Surface(
                        color = if (isSelected) Color(0xFFFFD54F).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .width(92.dp)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.7.dp,
                                color = if (isSelected) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                autoRecommend = false
                                selectedPoseId = item.pose.id
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                PoseMiniPreview(
                                    pose = item.pose,
                                    tintColor = if (isSelected) Color(0xFFFFE082) else Color.White,
                                    flipHorizontal = effectiveFlip,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (item.pose.isCustom) {
                                    // Delete custom pose badge
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.7f))
                                            .clickable {
                                                customPoses = CustomPoseRepository.deleteCustomPose(context, item.pose.id)
                                                if (selectedPoseId == item.pose.id) {
                                                    selectedPoseId = rankedBuiltIn.first().pose.id
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Delete",
                                            tint = Color(0xFFFF8A80),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "${item.matchScore}%",
                                        color = if (item.matchScore >= 80) Color(0xFF69F0AE) else Color.White.copy(alpha = 0.8f),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    )
                                }
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = item.pose.title(language),
                                color = Color.White,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Shutter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showTipBanner = !showTipBanner }) {
                    Icon(
                        imageVector = Icons.Rounded.Lightbulb,
                        contentDescription = "Tips",
                        tint = if (showTipBanner) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.5f)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
                            shutterFlash = true
                            scope.launch {
                                delay(120)
                                shutterFlash = false
                            }
                            takePhoto(context, imageCapture, language)
                        }
                )

                Text(
                    text = gridMode.label(language),
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 11.sp,
                    modifier = Modifier.clickable {
                        val modes = GridMode.entries
                        gridMode = modes[(modes.indexOf(gridMode) + 1) % modes.size]
                    }
                )
            }
        }
    }

    // Custom PNG/JPG Import Configuration Dialog
    if (pendingImportUri != null) {
        val uriToImport = pendingImportUri!!
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = {
                Text(
                    text = if (language == AppLanguage.VI) "Nhập ảnh tư thế (PNG / JPG)" else "Import Custom Pose (PNG / JPG)",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = importPoseName,
                        onValueChange = { importPoseName = it },
                        label = {
                            Text(if (language == AppLanguage.VI) "Tên tư thế" else "Pose name")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { importExtractOutline = !importExtractOutline }
                    ) {
                        Checkbox(
                            checked = importExtractOutline,
                            onCheckedChange = { importExtractOutline = it }
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (language == AppLanguage.VI) {
                                "Tự động tách viền trắng (nên bật với ảnh JPG/ảnh chụp mẫu có nền)"
                            } else {
                                "Auto-extract white silhouette outline (best for JPG / solid-background photos)"
                            },
                            fontSize = 13.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD54F),
                        contentColor = Color.Black
                    ),
                    onClick = {
                        val finalName = importPoseName.ifBlank { "Custom Pose" }
                        val extract = importExtractOutline
                        pendingImportUri = null
                        scope.launch {
                            val imported = withContext(Dispatchers.IO) {
                                CustomPoseRepository.importFromUri(context, uriToImport, finalName, extract)
                            }
                            if (imported != null) {
                                customPoses = CustomPoseRepository.loadCustomPoses(context)
                                autoRecommend = false
                                selectedPoseId = imported.id
                                Toast.makeText(
                                    context,
                                    if (language == AppLanguage.VI) "Đã thêm '$finalName' vào thư viện!" else "Added '$finalName' to library!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    if (language == AppLanguage.VI) "Không đọc được ảnh đã chọn." else "Failed to decode selected image.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                ) {
                    Text(if (language == AppLanguage.VI) "Thêm vào thư viện" else "Add to Library")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) {
                    Text(if (language == AppLanguage.VI) "Hủy" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun ScenePill(label: String, accent: Color) {
    Surface(
        color = Color.Black.copy(alpha = 0.54f),
        shape = RoundedCornerShape(50),
        modifier = Modifier.border(0.8.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(50))
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

private fun takePhoto(context: Context, imageCapture: ImageCapture, lang: AppLanguage) {
    val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "Composition_$name")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Composition")
        }
    }

    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val msg = if (lang == AppLanguage.VI) {
                    "Đã lưu ảnh vào Pictures/Composition"
                } else {
                    "Saved photo to Pictures/Composition"
                }
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }

            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(context, "Capture failed: ${exception.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    )
}
