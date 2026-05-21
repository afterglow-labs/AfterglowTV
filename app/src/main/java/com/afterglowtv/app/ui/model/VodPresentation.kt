package com.afterglowtv.app.ui.model

import java.util.Locale

data class VodDisplayTitle(
    val title: String,
    val year: String? = null
)

object VodTitleFormatter {
    private val yearPattern = Regex("""(?<!\d)((?:19|20)\d{2})(?!\d)""")
    private val extensionPattern = Regex("""\.(mkv|mp4|avi|mov|m4v|ts|webm)$""", RegexOption.IGNORE_CASE)
    private val removableTokens = setOf(
        "240p",
        "360p",
        "480p",
        "540p",
        "576p",
        "720p",
        "1080p",
        "1440p",
        "2160p",
        "4k",
        "8k",
        "hdr",
        "hdr10",
        "dv",
        "sdr",
        "web",
        "webdl",
        "web-dl",
        "webrip",
        "bluray",
        "blu-ray",
        "brrip",
        "dvdrip",
        "hdtv",
        "x264",
        "x265",
        "h264",
        "h265",
        "hevc",
        "aac",
        "dts",
        "proper",
        "repack"
    )
    private val keepUppercase = setOf("TV", "UFC", "WWE", "AEW", "NBA", "NFL", "MLB", "NHL", "FBI", "CIA", "XXX")

    fun format(rawTitle: String?, fallbackYear: String? = null): VodDisplayTitle {
        val raw = rawTitle.orEmpty().trim()
        if (raw.isBlank()) return VodDisplayTitle(title = "Untitled", year = fallbackYear)

        val withoutExtension = raw.replace(extensionPattern, "")
        val normalized = withoutExtension
            .replace(Regex("""[\[\]{}()]"""), " ")
            .replace(Regex("""[._]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        val year = yearPattern.find(normalized)?.value ?: fallbackYear
        val tokens = normalized
            .split(' ')
            .map { it.trim(',', '-', ':', ';') }
            .filter { token ->
                val compact = token.lowercase(Locale.US).replace(".", "")
                token.isNotBlank() &&
                    token != year &&
                    compact !in removableTokens
            }

        val title = tokens
            .joinToString(" ")
            .replace(Regex("""\s+[-:]\s*$"""), "")
            .trim()
            .ifBlank { raw }

        return VodDisplayTitle(
            title = title.toDisplayTitle(),
            year = year
        )
    }

    private fun String.toDisplayTitle(): String {
        return split(Regex("""\s+"""))
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                val stripped = word.trim()
                val uppercase = stripped.uppercase(Locale.US)
                when {
                    uppercase in keepUppercase -> uppercase
                    uppercase.matches(Regex("""[IVXLCDM]{2,}""")) -> uppercase
                    else -> stripped.lowercase(Locale.US).replaceFirstChar { char ->
                        if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
                    }
                }
            }
    }
}

data class VodShelfItem(
    val id: String,
    val title: String,
    val providerCategory: String?,
    val genres: List<String> = emptyList()
)

data class VodShelfSection(
    val title: String,
    val items: List<VodShelfItem>
)

data class VodGuideItem(
    val id: String,
    val title: String,
    val providerCategory: String?
)

data class VodGuideRow(
    val title: String,
    val items: List<VodGuideItem>
)

object VodGuideRowBuilder {
    fun build(
        items: List<VodGuideItem>,
        uncategorizedTitle: String,
        maximumItemsPerRow: Int = 36
    ): List<VodGuideRow> {
        if (items.isEmpty()) return emptyList()

        val grouped = linkedMapOf<String, MutableList<VodGuideItem>>()
        items.forEach { item ->
            val category = item.providerCategory?.takeIf(String::isNotBlank) ?: uncategorizedTitle
            grouped.getOrPut(category) { mutableListOf() }.add(item)
        }

        return grouped.map { (title, rowItems) ->
            VodGuideRow(
                title = title,
                items = rowItems.take(maximumItemsPerRow)
            )
        }
    }
}

object VodShelfSectionBuilder {
    fun build(
        items: List<VodShelfItem>,
        minimumSectionSize: Int = 2,
        maximumItemsPerSection: Int = 24
    ): List<VodShelfSection> {
        if (items.isEmpty()) return emptyList()

        val remaining = items.toMutableList()
        val sections = mutableListOf<VodShelfSection>()

        sections += groupedSections(
            source = remaining,
            minimumSectionSize = minimumSectionSize,
            maximumItemsPerSection = maximumItemsPerSection,
            keySelector = { it.providerCategory?.takeIf(String::isNotBlank) }
        )
        remaining.removeAll(sections.flatMap { it.items }.toSet())

        val genreSections = groupedSections(
            source = remaining,
            minimumSectionSize = minimumSectionSize,
            maximumItemsPerSection = maximumItemsPerSection,
            keySelector = { item -> item.genres.firstOrNull { it.isNotBlank() } }
        )
        sections += genreSections
        remaining.removeAll(genreSections.flatMap { it.items }.toSet())

        sections += remaining
            .groupBy { alphabeticalKey(it.title) }
            .toSortedMap()
            .map { (title, sectionItems) ->
                VodShelfSection(
                    title = title,
                    items = sectionItems.sortedBy { it.title.lowercase(Locale.US) }.take(maximumItemsPerSection)
                )
            }

        return sections
    }

    private fun groupedSections(
        source: List<VodShelfItem>,
        minimumSectionSize: Int,
        maximumItemsPerSection: Int,
        keySelector: (VodShelfItem) -> String?
    ): List<VodShelfSection> {
        return source
            .groupBy { item -> keySelector(item)?.trim().orEmpty() }
            .filterKeys { it.isNotBlank() }
            .filterValues { it.size >= minimumSectionSize }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
            .map { (title, sectionItems) ->
                VodShelfSection(
                    title = title,
                    items = sectionItems.sortedBy { it.title.lowercase(Locale.US) }.take(maximumItemsPerSection)
                )
            }
    }

    private fun alphabeticalKey(title: String): String {
        val first = title.trim().firstOrNull { it.isLetterOrDigit() } ?: return "#"
        return if (first.isLetter()) first.uppercaseChar().toString() else "#"
    }
}
