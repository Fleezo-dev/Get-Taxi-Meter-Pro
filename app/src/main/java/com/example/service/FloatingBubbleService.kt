package com.example.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.data.model.TripState
import com.example.data.model.TripStatus
import java.util.Locale
import kotlin.math.roundToInt

class FloatingBubbleService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private var windowManager: WindowManager? = null
    private var floatingView: ComposeView? = null
    private lateinit var layoutParams: WindowManager.LayoutParams

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    companion object {
        private const val TAG = "FloatingBubbleService"
        var isServiceRunning = false
            private set

        fun start(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                return
            }
            val intent = Intent(context, FloatingBubbleService::class.java)
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingBubbleService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        isServiceRunning = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        initFloatingBubble()
    }

    @SuppressLint("RtlHardcoded")
    private fun initFloatingBubble() {
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 200
        }

        val params = layoutParams
        floatingView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingBubbleService)
            setViewTreeViewModelStoreOwner(this@FloatingBubbleService)
            setViewTreeSavedStateRegistryOwner(this@FloatingBubbleService)

            setContent {
                val tripState by LocationTrackingService.tripState.collectAsState()
                var isExpanded by remember { mutableStateOf(false) }

                FloatingBubbleUi(
                    tripState = tripState,
                    isExpanded = isExpanded,
                    onToggleExpand = { isExpanded = !isExpanded },
                    onOpenApp = { openMainActivity() },
                    onCloseBubble = { stopSelf() },
                    onDrag = { dx, dy ->
                        params.x = (params.x + dx).roundToInt()
                        params.y = (params.y + dy).roundToInt()
                        try {
                            windowManager?.updateViewLayout(floatingView, params)
                        } catch (e: Exception) {
                            // ignore if detached
                        }
                    }
                )
            }
        }

        try {
            windowManager?.addView(floatingView, layoutParams)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed adding floating bubble view: ${e.message}")
        }
    }

    private fun openMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()

        try {
            if (floatingView != null) {
                windowManager?.removeView(floatingView)
                floatingView = null
            }
        } catch (e: Exception) {
            // view already removed
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

@Composable
fun FloatingBubbleUi(
    tripState: TripState,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onOpenApp: () -> Unit,
    onCloseBubble: () -> Unit,
    onDrag: (Float, Float) -> Unit
) {
    val isRunning = tripState.status == TripStatus.RUNNING
    val isPaused = tripState.status == TripStatus.PAUSED
    val isIdle = tripState.status == TripStatus.IDLE

    val brandRed = Color(0xFFE11D48)
    val darkBg = Color(0xFF0F172A)
    val surfaceBg = Color(0xFF1E293B)
    val accentGreen = Color(0xFF10B981)
    val accentAmber = Color(0xFFF59E0B)

    val statusColor = when {
        isRunning -> accentGreen
        isPaused -> accentAmber
        else -> brandRed
    }

    val formattedFare = "${tripState.currency}${String.format(Locale.US, "%.1f", tripState.currentFare)}"
    val formattedDist = "${String.format(Locale.US, "%.2f", tripState.distanceKm)} km"
    val formattedSpeed = "${String.format(Locale.US, "%.0f", tripState.speedKmH)} km/h"

    val durationMin = tripState.durationSeconds / 60
    val durationSec = tripState.durationSeconds % 60
    val durationStr = String.format(Locale.US, "%02d:%02d", durationMin, durationSec)

    Box(
        modifier = Modifier
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
            .padding(6.dp)
    ) {
        if (!isExpanded) {
            // COMPACT FLOATING PILL BUBBLE
            Surface(
                modifier = Modifier
                    .shadow(elevation = 10.dp, shape = RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
                    .clickable { onToggleExpand() },
                color = darkBg,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, statusColor.copy(alpha = 0.8f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pulsing/Status indicator dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )

                    // Fare / Speed info
                    Column {
                        Text(
                            text = if (isIdle) "GET TAXI" else formattedFare,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (isIdle) formattedSpeed else formattedDist,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    // Expand arrow icon
                    Icon(
                        imageVector = Icons.Default.OpenInFull,
                        contentDescription = "Expand",
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        } else {
            // EXPANDED FLOATING METER CARD
            Surface(
                modifier = Modifier
                    .width(220.dp)
                    .shadow(elevation = 14.dp, shape = RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp)),
                color = darkBg,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, brandRed.copy(alpha = 0.8f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Text(
                                text = when {
                                    isRunning -> "METER ACTIVE"
                                    isPaused -> "METER PAUSED"
                                    else -> "METER IDLE"
                                },
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                color = statusColor,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Collapse Button
                            IconButton(
                                onClick = onToggleExpand,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloseFullscreen,
                                    contentDescription = "Collapse",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            // Close/Dismiss Bubble Button
                            IconButton(
                                onClick = onCloseBubble,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Bubble",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Main Fare Display
                    Surface(
                        color = surfaceBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "TOTAL FARE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = formattedFare,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Grid Stats: Distance, Duration, Speed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "DIST", fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                            Text(text = formattedDist, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "TIME", fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                            Text(text = durationStr, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "SPEED", fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                            Text(text = formattedSpeed, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Quick Return to Full App Button
                    Button(
                        onClick = onOpenApp,
                        colors = ButtonDefaults.buttonColors(containerColor = brandRed),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "FULL METER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
