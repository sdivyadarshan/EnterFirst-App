package com.example.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.EmeraldGreen

/**
 * Manages battery optimization exemptions and manufacturer-specific autostart settings
 * so EnterFirst can receive background notifications, alarms, and updates even when the app is closed.
 */
object BackgroundRestrictionHelper {
    private const val TAG = "BackgroundHelper"

    /**
     * Checks if battery optimizations are currently ignored for this app.
     */
    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
            } else {
                true
            }
        } catch (e: Exception) {
            true
        }
    }

    /**
     * Prompts the user to exempt EnterFirst from battery restrictions.
     */
    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimization(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                if (!isBatteryOptimizationIgnored(context)) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        if (context !is Activity) flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    return
                }
            } catch (e: Exception) {
                Log.w(TAG, "ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS failed: ${e.message}")
            }

            // Fallback 1: Battery saver settings
            try {
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    if (context !is Activity) flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
                return
            } catch (e: Exception) {
                Log.w(TAG, "ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS failed: ${e.message}")
            }
        }

        // Fallback 2: App details settings
        openAppDetailsSettings(context)
    }

    /**
     * Opens the standard Android App Details settings page.
     */
    fun openAppDetailsSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                if (context !is Activity) flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open app settings", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Attempts to open manufacturer-specific Auto-Start and Background Activity settings.
     */
    fun openAutoStartSettings(context: Context): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val intents = mutableListOf<Intent>()

        // Xiaomi / Redmi / POCO (MIUI / HyperOS)
        if (manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco")) {
            intents.add(Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")))
            intents.add(Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.powercenter.PowerSettings")))
        }

        // Samsung
        if (manufacturer.contains("samsung")) {
            intents.add(Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")))
            intents.add(Intent().setComponent(ComponentName("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity")))
        }

        // Oppo / Realme (ColorOS / Realme UI)
        if (manufacturer.contains("oppo") || manufacturer.contains("realme")) {
            intents.add(Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")))
            intents.add(Intent().setComponent(ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")))
            intents.add(Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")))
        }

        // Vivo (Funtouch OS / OriginOS)
        if (manufacturer.contains("vivo")) {
            intents.add(Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")))
            intents.add(Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")))
            intents.add(Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")))
        }

        // Huawei / Honor
        if (manufacturer.contains("huawei") || manufacturer.contains("honor")) {
            intents.add(Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")))
            intents.add(Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")))
        }

        // OnePlus (OxygenOS)
        if (manufacturer.contains("oneplus")) {
            intents.add(Intent().setComponent(ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListAct")))
        }

        for (intent in intents) {
            try {
                if (context !is Activity) intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                // Try next
            }
        }

        // Fallback to app details
        openAppDetailsSettings(context)
        return false
    }
}

/**
 * UI Dialog that guides the user through fixing background and battery restrictions
 * so notifications and alarms work reliably when the app is closed.
 */
@Composable
fun FixBackgroundRestrictionsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isUnrestricted = remember { mutableStateOf(BackgroundRestrictionHelper.isBatteryOptimizationIgnored(context)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070B13).copy(alpha = 0.95f))
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(20.dp)
                .testTag("fix_background_restrictions_dialog"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, EmeraldGreen.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(EmeraldGreen.copy(alpha = 0.15f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.BatteryChargingFull,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Background & Battery",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Fix missed notifications when app is closed",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Current Status Card
                Surface(
                    color = if (isUnrestricted.value) Color(0xFF064E3B) else Color(0xFF451A03),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isUnrestricted.value) EmeraldGreen else Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isUnrestricted.value) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                            contentDescription = null,
                            tint = if (isUnrestricted.value) EmeraldGreen else Color(0xFFFBBF24),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isUnrestricted.value) "Status: Unrestricted (Optimal)" else "Status: Battery Optimized (Restricted)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUnrestricted.value) EmeraldGreen else Color(0xFFFDE68A)
                                )
                            )
                            Text(
                                text = if (isUnrestricted.value)
                                    "App is permitted to wake up for alarms, admin updates, and instant push notifications."
                                else
                                    "Android Doze or battery saver may block notifications when EnterFirst is closed.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Steps Instructions
                Text(
                    text = "Recommended 3 Steps for 100% Delivery:",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text("1. ", color = EmeraldGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Disable Battery Optimization: Set EnterFirst to 'Don't Optimize' or 'Unrestricted'.",
                            color = Color(0xFFE2E8F0),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(verticalAlignment = Alignment.Top) {
                        Text("2. ", color = EmeraldGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Enable Auto-Start & Background Activity: Allow the app to launch after boot or alarms.",
                            color = Color(0xFFE2E8F0),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(verticalAlignment = Alignment.Top) {
                        Text("3. ", color = EmeraldGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Lock in Recents (Optional): Lock EnterFirst in the multi-tasking list to prevent task clearing.",
                            color = Color(0xFFE2E8F0),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            BackgroundRestrictionHelper.requestIgnoreBatteryOptimization(context)
                            isUnrestricted.value = BackgroundRestrictionHelper.isBatteryOptimizationIgnored(context)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("exempt_battery_optimization_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Icon(Icons.Filled.FlashOn, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "1. Set Battery to Unrestricted",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            BackgroundRestrictionHelper.openAutoStartSettings(context)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_autostart_settings_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8))
                    ) {
                        Icon(Icons.Filled.SettingsPower, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "2. Open Auto-Start / App Settings",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    TextButton(
                        onClick = {
                            AnnouncementSyncScheduler.startAllBackgroundSync(context)
                            Toast.makeText(context, "🔄 Background sync heartbeat re-armed!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done & Re-arm Background Alarms", color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
