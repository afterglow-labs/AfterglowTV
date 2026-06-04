package com.afterglowtv.data.util

import com.afterglowtv.domain.util.StreamEntryUrlPolicy
import java.net.URI
import java.util.Locale

object UrlSecurityPolicy {
    private val secureRemoteSchemes = setOf("https")
    private val playlistSourceSchemes = setOf("http", "https")
    private val xtreamServerSchemes = setOf("http", "https")
    private val localSchemes = setOf("file", "content")
    // Playlist storage only ever writes file:// paths via the internal copy flow;
    // content:// URIs are not openable by SyncManagerM3uImporter (OkHttp + java.io.File only).
    private val playlistLocalSchemes = setOf("file")
    // IPTV stream/asset URLs may legitimately use plain HTTP or RTSP
    private val streamEntrySchemes = setOf("http", "https", "rtsp", "rtsps", "rtmp", "file", "content")

    fun isSecureRemoteUrl(url: String): Boolean = !containsNewlines(url) && hasAllowedScheme(url, secureRemoteSchemes)

    fun isAllowedImportedUrl(url: String): Boolean =
        !containsNewlines(url) && hasAllowedScheme(url, secureRemoteSchemes + localSchemes)

    /** Validates individual stream/asset entries inside an imported playlist. HTTP is allowed here
     *  because the majority of IPTV providers serve streams over plain HTTP. */
    fun isAllowedStreamEntryUrl(url: String): Boolean =
        StreamEntryUrlPolicy.isAllowed(url)

    fun validateXtreamServerUrl(url: String): String? {
        return validateRemoteUrl(
            url = url,
            allowedSchemes = xtreamServerSchemes,
            invalidSchemeMessage = "Xtream server URLs must use HTTP or HTTPS.",
            missingHostMessage = "Xtream server URLs must include a host.",
            userInfoMessage = "Xtream server URLs must not include embedded credentials.",
            queryFragmentMessage = "Xtream server URLs must not include query parameters or fragments.",
            allowQuery = false
        )
    }

    fun validateStalkerPortalUrl(url: String): String? {
        return validateRemoteUrl(
            url = url,
            allowedSchemes = xtreamServerSchemes,
            invalidSchemeMessage = "Portal URLs must use HTTP or HTTPS.",
            missingHostMessage = "Portal URLs must include a host.",
            userInfoMessage = "Portal URLs must not include embedded credentials.",
            queryFragmentMessage = "Portal URLs must not include query parameters or fragments.",
            allowQuery = false
        )
    }

    fun validateXtreamEpgUrl(url: String): String? {
        return validateRemoteUrl(
            url = url,
            allowedSchemes = xtreamServerSchemes,
            invalidSchemeMessage = "Xtream EPG URLs must use HTTP or HTTPS.",
            missingHostMessage = "Xtream EPG URLs must include a host.",
            userInfoMessage = "Xtream EPG URLs must not include embedded credentials.",
            queryFragmentMessage = "Xtream EPG URLs must not include URL fragments.",
            allowQuery = true
        )
    }

    fun validatePlaylistSourceUrl(url: String): String? {
        if (containsNewlines(url)) {
            return "Playlist sources must use HTTP, HTTPS, or point to a local file."
        }
        val scheme = parseScheme(url) ?: return "Playlist sources must use HTTP, HTTPS, or point to a local file."
        if (scheme in playlistLocalSchemes) {
            return null
        }
        return validateRemoteUrl(
            url = url,
            allowedSchemes = playlistSourceSchemes,
            invalidSchemeMessage = "Playlist sources must use HTTP, HTTPS, or point to a local file.",
            missingHostMessage = "Playlist sources must include a host.",
            userInfoMessage = "Playlist sources must not include embedded credentials in the URL authority.",
            queryFragmentMessage = "Playlist sources must not include URL fragments.",
            allowQuery = true
        )
    }

    fun validateOptionalEpgUrl(url: String): String? {
        return when {
            url.isBlank() -> null
            !containsNewlines(url) && hasAllowedScheme(url, localSchemes) -> null
            // Allow http:// as well as https:// — many IPTV portals serve their XMLTV
            // EPG endpoint over plain HTTP on non-standard ports (same policy as playlists).
            !containsNewlines(url) && hasAllowedScheme(url, playlistSourceSchemes) -> null
            else -> "EPG URLs must use HTTP, HTTPS, or select a local file."
        }
    }

    fun sanitizeImportedAssetUrl(url: String?): String? {
        val value = url?.trim().orEmpty()
        return value.takeIf { it.isNotEmpty() && isAllowedStreamEntryUrl(it) }
    }

    /**
     * Like [sanitizeImportedAssetUrl] but tolerates `{utc}` / `{utcend}` /
     * `{Y}-{m}-{d}-{H}-{M}-{S}` style placeholders found in M3U
     * `catchup-source` URLs. The strict sanitizer rejects these because
     * `URI("http://x/{utc}.ts")` fails to parse — which silently breaks
     * catchup (the import keeps `catchUpSupported = true` but nulls the
     * source URL).
     *
     * We substitute every `{...}` token with a digit, run the resulting
     * concrete URL through the strict validator, and on success return the
     * ORIGINAL template string so runtime substitution still works.
     */
    fun sanitizeImportedTemplateUrl(url: String?): String? {
        val value = url?.trim().orEmpty()
        if (value.isEmpty()) return null
        // Cheap path — no placeholders, defer to strict sanitization.
        if (!value.contains('{')) {
            return value.takeIf { isAllowedStreamEntryUrl(it) }
        }
        // Replace every `{...}` (non-greedy) with "1" to produce a URL the
        // strict scheme/structure check can parse.
        val concrete = value.replace(Regex("\\{[^{}]*\\}"), "1")
        return value.takeIf { isAllowedStreamEntryUrl(concrete) }
    }

    private fun hasAllowedScheme(url: String, allowedSchemes: Set<String>): Boolean {
        val scheme = parseScheme(url) ?: return false
        return scheme in allowedSchemes
    }

    private fun containsNewlines(url: String): Boolean {
        // Decode up to two percent-encoding layers to catch double-encoded payloads
        // (e.g. %250A → %0A → \n). Also checks %09 (tab) which can split log lines.
        var decoded = url
        repeat(2) {
            decoded = decoded
                .replace("%0A", "\n", ignoreCase = true)
                .replace("%0D", "\r", ignoreCase = true)
                .replace("%09", "\t", ignoreCase = true)
        }
        return decoded.any { it == '\n' || it == '\r' || it == '\t' }
    }

    private fun parseScheme(url: String): String? {
        return runCatching { URI(url.trim()).scheme }
            .getOrNull()
            ?.lowercase(Locale.ROOT)
    }

    private fun validateRemoteUrl(
        url: String,
        allowedSchemes: Set<String>,
        invalidSchemeMessage: String,
        missingHostMessage: String,
        userInfoMessage: String,
        queryFragmentMessage: String,
        allowQuery: Boolean
    ): String? {
        if (containsNewlines(url)) return invalidSchemeMessage

        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return invalidSchemeMessage
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return invalidSchemeMessage
        if (scheme !in allowedSchemes) return invalidSchemeMessage
        if (uri.host.isNullOrBlank()) return missingHostMessage
        if (!uri.userInfo.isNullOrBlank()) return userInfoMessage
        if (!allowQuery && !uri.rawQuery.isNullOrBlank()) return queryFragmentMessage
        if (!uri.rawFragment.isNullOrBlank()) return queryFragmentMessage
        return null
    }
}
