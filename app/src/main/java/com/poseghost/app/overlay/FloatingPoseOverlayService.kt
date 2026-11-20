package com.poseghost.app.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.poseghost.app.R
import com.poseghost.app.model.PoseGuide
import com.poseghost.app.poses.CustomPoseRepository
import com.poseghost.app.poses.PoseLibrary
import kotlin.math.min

/**
 * Floating System Overlay Service (SYSTEM_ALERT_WINDOW).
 *
 * Renders both built-in vector poses AND user-imported custom PNG/JPG silhouettes
 * directly on top of the stock Android Camera app with touch pass-through.
 */
class FloatingPoseOverlayService : Service() {

    companion object {
        const val EXTRA_POSE_ID = "extra_pose_id"
        const val EXTRA_OPACITY = "extra_opacity"
        const val EXTRA_FLIPPED = "extra_flipped"
        private const val CHANNEL_ID = "poseghost_overlay_channel"
        private const val NOTIFICATION_ID = 1042
    }

    private lateinit var windowManager: WindowManager
    private var ghostView: GhostPoseAndroidView? = null
    private var controlBar: View? = null

    private var allPoses: List<PoseGuide> = PoseLibrary.allPoses
    private var poseIndex = 0
    private var opacity = 0.45f
    private var flipped = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        allPoses = CustomPoseRepository.loadCustomPoses(this) + PoseLibrary.allPoses
        val poseId = intent?.getStringExtra(EXTRA_POSE_ID)
        val idx = allPoses.indexOfFirst { it.id == poseId }
        if (idx >= 0) poseIndex = idx else poseIndex = 0
        opacity = intent?.getFloatExtra(EXTRA_OPACITY, 0.45f) ?: 0.45f
        flipped = intent?.getBooleanExtra(EXTRA_FLIPPED, false) ?: false

        if (ghostView == null) {
            attachGhostOverlay()
            attachControlBar()
        } else {
            updateGhostState()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        ghostView?.recycleBitmap()
        ghostView?.let { windowManager.removeView(it) }
        controlBar?.let { windowManager.removeView(it) }
        ghostView = null
        controlBar = null
        super.onDestroy()
    }

    private fun attachGhostOverlay() {
        val view = GhostPoseAndroidView(this).apply {
            setPoseGuide(allPoses[poseIndex])
            ghostAlpha = opacity
            flipHorizontal = flipped
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        windowManager.addView(view, params)
        ghostView = view
    }

    private fun attachControlBar() {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.argb(190, 18, 22, 30))
            setPadding(18, 10, 18, 10)
            gravity = Gravity.CENTER_VERTICAL
        }

        fun makeBtn(text: String, onClick: () -> Unit): TextView {
            return TextView(this).apply {
                this.text = text
                setTextColor(Color.WHITE)
                textSize = 13f
                setPadding(18, 8, 18, 8)
                setOnClickListener { onClick() }
            }
        }

        bar.addView(makeBtn("◀") {
            poseIndex = (poseIndex - 1 + allPoses.size) % allPoses.size
            updateGhostState()
        })
        bar.addView(makeBtn("▶") {
            poseIndex = (poseIndex + 1) % allPoses.size
            updateGhostState()
        })
        bar.addView(makeBtn("⇄") {
            flipped = !flipped
            updateGhostState()
        })
        bar.addView(makeBtn("−") {
            opacity = (opacity - 0.1f).coerceAtLeast(0.12f)
            updateGhostState()
        })
        bar.addView(makeBtn("+") {
            opacity = (opacity + 0.1f).coerceAtMost(0.85f)
            updateGhostState()
        })
        bar.addView(makeBtn("✕") {
            stopSelf()
        })

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 120
        }

        windowManager.addView(bar, params)
        controlBar = bar
    }

    private fun updateGhostState() {
        ghostView?.apply {
            setPoseGuide(allPoses[poseIndex])
            ghostAlpha = opacity
            flipHorizontal = flipped
            invalidate()
        }
    }

    private fun startForegroundNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.overlay_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }
}

private class GhostPoseAndroidView(context: Context) : View(context) {
    private var pose: PoseGuide? = null
    private var customBitmap: Bitmap? = null
    var ghostAlpha: Float = 0.45f
    var flipHorizontal: Boolean = false

    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val dashedPaint = Paint(mainPaint).apply {
        pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f)
    }
    private val haloPaint = Paint(mainPaint).apply {
        color = Color.BLACK
    }

    fun setPoseGuide(newPose: PoseGuide) {
        if (pose?.id == newPose.id) return
        pose = newPose
        recycleBitmap()
        val path = newPose.customImagePath
        if (path != null) {
            customBitmap = try {
                BitmapFactory.decodeFile(path)
            } catch (_: Exception) {
                null
            }
        }
    }

    fun recycleBitmap() {
        customBitmap?.recycle()
        customBitmap = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val currentPose = pose ?: return
        val alphaInt = (ghostAlpha * 255).toInt().coerceIn(0, 255)

        val bmp = customBitmap
        if (bmp != null && !bmp.isRecycled) {
            bitmapPaint.alpha = alphaInt
            val maxW = width * 0.88f
            val maxH = height * 0.72f
            val aspect = bmp.width.toFloat() / bmp.height.toFloat().coerceAtLeast(1f)
            val drawW: Float
            val drawH: Float
            if (maxW / maxH > aspect) {
                drawH = maxH
                drawW = maxH * aspect
            } else {
                drawW = maxW
                drawH = maxW / aspect
            }
            val left = (width - drawW) * 0.5f
            val top = (height - drawH) * 0.5f
            canvas.save()
            if (flipHorizontal) {
                canvas.scale(-1f, 1f, width * 0.5f, height * 0.5f)
            }
            canvas.drawBitmap(bmp, null, RectF(left, top, left + drawW, top + drawH), bitmapPaint)
            canvas.restore()
            return
        }

        mainPaint.alpha = alphaInt
        dashedPaint.alpha = (alphaInt * 0.6f).toInt()
        haloPaint.alpha = (alphaInt * 0.4f).toInt()

        val box = min(width * 0.9f, height * 0.68f)
        val ox = (width - box) / 2f
        val oy = (height - box) / 2f

        fun mx(nx: Float) = ox + (if (flipHorizontal) 1f - nx else nx) * box
        fun my(ny: Float) = oy + ny * box

        currentPose.strokes.forEach { stroke ->
            if (stroke.points.size < 2) return@forEach
            val path = Path().apply {
                moveTo(mx(stroke.points[0].x), my(stroke.points[0].y))
                for (i in 1 until stroke.points.size) {
                    lineTo(mx(stroke.points[i].x), my(stroke.points[i].y))
                }
            }
            if (!stroke.isDashed) {
                haloPaint.strokeWidth = 12f * stroke.strokeWeight
                canvas.drawPath(path, haloPaint)
            }
            val p = if (stroke.isDashed) dashedPaint else mainPaint
            p.strokeWidth = 5.5f * stroke.strokeWeight
            canvas.drawPath(path, p)
        }

        currentPose.heads.forEach { head ->
            val cx = mx(head.center.x)
            val cy = my(head.center.y)
            val r = head.radius * box
            haloPaint.strokeWidth = 12f
            canvas.drawCircle(cx, cy, r, haloPaint)
            mainPaint.strokeWidth = 5.5f
            canvas.drawCircle(cx, cy, r, mainPaint)
        }
    }
}
