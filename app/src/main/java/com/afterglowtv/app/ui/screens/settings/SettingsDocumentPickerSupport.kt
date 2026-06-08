package com.afterglowtv.app.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.util.UUID

internal fun launchDocumentPickerSafely(
    unavailableMessage: String,
    onError: (String) -> Unit,
    launchPrimary: () -> Unit,
    launchFallback: (() -> Unit)? = null
) {
    runCatching { launchPrimary() }
        .onFailure { error ->
            if (error is ActivityNotFoundException && launchFallback != null) {
                runCatching { launchFallback() }
                    .onFailure { fallbackError ->
                        onError(fallbackError.toPickerErrorMessage(unavailableMessage))
                    }
            } else {
                onError(error.toPickerErrorMessage(unavailableMessage))
            }
        }
}

internal fun openDocumentIntent(mimeTypes: Array<String>): Intent =
    Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = mimeTypes.singleOrNull() ?: "*/*"
        if (mimeTypes.size > 1) {
            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        }
        addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        )
    }

internal fun getContentIntent(mimeTypes: Array<String>): Intent =
    Intent(Intent.ACTION_GET_CONTENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = mimeTypes.singleOrNull() ?: "*/*"
        if (mimeTypes.size > 1) {
            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

internal fun openDocumentTreeIntent(): Intent =
    Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
        addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        )
    }

private fun Throwable.toPickerErrorMessage(fallback: String): String =
    message?.takeIf { it.isNotBlank() } ?: fallback

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
