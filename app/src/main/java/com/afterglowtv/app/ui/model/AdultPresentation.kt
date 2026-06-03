package com.afterglowtv.app.ui.model

import com.afterglowtv.domain.model.Category
import com.afterglowtv.domain.model.Channel
import com.afterglowtv.domain.model.LocalMediaItem
import com.afterglowtv.domain.model.Movie
import com.afterglowtv.domain.model.Series
import java.util.Locale

data class AdultCategory(
    val key: String,
    val title: String,
    val channels: List<Channel>
)

internal data class AdultCategoryMatch(
    val key: String,
    val title: String
)

object AdultCategoryBuilder {
    const val ALL_CATEGORY_KEY = "all"

    private const val OTHER_CATEGORY_KEY = "other"
    private val explicitAdultSignals = listOf(
        "milf",
        "milfs",
        "step",
        "stepmom",
        "step mom",
        "stepdad",
        "step dad",
        "stepsister",
        "step sister",
        "stepbrother",
        "step brother",
        "taboo",
        "trans",
        "transgender",
        "tg",
        "ts",
        "lesbian",
        "bbw",
        "cougar",
        "cougars",
        "fetish",
        "bdsm",
        "bondage",
        "threesome",
        "orgy"
    )
    private val normalizedExplicitAdultSignals = explicitAdultSignals.map(::normalizeAdultText)
    private val categoryRules = listOf(
        AdultRule("milf", "MILF", listOf("milf", "milfs", "stepmom", "step mom")),
        AdultRule(
            "taboo",
            "Taboo",
            listOf(
                "taboo",
                "family",
                "step",
                "stepmom",
                "step mom",
                "stepdad",
                "step dad",
                "stepsister",
                "step sister",
                "stepbrother",
                "step brother",
                "cousin",
                "aunt",
                "uncle"
            )
        ),
        AdultRule("interracial", "Interracial", listOf("interracial", "ir")),
        AdultRule("blondes", "Blondes", listOf("blonde", "blondes")),
        AdultRule("brunettes", "Brunettes", listOf("brunette", "brunettes")),
        AdultRule("trans", "Trans", listOf("trans", "transgender", "tg", "ts")),
        AdultRule("latina", "Latina", listOf("latina", "latinas", "latin")),
        AdultRule("asian", "Asian", listOf("asian", "japanese", "korean", "chinese")),
        AdultRule("ebony", "Ebony", listOf("ebony", "black")),
        AdultRule("amateur", "Amateur", listOf("amateur", "homemade")),
        AdultRule("lesbian", "Lesbian", listOf("lesbian", "lesbians")),
        AdultRule("gay", "Gay", listOf("gay")),
        AdultRule("bbw", "BBW", listOf("bbw")),
        AdultRule("mature", "Mature", listOf("mature", "cougar", "cougars")),
        AdultRule("pov", "POV", listOf("pov")),
        AdultRule("group", "Group", listOf("group", "threesome", "orgy")),
        AdultRule("fetish", "Fetish", listOf("fetish", "bdsm", "bondage")),
        AdultRule("reality", "Reality", listOf("reality", "casting", "audition")),
        AdultRule("vr", "VR", listOf("vr", "virtual reality")),
        AdultRule("4k", "4K", listOf("4k", "uhd"))
    )

    fun build(
        channels: List<Channel>,
        providerCategories: List<Category>,
        includeAllCategory: Boolean = true
    ): List<AdultCategory> {
        if (channels.isEmpty()) return emptyList()

        val providerCategoryById = providerCategories.associateBy { it.id }
        val grouped = linkedMapOf<String, AdultMutableCategory>()
        if (includeAllCategory) {
            grouped[ALL_CATEGORY_KEY] = AdultMutableCategory(ALL_CATEGORY_KEY, "All", channels.toMutableList())
        }

        channels.forEach { channel ->
            val matches = keywordCategoryMatches(channel.name, channel.categoryName, channel.groupTitle)
            if (matches.isEmpty()) {
                val sourceCategory = channel.categoryId?.let(providerCategoryById::get)
                if (isAdultChannel(channel, sourceCategory)) {
                    val fallbackCategory = sourceCategory?.takeIf(::isAdultCategory)
                        ?: channel.categoryName
                            ?.takeIf(::isAdultText)
                            ?.let { Category(id = Long.MIN_VALUE, name = it, isAdult = true) }
                    val key = fallbackCategory?.name
                        ?.let(::adultCategoryKey)
                        ?.takeIf(String::isNotBlank)
                        ?: OTHER_CATEGORY_KEY
                    val title = fallbackCategory?.name
                        ?.trim()
                        ?.takeIf(String::isNotBlank)
                        ?: "Other"
                    grouped.getOrPut(key) {
                        AdultMutableCategory(key, title, mutableListOf())
                    }.channels.add(channel)
                }
            } else {
                matches.forEach { rule ->
                    grouped.getOrPut(rule.key) {
                        AdultMutableCategory(rule.key, rule.title, mutableListOf())
                    }.channels.add(channel)
                }
            }
        }

        return grouped.values
            .map { category ->
                AdultCategory(
                    key = category.key,
                    title = category.title,
                    channels = category.channels.distinctBy { it.id }
                )
            }
            .filter { it.channels.isNotEmpty() }
    }

    fun matchesGeneratedCategory(value: String?): Boolean =
        normalizeAdultText(value)
            .takeIf(String::isNotBlank)
            ?.let { normalized -> categoryRules.any { it.matchesNormalized(normalized) } }
            ?: false

    internal fun keywordCategoryMatches(
        primaryValue: String?,
        vararg contextValues: String?
    ): List<AdultCategoryMatch> {
        val normalizedPrimary = normalizeAdultText(primaryValue)
        val normalizedContextValues = contextValues
            .map(::normalizeAdultText)
            .filter(String::isNotBlank)
        val primaryMatches = categoryRules
            .filter { rule -> rule.matchesNormalized(normalizedPrimary) }
            .map { rule -> AdultCategoryMatch(rule.key, rule.title) }
        val primaryKeys = primaryMatches.mapTo(mutableSetOf()) { it.key }
        val contextMatches = categoryRules
            .filterNot { rule -> rule.key in primaryKeys }
            .filter { rule -> normalizedContextValues.any(rule::matchesNormalized) }
            .map { rule -> AdultCategoryMatch(rule.key, rule.title) }
        return primaryMatches + contextMatches
    }

    fun matchesExplicitAdultSignal(value: String?): Boolean {
        val normalized = normalizeAdultText(value)
        if (normalized.isBlank()) return false
        return normalizedExplicitAdultSignals.any { normalizedSignal ->
            normalized == normalizedSignal ||
                normalized.startsWith("$normalizedSignal ") ||
                normalized.endsWith(" $normalizedSignal") ||
                normalized.contains(" $normalizedSignal ")
        }
    }
}

internal fun isAdultCategory(category: Category?): Boolean {
    if (category == null) return false
    return category.isAdult || isAdultText(category.name)
}

internal fun isLikelyAdultCategory(category: Category?): Boolean {
    if (category == null) return false
    return isAdultCategory(category) || AdultCategoryBuilder.matchesExplicitAdultSignal(category.name)
}

internal fun isAdultChannel(channel: Channel, category: Category? = null): Boolean {
    return channel.isAdult ||
        isLikelyAdultCategory(category) ||
        isAdultText(channel.categoryName) ||
        isAdultText(channel.groupTitle) ||
        isAdultText(channel.name) ||
        AdultCategoryBuilder.matchesGeneratedCategory(channel.categoryName) ||
        AdultCategoryBuilder.matchesGeneratedCategory(channel.groupTitle) ||
        AdultCategoryBuilder.matchesGeneratedCategory(channel.name)
}

internal fun isExplicitAdultChannel(channel: Channel): Boolean {
    return channel.isAdult ||
        isAdultText(channel.categoryName) ||
        isAdultText(channel.groupTitle) ||
        isAdultText(channel.name) ||
        AdultCategoryBuilder.matchesExplicitAdultSignal(channel.categoryName) ||
        AdultCategoryBuilder.matchesExplicitAdultSignal(channel.groupTitle) ||
        AdultCategoryBuilder.matchesExplicitAdultSignal(channel.name)
}

internal fun isAdultVodMovie(movie: Movie, category: Category? = null): Boolean =
    movie.isAdult ||
        isAdultCategory(category) ||
        isAdultText(movie.categoryName) ||
        isAdultText(movie.genre) ||
        isAdultText(movie.name) ||
        AdultCategoryBuilder.matchesGeneratedCategory(movie.categoryName) ||
        AdultCategoryBuilder.matchesGeneratedCategory(movie.genre) ||
        AdultCategoryBuilder.matchesGeneratedCategory(movie.name)

internal fun isAdultVodSeries(series: Series, category: Category? = null): Boolean =
    series.isAdult ||
        isAdultCategory(category) ||
        isAdultText(series.categoryName) ||
        isAdultText(series.genre) ||
        isAdultText(series.name) ||
        AdultCategoryBuilder.matchesGeneratedCategory(series.categoryName) ||
        AdultCategoryBuilder.matchesGeneratedCategory(series.genre) ||
        AdultCategoryBuilder.matchesGeneratedCategory(series.name)

internal fun isAdultLocalMediaItem(item: LocalMediaItem): Boolean =
    isAdultText(item.title) ||
        isAdultText(item.displayName) ||
        isAdultText(item.genre) ||
        isAdultText(item.description) ||
        AdultCategoryBuilder.matchesGeneratedCategory(item.title) ||
        AdultCategoryBuilder.matchesGeneratedCategory(item.displayName) ||
        AdultCategoryBuilder.matchesGeneratedCategory(item.genre) ||
        AdultCategoryBuilder.matchesGeneratedCategory(item.description)

private data class AdultMutableCategory(
    val key: String,
    val title: String,
    val channels: MutableList<Channel>
)

private data class AdultRule(
    val key: String,
    val title: String,
    val aliases: List<String>
) {
    private val normalizedAliases = aliases.map(::normalizeAdultText)

    fun matches(value: String?): Boolean {
        val normalized = normalizeAdultText(value)
        return matchesNormalized(normalized)
    }

    fun matchesNormalized(normalized: String): Boolean {
        if (normalized.isBlank()) return false
        return normalizedAliases.any { normalizedAlias ->
            normalized == normalizedAlias ||
                normalized.startsWith("$normalizedAlias ") ||
                normalized.endsWith(" $normalizedAlias") ||
                normalized.contains(" $normalizedAlias ")
        }
    }
}

private fun isAdultText(value: String?): Boolean {
    val normalized = normalizeAdultText(value)
    if (normalized.isBlank()) return false
    return normalized.containsWholeAdultTerm("xxx") ||
        normalized.containsWholeAdultTerm("adult") ||
        normalized.containsWholeAdultTerm("18 plus") ||
        normalized.containsWholeAdultTerm("x rated") ||
        normalized.containsWholeAdultTerm("porn")
}

private fun String.containsWholeAdultTerm(term: String): Boolean =
    this == term ||
        startsWith("$term ") ||
        endsWith(" $term") ||
        contains(" $term ")

private val adultNonAlphaNumericRegex = Regex("""[^a-z0-9]+""")
private val adultWhitespaceRegex = Regex("""\s+""")

private fun normalizeAdultText(value: String?): String =
    value
        .orEmpty()
        .lowercase(Locale.US)
        .replace("+", " plus ")
        .replace(adultNonAlphaNumericRegex, " ")
        .replace(adultWhitespaceRegex, " ")
        .trim()

private fun adultCategoryKey(value: String): String =
    normalizeAdultText(value).replace(" ", "_")
