package com.afterglowtv.app.ui.screens.settings

import android.content.Context
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.afterglowtv.app.BuildConfig
import com.afterglowtv.app.ui.theme.OnSurface
import com.afterglowtv.app.ui.theme.OnSurfaceDim
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val AMAZON_APPSTORE_PACKAGE = "com.amazon.venezia"
private const val PACKAGE_INSTALLER_MIME = "application/vnd.android.package-archive"
private val KnownInstallerPackages = listOf(
    "com.android.packageinstaller",
    "com.google.android.packageinstaller",
    "com.amazon.packageinstaller",
    "com.amazon.device.software.ota",
    "com.amazon.tv.launcher",
    AMAZON_APPSTORE_PACKAGE,
)

internal fun LazyListScope.settingsInstallDiagnosticsSection(
    context: Context
) {
    item {
        SettingsSectionHeader(
            title = "Install Pathways",
            subtitle = "Developer diagnostics for package installer, unknown-source, and Appstore launch paths."
        )
    }
    item {
        var logLines by rememberSaveable { mutableStateOf(listOf("Press Test to probe this device.")) }
        var watchDeviceSettings by rememberSaveable { mutableStateOf(false) }
        if (watchDeviceSettings) {
            DisposableEffect(context) {
                val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean, uri: Uri?) {
                        logLines = logLines + buildDeviceSettingsChangeLog(context, uri)
                    }
                }
                val resolver = context.contentResolver
                resolver.registerContentObserver(Settings.Global.getUriFor("adb_enabled"), false, observer)
                resolver.registerContentObserver(Settings.Global.getUriFor("development_settings_enabled"), false, observer)
                resolver.registerContentObserver(Settings.Secure.getUriFor("install_non_market_apps"), false, observer)
                logLines = logLines + buildDeviceSettingsWatcherStartedLog(context)
                onDispose {
                    resolver.unregisterContentObserver(observer)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ClickableSettingsRow(
                label = "Test",
                value = "Run verbose probes",
                onClick = { logLines = runInstallPathwayDiagnostics(context) }
            )
            ClickableSettingsRow(
                label = "Watch device dev settings",
                value = if (watchDeviceSettings) "Watching" else "Start watcher",
                onClick = { watchDeviceSettings = !watchDeviceSettings }
            )
            ClickableSettingsRow(
                label = "Clear log",
                value = "${logLines.size} lines",
                onClick = { logLines = listOf("Log cleared.") }
            )
            InstallDiagnosticsLog(lines = logLines)
        }
    }
}

@androidx.compose.runtime.Composable
private fun InstallDiagnosticsLog(lines: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.32f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Log",
            style = MaterialTheme.typography.labelLarge,
            color = OnSurface
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(lines, key = { index, line -> "$index-$line" }) { _, line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = when {
                        line.contains("FAIL", ignoreCase = true) ||
                            line.contains("blocked", ignoreCase = true) -> Color(0xFFFF8A80)
                        line.contains("OK", ignoreCase = true) ||
                            line.contains("accepted", ignoreCase = true) -> Color(0xFF8FE388)
                        else -> OnSurfaceDim
                    }
                )
            }
        }
    }
}

@Suppress("DEPRECATION")
private fun runInstallPathwayDiagnostics(context: Context): List<String> {
    val pm = context.packageManager
    val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
    return buildList {
        add("Afterglow install pathway probe")
        add("Time: $now")
        add("App: ${BuildConfig.APPLICATION_ID} ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        add("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        add("Product: ${Build.PRODUCT}")
        add("SDK: ${Build.VERSION.SDK_INT} / ${Build.VERSION.RELEASE}")
        add("Build: ${Build.DISPLAY}")
        add("Fingerprint: ${Build.FINGERPRINT.take(120)}")
        add("")
        add("Install source: ${currentInstallSource(pm, context.packageName)}")
        add("Amazon Appstore installed: ${pm.hasPackage(AMAZON_APPSTORE_PACKAGE)}")
        add("REQUEST_INSTALL_PACKAGES declared: ${pm.hasPermission(context.packageName, android.Manifest.permission.REQUEST_INSTALL_PACKAGES)}")
        add("INSTALL_PACKAGES granted: ${pm.hasPermission(context.packageName, "android.permission.INSTALL_PACKAGES")}")
        add("Global adb_enabled: ${readGlobalSetting(context, "adb_enabled")}")
        add("Global development_settings_enabled: ${readGlobalSetting(context, "development_settings_enabled")}")
        add("Secure install_non_market_apps: ${readSecureSetting(context, "install_non_market_apps")}")
        add("canRequestPackageInstalls: ${safeCanRequestPackageInstalls(pm)}")
        add("")
        add("Known installer package inventory:")
        KnownInstallerPackages.forEach { packageName ->
            add("  $packageName: ${if (pm.hasPackage(packageName)) "present" else "missing"}")
        }
        add("")
        addInstallPermissionInventory(pm)
        add("")
        addIntentProbe(
            label = "Unknown app sources settings for this app",
            pm = pm,
            intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        )
        addIntentProbe(
            label = "Unknown app sources settings generic",
            pm = pm,
            intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
        )
        addIntentProbe(
            label = "General security settings",
            pm = pm,
            intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
        )
        addIntentProbe(
            label = "All application settings",
            pm = pm,
            intent = Intent(Settings.ACTION_APPLICATION_SETTINGS)
        )
        addIntentProbe(
            label = "Manage applications settings",
            pm = pm,
            intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
        )
        addIntentProbe(
            label = "Application details settings for Afterglow",
            pm = pm,
            intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        )
        KnownInstallerPackages.filter(pm::hasPackage).forEach { packageName ->
            addIntentProbe(
                label = "Application details settings for $packageName",
                pm = pm,
                intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        }
        add("")
        add("ADB / developer settings intent probes:")
        addIntentProbe(
            label = "Developer settings",
            pm = pm,
            intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        )
        addIntentProbe(
            label = "Device info settings",
            pm = pm,
            intent = Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)
        )
        addIntentProbe(
            label = "Main settings",
            pm = pm,
            intent = Intent(Settings.ACTION_SETTINGS)
        )
        addComponentProbe(
            label = "Android DevelopmentSettings component",
            pm = pm,
            packageName = "com.android.settings",
            className = "com.android.settings.DevelopmentSettings"
        )
        addComponentProbe(
            label = "Android dev dashboard component",
            pm = pm,
            packageName = "com.android.settings",
            className = "com.android.settings.Settings\$DevelopmentSettingsDashboardActivity"
        )
        addComponentProbe(
            label = "Amazon settings package",
            pm = pm,
            packageName = "com.amazon.tv.settings",
            className = "com.amazon.tv.settings.MainSettingsActivity"
        )
        addComponentProbe(
            label = "Amazon developer options component",
            pm = pm,
            packageName = "com.amazon.tv.settings",
            className = "com.amazon.tv.settings.tv.developeroptions.DeveloperOptionsActivity"
        )
        add("")
        add("Package installer intent probes:")
        addIntentProbe(
            label = "Package installer ACTION_VIEW content uri",
            pm = pm,
            intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    Uri.parse("content://${BuildConfig.APPLICATION_ID}.fileprovider/probe.apk"),
                    PACKAGE_INSTALLER_MIME
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        )
        addIntentProbe(
            label = "Package installer ACTION_VIEW file uri",
            pm = pm,
            intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    Uri.fromFile(File("/sdcard/Download/afterglow-probe.apk")),
                    PACKAGE_INSTALLER_MIME
                )
            }
        )
        addIntentProbe(
            label = "Package installer ACTION_VIEW no data",
            pm = pm,
            intent = Intent(Intent.ACTION_VIEW).apply {
                type = PACKAGE_INSTALLER_MIME
            }
        )
        addIntentProbe(
            label = "Package installer ACTION_INSTALL_PACKAGE content uri",
            pm = pm,
            intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                data = Uri.parse("content://${BuildConfig.APPLICATION_ID}.fileprovider/probe.apk")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        )
        addIntentProbe(
            label = "Package installer ACTION_INSTALL_PACKAGE file uri",
            pm = pm,
            intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                data = Uri.fromFile(File("/sdcard/Download/afterglow-probe.apk"))
            }
        )
        KnownInstallerPackages.filter(pm::hasPackage).forEach { packageName ->
            addIntentProbe(
                label = "Package installer ACTION_VIEW forced to $packageName",
                pm = pm,
                intent = Intent(Intent.ACTION_VIEW).apply {
                    setPackage(packageName)
                    setDataAndType(
                        Uri.parse("content://${BuildConfig.APPLICATION_ID}.fileprovider/probe.apk"),
                        PACKAGE_INSTALLER_MIME
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            )
        }
        add("")
        add("Picker/download-manager intent probes:")
        addIntentProbe(
            label = "File picker ACTION_OPEN_DOCUMENT apk",
            pm = pm,
            intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = PACKAGE_INSTALLER_MIME
            }
        )
        addIntentProbe(
            label = "File picker ACTION_GET_CONTENT apk",
            pm = pm,
            intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = PACKAGE_INSTALLER_MIME
            }
        )
        addIntentProbe(
            label = "Downloads UI",
            pm = pm,
            intent = Intent("android.intent.action.VIEW_DOWNLOADS")
        )
        add("")
        add("Launch probes: skipped by Test so diagnostics cannot move focus or close the app.")
        add("")
        add("Probe complete.")
    }
}

private fun buildDeviceSettingsWatcherStartedLog(context: Context): List<String> =
    listOf(
        "",
        "Device dev settings watcher started at ${timestampMs()}",
        "Global adb_enabled: ${readGlobalSetting(context, "adb_enabled")}",
        "Global development_settings_enabled: ${readGlobalSetting(context, "development_settings_enabled")}",
        "Secure install_non_market_apps: ${readSecureSetting(context, "install_non_market_apps")}",
    )

private fun buildDeviceSettingsChangeLog(context: Context, uri: Uri?): List<String> =
    listOf(
        "",
        "Device setting changed at ${timestampMs()} uri=${uri ?: "unknown"}",
        "Global adb_enabled: ${readGlobalSetting(context, "adb_enabled")}",
        "Global development_settings_enabled: ${readGlobalSetting(context, "development_settings_enabled")}",
        "Secure install_non_market_apps: ${readSecureSetting(context, "install_non_market_apps")}",
    )

private fun timestampMs(): String =
    SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())

private fun MutableList<String>.addIntentProbe(
    label: String,
    pm: PackageManager,
    intent: Intent
) {
    val resolved = runCatching { intent.resolveActivity(pm) }
        .getOrElse {
            add("$label: blocked ${it.javaClass.simpleName} resolving default ${it.message.orEmpty()}")
            return
        }
    val matches = runCatching { pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY) }
        .getOrElse {
            add("$label: blocked ${it.javaClass.simpleName} querying handlers ${it.message.orEmpty()}")
            return
        }
    if (resolved == null && matches.isEmpty()) {
        add("$label: FAIL no handler")
    } else {
        add("$label: OK ${resolved?.flattenToShortString() ?: "no default"} (${matches.size} handlers)")
        matches.take(5).forEach { match ->
            add("  - ${match.activityInfo.packageName}/${match.activityInfo.name}")
        }
    }
}

private fun MutableList<String>.addComponentProbe(
    label: String,
    pm: PackageManager,
    packageName: String,
    className: String
) {
    val intent = Intent().apply {
        component = ComponentName(packageName, className)
    }
    val resolved = runCatching { intent.resolveActivity(pm) }
        .getOrElse {
            add("$label: blocked ${it.javaClass.simpleName} ${it.message.orEmpty()}")
            return
        }
    add("$label: ${if (resolved == null) "FAIL no handler" else "OK ${resolved.flattenToShortString()}"}")
}

private fun MutableList<String>.addStartActivityProbe(
    context: Context,
    label: String,
    intent: Intent
) {
    try {
        context.startActivity(intent)
        add("$label: accepted by ActivityManager")
    } catch (error: ActivityNotFoundException) {
        add("$label: blocked ActivityNotFoundException ${error.message.orEmpty()}")
    } catch (error: SecurityException) {
        add("$label: blocked SecurityException ${error.message.orEmpty()}")
    } catch (error: RuntimeException) {
        add("$label: blocked ${error.javaClass.simpleName} ${error.message.orEmpty()}")
    }
}

private fun MutableList<String>.addInstallPermissionInventory(pm: PackageManager) {
    add("Packages requesting installer permissions:")
    @Suppress("DEPRECATION")
    val packages = runCatching { pm.getInstalledPackages(PackageManager.GET_PERMISSIONS) }
        .getOrDefault(emptyList())
    val matches = packages
        .filter { info ->
            info.requestedPermissions.orEmpty().any { permission ->
                permission == android.Manifest.permission.REQUEST_INSTALL_PACKAGES ||
                    permission == "android.permission.INSTALL_PACKAGES"
            }
        }
        .sortedBy { it.packageName }
    if (matches.isEmpty()) {
        add("  none visible to Afterglow")
    } else {
        matches.take(30).forEach { info ->
            val permissions = info.requestedPermissions.orEmpty()
                .filter {
                    it == android.Manifest.permission.REQUEST_INSTALL_PACKAGES ||
                        it == "android.permission.INSTALL_PACKAGES"
                }
                .joinToString()
            add("  ${info.packageName}: $permissions")
        }
        if (matches.size > 30) {
            add("  ... ${matches.size - 30} more")
        }
    }
}

private fun PackageManager.hasPackage(packageName: String): Boolean =
    runCatching {
        getPackageInfo(packageName, 0)
        true
    }.getOrDefault(false)

private fun PackageManager.hasPermission(packageName: String, permission: String): Boolean =
    runCatching { checkPermission(permission, packageName) == PackageManager.PERMISSION_GRANTED }
        .getOrDefault(false)

private fun safeCanRequestPackageInstalls(pm: PackageManager): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        runCatching { pm.canRequestPackageInstalls().toString() }
            .getOrElse { "blocked ${it.javaClass.simpleName}: ${it.message.orEmpty()}" }
    } else {
        "n/a before Android O"
    }

private fun readGlobalSetting(context: Context, key: String): String =
    runCatching { Settings.Global.getString(context.contentResolver, key) ?: "null" }
        .getOrElse { "blocked ${it.javaClass.simpleName}: ${it.message.orEmpty()}" }

private fun readSecureSetting(context: Context, key: String): String =
    runCatching { Settings.Secure.getString(context.contentResolver, key) ?: "null" }
        .getOrElse { "blocked ${it.javaClass.simpleName}: ${it.message.orEmpty()}" }

private fun currentInstallSource(pm: PackageManager, packageName: String): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        runCatching {
            val info = pm.getInstallSourceInfo(packageName)
            listOfNotNull(
                "initiating=${info.initiatingPackageName}",
                "installing=${info.installingPackageName}",
                "originating=${info.originatingPackageName}"
            ).joinToString(" ")
        }.getOrElse { "unknown (${it.javaClass.simpleName}: ${it.message})" }
    } else {
        @Suppress("DEPRECATION")
        runCatching { pm.getInstallerPackageName(packageName) ?: "unknown" }
            .getOrElse { "unknown (${it.javaClass.simpleName}: ${it.message})" }
    }
