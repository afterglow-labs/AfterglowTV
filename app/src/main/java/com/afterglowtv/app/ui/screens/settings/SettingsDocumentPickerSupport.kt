package com.afterglowtv.app.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.util.UUID

private const val TV_FRAMEWORK_STUBS_PACKAGE = "com.android.tv.frameworkpackagestubs"

internal fun launchDocumentPickerSafely(
    context: Context,
    action: String,
    unavailableMessage: String,
    onError: (String) -> Unit,
    launch: () -> Unit
) {
    if (!context.hasUsableDocumentPicker(action)) {
        onError(unavailableMessage)
        return
    }
    runCatching { launch() }
        .onFailure { error ->
            val message = when (error) {
                is ActivityNotFoundException -> unavailableMessage
                else -> error.message?.takeIf { it.isNotBlank() } ?: unavailableMessage
            }
            onError(message)
        }
}

internal fun Context.hasUsableDocumentPicker(action: String): Boolean {
    val intent = Intent(action).apply {
        if (action == Intent.ACTION_OPEN_DOCUMENT) {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
    }
    @Suppress("DEPRECATION")
    val activities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    return activities.any { resolved ->
        val info = resolved.activityInfo ?: return@any false
        info.enabled && info.packageName != TV_FRAMEWORK_STUBS_PACKAGE
    }
}

internal fun persistReadPermissionIfAvailable(context: Context, uri: Uri) {
    runCatching {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }
}

internal fun copyPlaylistUriToInternalFile(context: Context, uri: Uri): String {
    if (uri.scheme == "file") return uri.toString()
    val displayName = context.displayNameForUri(uri)
        ?.takeIf { it.isNotBlank() }
        ?: "playlist-${UUID.randomUUID()}.m3u"
    val targetDir = File(context.filesDir, "playlist_sources").apply { mkdirs() }
    val target = File(targetDir, displayName.safePlaylistFileName())
    context.contentResolver.openInputStream(uri)?.use { input ->
        target.outputStream().use { output -> input.copyTo(output) }
    } ?: error("Cannot open selected playlist file.")
    return Uri.fromFile(target).toString()
}

private fun Context.displayNameForUri(uri: Uri): String? {
    var cursor: Cursor? = null
    return try {
        cursor = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) cursor.getString(index) else null
        } else {
            uri.lastPathSegment
        }
    } catch (_: Exception) {
        uri.lastPathSegment
    } finally {
        cursor?.close()
    }
}

private fun String.safePlaylistFileName(): String {
    val cleaned = replace(Regex("[^A-Za-z0-9._-]+"), "_")
        .trim('_', '.', '-')
        .ifBlank { "playlist-${UUID.randomUUID()}.m3u" }
    return if (cleaned.endsWith(".m3u", ignoreCase = true) ||
        cleaned.endsWith(".m3u8", ignoreCase = true)
    ) {
        cleaned
    } else {
        "$cleaned.m3u"
    }
}
