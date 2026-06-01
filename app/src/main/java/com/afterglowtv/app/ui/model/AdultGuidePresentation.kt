package com.afterglowtv.app.ui.model

import com.afterglowtv.domain.model.Category
import com.afterglowtv.domain.model.Channel
import com.afterglowtv.domain.model.LocalMediaItem
import com.afterglowtv.domain.model.Movie
import com.afterglowtv.domain.model.Series
import java.util.Locale

data class AdultGuideCategory(
    val key: String,
    val title: String,
    val channels: List<Channel>
)

object AdultGuideCategoryBuilder {
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
    private val categoryRules = listOf(
        AdultGuideRule(
            "milf",
            "MILF",
            listOf("milf", "milfs", "milfy", "stepmom", "step mom"),
            embeddedAliases = listOf("milf", "milfy")
        ),
        AdultGuideRule(
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
                "family",
                "teacher",
                "student",
                "school",
                "schoolgirl",
                "school girl",
                "college",
                "cousin",
                "aunt",
                "uncle",
                "mom",
                "mother",
                "dad",
                "father",
                "sister",
                "brother"
            ),
            embeddedAliases = listOf(
                "stepmom",
                "stepdad",
                "stepsister",
                "stepbrother",
                "family",
                "teacher",
                "student",
                "schoolgirl",
                "cousin",
                "mother",
                "father"
            )
        ),
        AdultGuideRule("interracial", "Interracial", listOf("interracial", "ir")),
        AdultGuideRule(
            "teen",
            "Teen",
            listOf("teen", "teens", "eighteen", "18 plus", "barely legal", "nubile"),
            embeddedAliases = listOf("teen", "teens", "realteens", "eighteen", "barelylegal", "nubile")
        ),
        AdultGuideRule(
            "anal",
            "Anal",
            listOf("anal", "anal 4k", "anal only", "anal mom", "analized"),
            embeddedAliases = listOf("anal", "anal4k", "analonly", "analmom", "analized")
        ),
        AdultGuideRule(
            "blowjob",
            "Blowjob",
            listOf("blowjob", "blowjobs", "bj", "deep throat", "deepthroat"),
            embeddedAliases = listOf("blowjob", "blowjobs", "deepthroat")
        ),
        AdultGuideRule("oral", "Oral", listOf("oral"), embeddedAliases = listOf("oral")),
        AdultGuideRule(
            "big_tits",
            "Big Tits",
            listOf("big tits", "big titty", "bigtitty", "busty", "boobs"),
            embeddedAliases = listOf("bigtits", "bigtitty", "busty")
        ),
        AdultGuideRule("hardcore", "Hardcore", listOf("hardcore", "rough"), embeddedAliases = listOf("hardcore")),
        AdultGuideRule("bondage", "Bondage", listOf("bondage", "bdsm"), embeddedAliases = listOf("bondage", "bdsm")),
        AdultGuideRule("babes", "Babes", listOf("babes", "babe"), embeddedAliases = listOf("babes")),
        AdultGuideRule("cuckold", "Cuckold", listOf("cuckold", "cuck"), embeddedAliases = listOf("cuckold")),
        AdultGuideRule(
            "compilation",
            "Compilation",
            listOf("compilation", "compilations"),
            embeddedAliases = listOf("compilation", "compilations")
        ),
        AdultGuideRule("parody", "Parody", listOf("parody", "parodies"), embeddedAliases = listOf("parody")),
        AdultGuideRule("pornstar", "Pornstar", listOf("pornstar", "pornstars"), embeddedAliases = listOf("pornstar")),
        AdultGuideRule("cartoon", "Cartoon", listOf("cartoon", "cartoons", "toon", "animated"), embeddedAliases = listOf("cartoon")),
        AdultGuideRule("hentai", "Hentai", listOf("hentai", "anime"), embeddedAliases = listOf("hentai", "anime")),
        AdultGuideRule("solo", "Solo", listOf("solo", "solo girl", "solo girls"), embeddedAliases = listOf("solo")),
        AdultGuideRule("onlyfans", "OnlyFans", listOf("onlyfans", "only fans"), embeddedAliases = listOf("onlyfans")),
        AdultGuideRule("tiktok", "TikTok", listOf("tiktok", "tik tok"), embeddedAliases = listOf("tiktok")),
        AdultGuideRule("blondes", "Blonde", listOf("blonde", "blondes"), embeddedAliases = listOf("blonde", "blondessa")),
        AdultGuideRule("brunettes", "Brunettes", listOf("brunette", "brunettes")),
        AdultGuideRule("trans", "Trans", listOf("trans", "transgender", "tg", "ts")),
        AdultGuideRule("latina", "Latina", listOf("latina", "latinas", "latin")),
        AdultGuideRule(
            "asian",
            "Asian",
            listOf("asian", "japanese", "korean", "chinese", "thai", "japan", "japanhdv"),
            embeddedAliases = listOf("asian", "japanese", "korean", "chinese", "japanhdv")
        ),
        AdultGuideRule("ebony", "Ebony", listOf("ebony", "black")),
        AdultGuideRule("amateur", "Amateur", listOf("amateur", "homemade")),
        AdultGuideRule("lesbian", "Lesbian", listOf("lesbian", "lesbians")),
        AdultGuideRule("gay", "Gay", listOf("gay")),
        AdultGuideRule("bbw", "BBW", listOf("bbw")),
        AdultGuideRule("mature", "Mature", listOf("mature", "cougar", "cougars")),
        AdultGuideRule("pov", "POV", listOf("pov")),
        AdultGuideRule("group", "Group", listOf("group", "threesome", "orgy")),
        AdultGuideRule("fetish", "Fetish", listOf("fetish", "bdsm", "bondage")),
        AdultGuideRule("reality", "Reality", listOf("reality", "casting", "audition")),
        AdultGuideRule("vr", "VR", listOf("vr", "virtual reality")),
        AdultGuideRule("4k", "4K", listOf("4k", "uhd"))
    )

    fun build(
        channels: List<Channel>,
        providerCategories: List<Category>,
        includeAllCategory: Boolean = true
    ): List<AdultGuideCategory> {
        if (channels.isEmpty()) return emptyList()

        val providerCategoryById = providerCategories.associateBy { it.id }
        val grouped = linkedMapOf<String, AdultGuideMutableCategory>()
        if (includeAllCategory) {
            grouped[ALL_CATEGORY_KEY] = AdultGuideMutableCategory(ALL_CATEGORY_KEY, "All", channels.toMutableList())
        }

        channels.forEach { channel ->
            val titleMatches = categoryRules.filter { rule ->
                rule.matches(channel.name)
            }
            val contextMatches = categoryRules.filter { rule ->
                rule !in titleMatches &&
                    (rule.matches(channel.categoryName) || rule.matches(channel.groupTitle))
            }
            val matches = titleMatches + contextMatches
            if (matches.isEmpty()) {
                val sourceCategory = channel.categoryId?.let(providerCategoryById::get)
                if (isAdultGuideChannel(channel, sourceCategory)) {
                    val fallbackCategory = sourceCategory?.takeIf(::isAdultGuideCategory)
                        ?: channel.categoryName
                            ?.takeIf(::isAdultGuideText)
                            ?.let { Category(id = Long.MIN_VALUE, name = it, isAdult = true) }
                    val key = fallbackCategory?.name
                        ?.let(::adultGuideCategoryKey)
                        ?.takeIf(String::isNotBlank)
                        ?: OTHER_CATEGORY_KEY
                    val title = fallbackCategory?.name
                        ?.trim()
                        ?.takeIf(String::isNotBlank)
                        ?: "Other"
                    grouped.getOrPut(key) {
                        AdultGuideMutableCategory(key, title, mutableListOf())
                    }.channels.add(channel)
                }
            } else {
                matches.forEach { rule ->
                    grouped.getOrPut(rule.key) {
                        AdultGuideMutableCategory(rule.key, rule.title, mutableListOf())
                    }.channels.add(channel)
                }
            }
        }

        return grouped.values
            .map { category ->
                AdultGuideCategory(
                    key = category.key,
                    title = category.title,
                    channels = category.channels.distinctBy { it.id }
                )
            }
            .filter { it.channels.isNotEmpty() }
    }

    fun matchesGeneratedCategory(value: String?): Boolean =
        categoryRules.any { it.matches(value) }

    fun resolveVodCategoryTitle(title: String?, providerCategory: String?): String? {
        categoryRules.firstOrNull { rule -> rule.matches(title) }?.let { rule ->
            return rule.title
        }
        providerCategory
            ?.trim()
            ?.takeIf { it.isNotBlank() && (isAdultGuideText(it) || matchesExplicitAdultSignal(it)) }
            ?.let { return it }
        categoryRules.firstOrNull { rule -> rule.matches(providerCategory) }?.let { rule ->
            return rule.title
        }
        return providerCategory?.trim()?.takeIf(String::isNotBlank)
    }

    fun matchesExplicitAdultSignal(value: String?): Boolean {
        val normalized = normalizeAdultGuideText(value)
        if (normalized.isBlank()) return false
        return explicitAdultSignals.any { signal ->
            val normalizedSignal = normalizeAdultGuideText(signal)
            normalized == normalizedSignal ||
                normalized.startsWith("$normalizedSignal ") ||
                normalized.endsWith(" $normalizedSignal") ||
                normalized.contains(" $normalizedSignal ")
        }
    }
}

internal fun isAdultGuideCategory(category: Category?): Boolean {
    if (category == null) return false
    return category.isAdult || isAdultGuideText(category.name)
}

internal fun isLikelyAdultGuideCategory(category: Category?): Boolean {
    if (category == null) return false
    return isAdultGuideCategory(category) || AdultGuideCategoryBuilder.matchesExplicitAdultSignal(category.name)
}

internal fun isAdultGuideChannel(channel: Channel, category: Category? = null): Boolean {
    return channel.isAdult ||
        isLikelyAdultGuideCategory(category) ||
        isAdultGuideText(channel.categoryName) ||
        isAdultGuideText(channel.groupTitle) ||
        isAdultGuideText(channel.name) ||
        AdultGuideCategoryBuilder.matchesGeneratedCategory(channel.categoryName) ||
        AdultGuideCategoryBuilder.matchesGeneratedCategory(channel.groupTitle) ||
        AdultGuideCategoryBuilder.matchesGeneratedCategory(channel.name)
}

internal fun isExplicitAdultGuideChannel(channel: Channel): Boolean {
    return channel.isAdult ||
        isAdultGuideText(channel.categoryName) ||
        isAdultGuideText(channel.groupTitle) ||
        isAdultGuideText(channel.name) ||
        AdultGuideCategoryBuilder.matchesExplicitAdultSignal(channel.categoryName) ||
        AdultGuideCategoryBuilder.matchesExplicitAdultSignal(channel.groupTitle) ||
        AdultGuideCategoryBuilder.matchesExplicitAdultSignal(channel.name)
}

internal fun isAdultVodMovie(movie: Movie, category: Category? = null): Boolean =
    movie.isAdult ||
        isAdultGuideCategory(category) ||
        isAdultGuideText(movie.categoryName) ||
        isAdultGuideText(movie.genre) ||
        isAdultGuideText(movie.name) ||
        AdultGuideCategoryBuilder.matchesGeneratedCategory(movie.categoryName) ||
        AdultGuideCategoryBuilder.matchesGeneratedCategory(movie.genre) ||
        AdultGuideCategoryBuilder.matchesGeneratedCategory(movie.name)

internal fun isAdultVodSeries(series: Series, category: Category? = null): Boolean =
    series.isAdult ||
        isAdultGuideCategory(category) ||
        isAdultGuideText(series.categoryName) ||
        isAdultGuideText(series.genre) ||
        isAdultGuideText(series.name) ||
        AdultGuideCategoryBuilder.matchesGeneratedCategory(series.categoryName) ||
        AdultGuideCategoryBuilder.matchesGeneratedCategory(series.genre) ||
        AdultGuideCategoryBuilder.matchesGeneratedCategory(series.name)

internal fun isAdultLocalMediaItem(item: LocalMediaItem): Boolean =
    isAdultGuideText(item.title) ||
        isAdultGuideText(item.displayName) ||
        isAdultGuideText(item.genre) ||
        isAdultGuideText(item.description) ||
        AdultGuideCategoryBuilder.matchesGeneratedCategory(item.title) ||
        AdultGuideCategoryBuilder.matchesGeneratedCategory(item.displayName) ||
        AdultGuideCategoryBuilder.matchesGeneratedCategory(item.genre) ||
        AdultGuideCategoryBuilder.matchesGeneratedCategory(item.description)

private data class AdultGuideMutableCategory(
    val key: String,
    val title: String,
    val channels: MutableList<Channel>
)

private data class AdultGuideRule(
    val key: String,
    val title: String,
    val aliases: List<String>,
    val embeddedAliases: List<String> = emptyList()
) {
    fun matches(value: String?): Boolean {
        val normalized = normalizeAdultGuideText(value)
        if (normalized.isBlank()) return false
        val compact = normalized.replace(" ", "")
        return aliases.any { alias ->
            val normalizedAlias = normalizeAdultGuideText(alias)
            normalized == normalizedAlias ||
                normalized.startsWith("$normalizedAlias ") ||
                normalized.endsWith(" $normalizedAlias") ||
                normalized.contains(" $normalizedAlias ")
        } || embeddedAliases.any { alias ->
            val compactAlias = normalizeAdultGuideText(alias).replace(" ", "")
            compactAlias.isNotBlank() && compact.contains(compactAlias)
        }
    }
}

private fun isAdultGuideText(value: String?): Boolean {
    val normalized = normalizeAdultGuideText(value)
    if (normalized.isBlank()) return false
    return normalized.containsWholeAdultGuideTerm("xxx") ||
        normalized.containsWholeAdultGuideTerm("adult") ||
        normalized.containsWholeAdultGuideTerm("18 plus") ||
        normalized.containsWholeAdultGuideTerm("x rated") ||
        normalized.containsWholeAdultGuideTerm("porn")
}

private fun String.containsWholeAdultGuideTerm(term: String): Boolean =
    this == term ||
        startsWith("$term ") ||
        endsWith(" $term") ||
        contains(" $term ")

private fun normalizeAdultGuideText(value: String?): String =
    value
        .orEmpty()
        .lowercase(Locale.US)
        .replace("+", " plus ")
        .replace(Regex("""[^a-z0-9]+"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()

private fun adultGuideCategoryKey(value: String): String =
    normalizeAdultGuideText(value).replace(" ", "_")
