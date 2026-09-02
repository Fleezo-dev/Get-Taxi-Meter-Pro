package com.example.updater

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.data.repository.RemoteConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

data class UpdateInfo(
    val updateAvailable: Boolean = false,
    val forceUpdate: Boolean = false,
    val telegramUrl: String = "https://t.me/Gettaxikovai",
    val minRequiredVersion: Int = 2,
    val currentVersion: Int = BuildConfig.VERSION_CODE,
    val downloadUrl: String = ""
)

object AppUpdater {
    private const val TAG = "AppUpdater"
    private val _updateInfo = MutableStateFlow(UpdateInfo())
    val updateInfo: StateFlow<UpdateInfo> = _updateInfo

    private var downloadId: Long = -1L

    fun checkForUpdates() {
        try {
            val currentVersion = BuildConfig.VERSION_CODE
            val config = RemoteConfigRepository.getActiveConfig()
            val minRequired = config.minimumRequiredVersion
            val needsUpdate = currentVersion < minRequired

            _updateInfo.value = UpdateInfo(
                updateAvailable = needsUpdate,
                forceUpdate = needsUpdate,
                telegramUrl = config.telegramSupportUrl.ifBlank { "https://t.me/Gettaxikovai" },
                minRequiredVersion = minRequired,
                currentVersion = currentVersion
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for updates: ${e.message}")
        }
    }

    /**
     * Launch Telegram Deep Link Action:
     * Reads hardware Device ID, copies to clipboard with toast notification,
     * and opens Telegram with pre-filled handshake request containing Device ID.
     */
    fun launchTelegram(context: Context, url: String = "https://t.me/Gettaxikovai") {
        val deviceId = try {
            android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "DEV-7A82F90B"
        } catch (e: Exception) {
            "DEV-7A82F90B"
        }

        // 1. Copy Device ID to phone's clipboard
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Device ID", deviceId)
            clipboard?.setPrimaryClip(clip)
            android.widget.Toast.makeText(context, "Device ID copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy Device ID: ${e.message}")
        }

        // 2. Launch Telegram with pre-filled handshake text
        try {
            val encodedMessage = Uri.encode("Hi Get Taxi Admin! My Device ID is: $deviceId. Please provide my Activation Key for the Taxi Meter App.")
            val telegramIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://t.me/Gettaxikovai?text=$encodedMessage")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(telegramIntent)
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (ex: Exception) {
                Log.e(TAG, "Could not launch Telegram: ${ex.message}")
            }
        }
    }

    fun startDownload(context: Context, downloadUrl: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                .setTitle("Get Taxi Meter Update")
                .setDescription("Downloading latest version...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "GetTaxiMeter-update.apk")
                .setMimeType("application/vnd.android.package-archive")

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = downloadManager.enqueue(request)

            val onComplete = object : BroadcastReceiver() {
                override fun onReceive(ctxt: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        installApk(ctxt)
                        try {
                            ctxt.unregisterReceiver(this)
                        } catch (e: Exception) {
                            Log.e(TAG, "Receiver unregister error: ${e.message}")
                        }
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    onComplete,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_EXPORTED
                )
            } else {
                context.registerReceiver(
                    onComplete,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start download: ${e.message}")
        }
    }

    private fun installApk(context: Context) {
        try {
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "GetTaxiMeter-update.apk"
            )
            if (file.exists()) {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        file
                    )
                } else {
                    Uri.fromFile(file)
                }

                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(installIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install APK: ${e.message}")
        }
    }
}
