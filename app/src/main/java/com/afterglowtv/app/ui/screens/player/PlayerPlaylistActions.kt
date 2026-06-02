@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.afterglowtv.app.ui.screens.player

import androidx.lifecycle.viewModelScope
import com.afterglowtv.app.ui.model.AdultGuideCategoryBuilder
import com.afterglowtv.app.ui.model.adultGuideCachedChannelIdsForCategory
import com.afterglowtv.app.ui.model.adultGuideCategoryId
import com.afterglowtv.app.ui.model.isAdultGuideChannel
import com.afterglowtv.app.ui.model.isAdultGuideGeneratedCategoryId
import com.afterglowtv.app.ui.model.orderedByRequestedRawIds
import com.afterglowtv.domain.model.Category
import com.afterglowtv.domain.model.Channel
import com.afterglowtv.domain.model.ChannelNumberingMode
import com.afterglowtv.domain.model.ContentType
import com.afterglowtv.domain.model.Favorite
import com.afterglowtv.domain.model.VirtualCategoryIds
import com.afterglowtv.domain.repository.ChannelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun PlayerViewModel.observeCombinedLivePlaylist(
    profileId: Long,
    categoryId: Long
): Flow<List<Channel>> = when {
    categoryId == ChannelRepository.ALL_CHANNELS_ID -> {
        combinedM3uRepository.getCombinedCategories(profileId).flatMapLatest { combinedCategories ->
            combinedCategoriesById = combinedCategories.associateBy { it.category.id }
            val flows = combinedCategories.map { combinedM3uRepository.getCombinedChannels(profileId, it) }
            if (flows.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(flows) { arrays ->
                    arrays.toList().flatMap { it }
                }.map(::applyCombinedSourceProviderFilter)
            }
        }
    }

    categoryId == VirtualCategoryIds.RECENT -> {
        combinedProviderIdsFlow(profileId)
            .flatMapLatest { providerIds -> observeRecentLiveIds(effectiveCombinedProviderIds(providerIds), 24) }
            .flatMapLatest { ids -> loadLiveChannelsByOrderedIds(ids, currentCombinedSourceFilterProviderId) }
    }

    categoryId == VirtualCategoryIds.FAVORITES -> {
        combinedProviderIdsFlow(profileId)
            .flatMapLatest { providerIds -> observeLiveFavorites(effectiveCombinedProviderIds(providerIds)) }
            .map { favorites -> favorites.sortedBy { it.position }.map { it.contentId } }
            .flatMapLatest { ids -> loadLiveChannelsByOrderedIds(ids, currentCombinedSourceFilterProviderId) }
    }

    categoryId == VirtualCategoryIds.ADULT_GUIDE || isAdultGuideGeneratedCategoryId(categoryId) -> {
        combinedM3uRepository.getCombinedCategories(profileId).flatMapLatest { combinedCategories ->
            combinedCategoriesById = combinedCategories.associateBy { it.category.id }
            val categoriesById = combinedCategories.associate { it.category.id to it.category }
            val flows = combinedCategories.map { combinedM3uRepository.getCombinedChannels(profileId, it) }
            if (flows.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(flows) { arrays ->
                    val adultChannels = arrays.toList()
                        .flatMap { it }
                        .distinctBy { channel -> channel.providerId to channel.id }
                        .filter { channel -> isAdultGuideChannel(channel, channel.categoryId?.let(categoriesById::get)) }
                    adultGuideChannelsForPlaybackCategory(
                        channels = adultChannels,
                        providerCategories = combinedCategories.map { it.category },
                        categoryId = categoryId
                    )
                }.map(::applyCombinedSourceProviderFilter)
            }
        }
    }

    categoryId < 0L -> {
        favoriteRepository.getFavoritesByGroup(-categoryId)
            .map { favorites ->
                favorites
                    .sortedBy { it.position }
                    .let { groupFavorites ->
                        currentCombinedSourceFilterProviderId?.let { selectedProviderId ->
                            groupFavorites.filter { it.providerId == selectedProviderId }
                        } ?: groupFavorites
                    }
                    .map { it.contentId }
            }
            .flatMapLatest { ids -> loadLiveChannelsByOrderedIds(ids, currentCombinedSourceFilterProviderId) }
    }

    else -> {
        combinedM3uRepository.getCombinedCategories(profileId).flatMapLatest { combinedCategories ->
            combinedCategoriesById = combinedCategories.associateBy { it.category.id }
            val combinedCategory = combinedCategoriesById[categoryId]
            if (combinedCategory == null) {
                flowOf(emptyList())
            } else {
                combinedM3uRepository.getCombinedChannels(profileId, combinedCategory)
                    .map(::applyCombinedSourceProviderFilter)
            }
        }
    }
}

internal fun adultGuideChannelsForPlaybackCategory(
    channels: List<Channel>,
    providerCategories: List<Category>,
    categoryId: Long
): List<Channel> {
    val categoriesById = providerCategories.associateBy(Category::id)
    val adultChannels = channels
        .filter { channel -> isAdultGuideChannel(channel, channel.categoryId?.let(categoriesById::get)) }
        .distinctBy(Channel::id)
    if (categoryId == VirtualCategoryIds.ADULT_GUIDE) return adultChannels
    if (!isAdultGuideGeneratedCategoryId(categoryId)) return emptyList()

    return AdultGuideCategoryBuilder.build(
        channels = adultChannels,
        providerCategories = providerCategories,
        includeAllCategory = false
    ).firstOrNull { category -> adultGuideCategoryId(category.key) == categoryId }
        ?.channels
        .orEmpty()
}

internal fun PlayerViewModel.observeAdultGuideCachedPlaylist(
    providerId: Long,
    categoryId: Long
): Flow<List<Channel>> {
    if (categoryId != VirtualCategoryIds.ADULT_GUIDE && !isAdultGuideGeneratedCategoryId(categoryId)) {
        return flowOf(emptyList())
    }

    return adultGuideCacheRepository.observeProviderCache(providerId)
        .flatMapLatest { cache ->
            val ids = adultGuideCachedChannelIdsForCategory(cache, categoryId)
            if (ids.isNullOrEmpty()) {
                flowOf(emptyList())
            } else {
                channelRepository.getChannelsByIds(ids).map { channels ->
                    channels.orderedByRequestedRawIds(ids)
                }
            }
        }
}

internal fun PlayerViewModel.observeRecentChannels() {
    recentChannelsJob?.cancel()
    if (currentContentType != ContentType.LIVE || (currentProviderId <= 0 && currentCombinedProfileId == null)) {
        recentChannelsFlow.value = emptyList()
        return
    }

    recentChannelsJob = viewModelScope.launch {
        val recentFlow = currentCombinedProfileId?.let { profileId ->
            combinedProviderIdsFlow(profileId)
                .flatMapLatest { providerIds -> observeRecentLiveIds(effectiveCombinedProviderIds(providerIds), 12) }
                .flatMapLatest { ids -> loadLiveChannelsByOrderedIds(ids, currentCombinedSourceFilterProviderId) }
        } ?: playbackHistoryRepository.getRecentlyWatchedByProvider(currentProviderId, limit = 12)
            .map { history ->
                history.asSequence()
                    .filter { it.contentType == ContentType.LIVE }
                    .sortedByDescending { it.lastWatchedAt }
                    .distinctBy { it.contentId }
                    .map { it.contentId }
                    .toList()
            }
            .flatMapLatest { ids -> loadLiveChannelsByOrderedIds(ids) }

        combine(recentFlow, preferencesRepository.liveChannelNumberingMode) { channels, numberingMode ->
            numberingMode to channels
        }.collect { (numberingMode, channels) ->
            channelNumberingMode = numberingMode
            val currentListNumbers = channelList.withIndex().associate { (index, channel) ->
                channel.id to resolveChannelNumber(channel, index)
            }
            recentChannelsFlow.value = channels
                .filterNot { it.id == currentContentId }
                .map { channel ->
                    currentListNumbers[channel.id]?.let { number ->
                        channel.copy(number = number)
                    } ?: channel
                }
        }
    }
}

internal fun PlayerViewModel.observeLastVisitedCategory() {
    lastVisitedCategoryJob?.cancel()
    if (currentContentType != ContentType.LIVE || (currentProviderId <= 0 && currentCombinedProfileId == null)) {
        _lastVisitedCategory.value = null
        return
    }

    lastVisitedCategoryJob = viewModelScope.launch {
        val categoriesFlow = currentCombinedProfileId?.let { profileId ->
            combinedProviderIdsFlow(profileId).flatMapLatest { providerIds ->
                combine(
                    combinedM3uRepository.getCombinedCategories(profileId),
                    getCustomCategories(providerIds, ContentType.LIVE)
                ) { combinedCategories, customCategories ->
                    combinedCategoriesById = combinedCategories.associateBy { it.category.id }
                    buildCombinedLiveCategories(combinedCategories, customCategories) to null
                }
            }
        } ?: combine(
            channelRepository.getCategories(currentProviderId),
            getCustomCategories(currentProviderId, ContentType.LIVE),
            preferencesRepository.getLastLiveCategoryId(currentProviderId),
            preferencesRepository.getHiddenCategoryIds(currentProviderId, ContentType.LIVE)
        ) { providerCategories, customCategories, lastVisitedCategoryId, hiddenCategoryIds ->
            val visibleProviderCategories = providerCategories.filter { category ->
                category.id == ChannelRepository.ALL_CHANNELS_ID || category.id !in hiddenCategoryIds
            }
            val adjustedProviderCategories = visibleProviderCategories.map { category ->
                if (category.id == ChannelRepository.ALL_CHANNELS_ID) {
                    category.copy(
                        count = providerCategories
                            .filter { it.id != ChannelRepository.ALL_CHANNELS_ID && it.id !in hiddenCategoryIds }
                            .sumOf(Category::count)
                    )
                } else {
                    category
                }
            }
            val allCategories = customCategories + adjustedProviderCategories
            val lastVisited = if (lastVisitedCategoryId == null || lastVisitedCategoryId == VirtualCategoryIds.RECENT) {
                null
            } else {
                allCategories.firstOrNull { it.id == lastVisitedCategoryId }
            }
            allCategories to lastVisited
        }

        categoriesFlow.collect { (allCategories, lastVisited) ->
            availableCategoriesFlow.value = allCategories
            _lastVisitedCategory.value = lastVisited
        }
    }
}

fun PlayerViewModel.openLastVisitedCategory() {
    val category = _lastVisitedCategory.value ?: return
    if (currentContentType != ContentType.LIVE) return

    currentCategoryId = category.id
    activeCategoryIdFlow.value = category.id
    isVirtualCategory = category.isVirtual
    loadPlaylist(
        categoryId = category.id,
        providerId = currentProviderId,
        isVirtual = category.isVirtual,
        initialChannelId = currentContentId
    )
    openChannelListOverlay()
}

internal fun PlayerViewModel.loadPlaylist(
    categoryId: Long,
    providerId: Long,
    isVirtual: Boolean,
    initialChannelId: Long
) {
    playlistJob?.cancel()
    playlistJob = viewModelScope.launch {
        val flows = currentCombinedProfileId?.let { profileId ->
            observeCombinedLivePlaylist(profileId, categoryId)
        } ?: if (isVirtual) {
            if (categoryId == VirtualCategoryIds.RECENT) {
                playbackHistoryRepository.getRecentlyWatchedByProvider(providerId, limit = 24)
                    .map { history ->
                        history.asSequence()
                            .filter { it.contentType == ContentType.LIVE }
                            .sortedByDescending { it.lastWatchedAt }
                            .distinctBy { it.contentId }
                            .map { it.contentId }
                            .toList()
                    }
                    .flatMapLatest { ids ->
                        if (ids.isEmpty()) flowOf(emptyList())
                        else channelRepository.getChannelsByIds(ids).map { unsorted ->
                            unsorted.orderedByRequestedRawIds(ids)
                        }
                    }
            } else if (categoryId == VirtualCategoryIds.FAVORITES) {
                favoriteRepository.getFavorites(currentProviderId, ContentType.LIVE)
                    .map { favorites -> favorites.sortedBy { it.position }.map { it.contentId } }
                    .flatMapLatest { ids ->
                        if (ids.isEmpty()) flowOf(emptyList())
                        else channelRepository.getChannelsByIds(ids).map { unsorted ->
                            unsorted.orderedByRequestedRawIds(ids)
                        }
                    }
            } else if (categoryId == VirtualCategoryIds.ADULT_GUIDE || isAdultGuideGeneratedCategoryId(categoryId)) {
                observeAdultGuideCachedPlaylist(providerId, categoryId)
            } else {
                val groupId = if (categoryId < 0) -categoryId else categoryId
                favoriteRepository.getFavoritesByGroup(groupId)
                    .map { favorites -> favorites.sortedBy { it.position }.map { it.contentId } }
                    .flatMapLatest { ids ->
                        if (ids.isEmpty()) flowOf(emptyList())
                        else channelRepository.getChannelsByIds(ids).map { unsorted ->
                            unsorted.orderedByRequestedRawIds(ids)
                        }
                    }
            }
        } else {
            channelRepository.getChannelsByNumber(providerId, categoryId)
        }

        combine(flows, preferencesRepository.liveChannelNumberingMode) { channels, numberingMode ->
            val displayedChannels = when (numberingMode) {
                ChannelNumberingMode.GROUP -> channels.mapIndexed { index, channel ->
                    channel.copy(number = index + 1)
                }
                ChannelNumberingMode.PROVIDER -> channels
                ChannelNumberingMode.HIDDEN -> channels.map { it.copy(number = 0) }
            }
            numberingMode to displayedChannels.sanitizedChannelsForPlayer()
        }.collect { (numberingMode, displayedChannels) ->
            channelNumberingMode = numberingMode
            channelList = displayedChannels
            currentChannelFlowList.value = displayedChannels
            val targetId = if (currentContentId != -1L) currentContentId else initialChannelId
            if (targetId != -1L) {
                currentChannelIndex = channelList.indexOfFirst { it.id == targetId }
            }
            if (currentChannelIndex == -1) {
                currentChannelIndex = channelList.indexOfFirst { it.streamUrl == currentStreamUrl }
            }

            if (currentChannelIndex != -1) {
                currentChannelFlow.value = channelList[currentChannelIndex].sanitizedForPlayer()
                refreshCurrentChannelRecording()
                val channel = channelList[currentChannelIndex]
                displayChannelNumberFlow.value = resolveChannelNumber(channel, currentChannelIndex)
            } else if (displayedChannels.isNotEmpty() && currentContentId != -1L) {
                showPlayerNotice(
                    message = "Channel not found in current playlist",
                    recoveryType = PlayerRecoveryType.SOURCE,
                    actions = listOf(PlayerNoticeAction.OPEN_GUIDE)
                )
            }
        }
    }
}

internal fun PlayerViewModel.loadMoviePlaylist(
    categoryId: Long,
    providerId: Long,
    initialMovieId: Long
) {
    playlistJob?.cancel()
    playlistJob = viewModelScope.launch {
        val moviesFlow = if (categoryId == ChannelRepository.ALL_CHANNELS_ID || categoryId <= 0L) {
            movieRepository.getMovies(providerId)
        } else {
            movieRepository.getMoviesByCategory(providerId, categoryId)
        }
        moviesFlow.collect { movies ->
            movieList = movies
            val targetId = if (currentContentId != -1L) currentContentId else initialMovieId
            currentMovieIndex = if (targetId != -1L) {
                movieList.indexOfFirst { it.id == targetId }
            } else {
                -1
            }
            if (currentMovieIndex == -1) {
                currentMovieIndex = movieList.indexOfFirst { it.streamUrl == currentStreamUrl }
            }
        }
    }
}

internal fun PlayerViewModel.playMovieOffset(offset: Int): Boolean {
    if (currentContentType != ContentType.MOVIE || movieList.isEmpty()) return false
    val resolvedIndex = currentMovieIndex.takeIf { it in movieList.indices }
        ?: movieList.indexOfFirst { it.id == currentContentId }
            .takeIf { it in movieList.indices }
        ?: movieList.indexOfFirst { it.streamUrl == currentStreamUrl }
            .takeIf { it in movieList.indices }
        ?: return false
    val nextIndex = computeWrappedChannelIndex(
        resolvedIndex = resolvedIndex,
        channelCount = movieList.size,
        offset = offset
    )
    if (nextIndex == -1) return false
    changeMovie(nextIndex)
    return true
}

internal fun PlayerViewModel.changeMovie(index: Int) {
    check(index in movieList.indices) {
        "changeMovie index=$index out of movieList bounds (size=${movieList.size})"
    }
    clearNumericChannelInput()
    val requestVersion = beginPlaybackSession()
    val movie = movieList[index]
    currentMovieIndex = index
    currentContentType = ContentType.MOVIE
    currentContentId = movie.id
    currentProviderId = movie.providerId
    currentTitle = movie.name
    playbackTitleFlow.value = currentTitle
    currentStreamUrl = movie.streamUrl
    currentArtworkUrl = movie.posterUrl ?: movie.backdropUrl
    currentChannelFlow.value = null
    displayChannelNumberFlow.value = index + 1
    showControlsFlow.value = false

    viewModelScope.launch {
        val streamInfo = withContext(Dispatchers.Default) {
            resolvePlaybackStreamInfo(movie.streamUrl, movie.id, movie.providerId, ContentType.MOVIE)
        } ?: return@launch
        if (!isActivePlaybackSession(requestVersion, movie.streamUrl)) return@launch
        if (!preparePlayer(streamInfo, requestVersion)) return@launch
        playerEngine.play()
    }

    showChannelInfoOverlayFlow.value = false
    showChannelListOverlayFlow.value = false
    showCategoryListOverlayFlow.value = false
    showEpgOverlayFlow.value = false
    showZapOverlayFlow.value = true
    hideZapOverlayAfterDelay()
}

private fun PlayerViewModel.applyCombinedSourceProviderFilter(channels: List<Channel>): List<Channel> {
    val selectedProviderId = currentCombinedSourceFilterProviderId ?: return channels
    return channels.filter { it.providerId == selectedProviderId }
}

private fun PlayerViewModel.loadLiveChannelsByOrderedIds(
    ids: List<Long>,
    providerId: Long? = null
): Flow<List<Channel>> = if (ids.isEmpty()) {
    flowOf(emptyList())
} else {
    channelRepository.getChannelsByIds(ids).map { unsorted ->
        val filtered = providerId?.let { requiredProviderId ->
            unsorted.filter { it.providerId == requiredProviderId }
        } ?: unsorted
        filtered.orderedByRequestedRawIds(ids)
    }
}

private fun PlayerViewModel.combinedProviderIdsFlow(profileId: Long): Flow<List<Long>> = flow {
    emit(combinedM3uRepository.getProfile(profileId)?.members.orEmpty())
}.map { members ->
    currentCombinedProfileMembers = members
    members.filter { it.enabled }.map { it.providerId }
}

private fun PlayerViewModel.effectiveCombinedProviderIds(providerIds: List<Long>): List<Long> =
    currentCombinedSourceFilterProviderId?.let { selectedProviderId ->
        providerIds.filter { it == selectedProviderId }
    } ?: providerIds

private fun PlayerViewModel.observeLiveFavorites(providerIds: List<Long>): Flow<List<Favorite>> = when (providerIds.size) {
    0 -> flowOf(emptyList())
    1 -> favoriteRepository.getFavorites(providerIds.first(), ContentType.LIVE)
    else -> favoriteRepository.getFavorites(providerIds, ContentType.LIVE)
}

private fun PlayerViewModel.observeRecentLiveIds(providerIds: List<Long>, limit: Int): Flow<List<Long>> = when (providerIds.size) {
    0 -> flowOf(emptyList())
    1 -> playbackHistoryRepository.getRecentlyWatchedByProvider(providerIds.first(), limit)
        .map { history ->
            history.asSequence()
                .filter { it.contentType == ContentType.LIVE }
                .sortedByDescending { it.lastWatchedAt }
                .distinctBy { it.contentId }
                .map { it.contentId }
                .take(limit)
                .toList()
        }

    else -> combine(providerIds.map { providerId ->
        playbackHistoryRepository.getRecentlyWatchedByProvider(providerId, limit)
    }) { histories ->
        histories.toList()
            .flatMap { it }
            .asSequence()
            .filter { it.contentType == ContentType.LIVE }
            .sortedByDescending { it.lastWatchedAt }
            .distinctBy { it.providerId to it.contentId }
            .map { it.contentId }
            .take(limit)
            .toList()
    }
}

private fun buildCombinedLiveCategories(
    combinedCategories: List<com.afterglowtv.domain.model.CombinedCategory>,
    customCategories: List<Category>
): List<Category> = buildList {
    val favoritesCategory = customCategories.find { it.id == VirtualCategoryIds.FAVORITES }
    if (favoritesCategory != null) {
        add(favoritesCategory)
    }
    add(
        Category(
            id = VirtualCategoryIds.RECENT,
            name = "Recent",
            type = ContentType.LIVE,
            isVirtual = true,
            count = 0
        )
    )
    addAll(customCategories.filter { it.id != VirtualCategoryIds.FAVORITES })
    add(
        Category(
            id = ChannelRepository.ALL_CHANNELS_ID,
            name = "All Channels",
            type = ContentType.LIVE,
            count = combinedCategories.sumOf { it.category.count }
        )
    )
    addAll(combinedCategories.map { it.category })
}
