package com.afterglowtv.app.store

import com.afterglowtv.data.parser.M3uParser
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.random.Random

internal object BundledPublicGuideGenerator {
    private val xmltvTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss Z", Locale.US)

    fun writeGuide(
        playlistFile: File,
        outputFile: File,
        nowMs: Long = System.currentTimeMillis()
    ): Int {
        val entries = playlistFile.inputStream().use { input ->
            M3uParser().parse(input).entries
        }
            .distinctBy { entry -> entry.channelId() }
            .filter { entry -> entry.channelId().isNotBlank() }

        outputFile.parentFile?.mkdirs()
        val zoneId = ZoneId.systemDefault()
        val firstDay = Instant.ofEpochMilli(nowMs)
            .atZone(zoneId)
            .toLocalDate()
            .minusDays(1)

        outputFile.outputStream().bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            writer.appendLine("""<tv generator-info-name="AfterglowTV Bundled Public Guide">""")
            entries.forEach { entry ->
                writer.appendLine("""  <channel id="${entry.channelId().xmlEscaped()}">""")
                writer.appendLine("""    <display-name>${entry.displayName().xmlEscaped()}</display-name>""")
                entry.tvgLogo?.takeIf { it.isNotBlank() }?.let { logo ->
                    writer.appendLine("""    <icon src="${logo.xmlEscaped()}"/>""")
                }
                writer.appendLine("  </channel>")
            }
            entries.forEach { entry ->
                val profile = profileFor(entry)
                repeat(GUIDE_DAYS) { dayOffset ->
                    val day = firstDay.plusDays(dayOffset.toLong())
                    val seed = "${entry.channelId()}|$day".hashCode()
                    val random = Random(seed)
                    var cursor = day.atStartOfDay(zoneId)
                    val dayEnd = cursor.plusDays(1)
                    var segmentIndex = 0
                    while (cursor.isBefore(dayEnd)) {
                        val durationMinutes = profile.durationMinutes.random(random)
                        val stop = cursor.plusMinutes(durationMinutes.toLong()).coerceAtMost(dayEnd)
                        val title = profile.titles[(segmentIndex + random.nextInt(profile.titles.size)) % profile.titles.size]
                        val description = profile.descriptions[(segmentIndex + random.nextInt(profile.descriptions.size)) % profile.descriptions.size]
                        writer.appendLine(
                            """  <programme start="${xmltvTimeFormatter.format(cursor)}" stop="${xmltvTimeFormatter.format(stop)}" channel="${entry.channelId().xmlEscaped()}">"""
                        )
                        writer.appendLine("""    <title>${title.xmlEscaped()}</title>""")
                        writer.appendLine("""    <desc>${description.xmlEscaped()}</desc>""")
                        writer.appendLine("""    <category>${profile.category.xmlEscaped()}</category>""")
                        writer.appendLine("  </programme>")
                        cursor = stop
                        segmentIndex++
                    }
                }
            }
            writer.appendLine("</tv>")
        }
        return entries.size
    }

    private fun ZonedDateTime.coerceAtMost(maximum: ZonedDateTime): ZonedDateTime =
        if (isAfter(maximum)) maximum else this

    private fun M3uParser.M3uEntry.channelId(): String =
        tvgId?.takeIf { it.isNotBlank() }
            ?: tvgName?.takeIf { it.isNotBlank() }
            ?: "afterglow.public.${name.slug()}"

    private fun M3uParser.M3uEntry.displayName(): String =
        tvgName?.takeIf { it.isNotBlank() } ?: name

    private fun profileFor(entry: M3uParser.M3uEntry): GuideProfile {
        val text = "${entry.name} ${entry.groupTitle}".lowercase(Locale.US)
        return when {
            text.contains("weather") -> GuideProfile(
                category = "Weather",
                durationMinutes = listOf(30, 30, 45, 60),
                titles = listOf(
                    "Weather Now",
                    "Local Forecast",
                    "Regional Weather",
                    "Travel Forecast",
                    "Weekend Outlook"
                ),
                descriptions = listOf(
                    "Current conditions, forecast updates, and weather headlines.",
                    "Regional weather coverage with practical forecast timing.",
                    "A rolling weather update with conditions and outlooks."
                )
            )
            text.contains("classic") || text.contains("movie") -> GuideProfile(
                category = "Movies",
                durationMinutes = listOf(90, 105, 120, 135, 150),
                titles = listOf(
                    "Classic Movie",
                    "Matinee Feature",
                    "Vintage Cinema",
                    "Classic Film Block",
                    "Cinema Encore",
                    "Late Night Movie"
                ),
                descriptions = listOf(
                    "A classic feature presentation from the public movie stream.",
                    "Vintage film programming and classic cinema selections.",
                    "A rotating movie block for classic film fans."
                )
            )
            text.contains("arts") || text.contains("showcase") -> GuideProfile(
                category = "Arts",
                durationMinutes = listOf(45, 60, 75, 90),
                titles = listOf(
                    "Arts Showcase",
                    "Gallery Spotlight",
                    "Performance Hour",
                    "Creative Works",
                    "Concert and Stage"
                ),
                descriptions = listOf(
                    "Performances, gallery pieces, and arts programming.",
                    "A curated block of public arts and culture selections.",
                    "Visual arts, music, dance, and stage programming."
                )
            )
            text.contains("music") || text.contains("boni") -> GuideProfile(
                category = "Music",
                durationMinutes = listOf(45, 60, 60, 90),
                titles = listOf(
                    "Music Mix",
                    "Artist Spotlight",
                    "Indie Music Hour",
                    "Boni Records Playlist",
                    "New Sounds"
                ),
                descriptions = listOf(
                    "Music programming from public and independent sources.",
                    "A rotating music block with artist and label selections.",
                    "Independent music videos, performances, and playlist cuts."
                )
            )
            text.contains("access") ||
                text.contains("community") ||
                text.contains("nashua") ||
                text.contains("monterey") ||
                text.contains("aftv") ||
                text.contains("amp") -> GuideProfile(
                    category = "Public Access",
                    durationMinutes = listOf(30, 45, 60, 60, 90),
                    titles = listOf(
                        "Community Bulletin",
                        "Local Affairs",
                        "Public Access Magazine",
                        "Community Calendar",
                        "Neighborhood Spotlight",
                        "Civic Replay"
                    ),
                    descriptions = listOf(
                        "Local public access programming and community information.",
                        "Community-focused programming, events, and civic coverage.",
                        "Public access programming from local and regional producers."
                    )
                )
            text.contains("tears") -> GuideProfile(
                category = "Sample Streams",
                durationMinutes = listOf(45, 60, 75, 90),
                titles = listOf(
                    "Tears of Steel",
                    "Short Film Showcase",
                    "Public Demo Feature",
                    "Streaming Sample"
                ),
                descriptions = listOf(
                    "A public short-film stream used for playback and guide demos.",
                    "Public demo programming suitable for testing video playback.",
                    "A video sample block for validating stream playback."
                )
            )
            text.contains("sample") ||
                text.contains("demo") ||
                text.contains("test") ||
                text.contains("bipbop") ||
                text.contains("mux") -> GuideProfile(
                    category = "Sample Streams",
                    durationMinutes = listOf(30, 45, 60, 75),
                    titles = listOf(
                        "Public Stream Sample",
                        "Playback Demo",
                        "HD Video Sample",
                        "Streaming Test Segment",
                        "Demo Reel"
                    ),
                    descriptions = listOf(
                        "A public HLS sample stream for playback testing.",
                        "Demo programming for checking video startup and guide behavior.",
                        "A rotating sample block for public stream validation."
                    )
                )
            else -> GuideProfile(
                category = "Public Programming",
                durationMinutes = listOf(45, 60, 75, 90),
                titles = listOf(
                    "Public Programming",
                    "Channel Showcase",
                    "Featured Block",
                    "Public Stream"
                ),
                descriptions = listOf(
                    "Publicly available linear programming.",
                    "A rotating block from the bundled public source.",
                    "General programming from the included public playlist."
                )
            )
        }
    }

    private fun String.slug(): String =
        lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), ".")
            .trim('.')
            .ifBlank { "channel" }

    private fun String.xmlEscaped(): String =
        replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("'", "&apos;")

    private data class GuideProfile(
        val category: String,
        val durationMinutes: List<Int>,
        val titles: List<String>,
        val descriptions: List<String>
    )

    private const val GUIDE_DAYS = 8
}
