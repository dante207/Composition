package com.poseghost.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.poseghost.app.overlay.FloatingPoseOverlayService
import com.poseghost.app.ui.CameraPoseScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                PoseGhostApp(
                    onLaunchFloatingOverlay = { poseId, opacity, flipped ->
                        launchFloatingOverlay(poseId, opacity, flipped)
                    }
                )
            }
        }
    }

    private fun launchFloatingOverlay(poseId: String, opacity: Float, flipped: Boolean) {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(
                this,
                "Cấp quyền 'Hiển thị trên ứng dụng khác' để dùng lớp mờ trên Camera mặc định.",
                Toast.LENGTH_LONG
            ).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return
        }

        val serviceIntent = Intent(this, FloatingPoseOverlayService::class.java).apply {
            putExtra(FloatingPoseOverlayService.EXTRA_POSE_ID, poseId)
            putExtra(FloatingPoseOverlayService.EXTRA_OPACITY, opacity)
            putExtra(FloatingPoseOverlayService.EXTRA_FLIPPED, flipped)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        Toast.makeText(
            this,
            "Đã bật lớp phủ mờ! Bạn có thể mở Camera mặc định ngay bây giờ.",
            Toast.LENGTH_SHORT
        ).show()
    }
}

@Composable
private fun PoseGhostApp(
    onLaunchFloatingOverlay: (poseId: String, opacity: Float, flipped: Boolean) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        CameraPoseScreen(onLaunchFloatingOverlay = onLaunchFloatingOverlay)
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0E131B))
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_composition_logo),
                contentDescription = "Composition Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.76f)
                    .height(170.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Ứng dụng cần quyền Camera để phân tích bố cục khung hình, hướng sáng, nhận diện khung xương ML Kit và hiển thị lớp gợi ý tư thế ẩn mờ.",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53935),
                    contentColor = Color.White
                )
            ) {
                Text("Cấp quyền Camera", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
