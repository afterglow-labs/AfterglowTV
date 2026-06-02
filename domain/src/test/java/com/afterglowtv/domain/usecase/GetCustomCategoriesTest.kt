package com.afterglowtv.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.afterglowtv.domain.model.ContentType
import com.afterglowtv.domain.model.Favorite
import com.afterglowtv.domain.model.VirtualCategoryIds
import com.afterglowtv.domain.model.VirtualGroup
import com.afterglowtv.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GetCustomCategoriesTest {

    @Test
    fun mergesCustomCategoriesAcrossProviders() = runTest {
        val useCase = GetCustomCategories(
            favoriteRepository = FakeFavoriteRepository(
                favorites = listOf(
                    Favorite(providerId = 1L, contentId = 101L, contentType = ContentType.LIVE, groupId = null),
                    Favorite(providerId = 1L, contentId = 102L, contentType = ContentType.LIVE, groupId = 10L),
                    Favorite(providerId = 2L, contentId = 201L, contentType = ContentType.LIVE, groupId = 20L),
                    Favorite(providerId = 2L, contentId = 202L, contentType = ContentType.LIVE, groupId = 20L)
                ),
                groups = listOf(
                    VirtualGroup(id = 10L, providerId = 1L, name = "Sports", contentType = ContentType.LIVE),
                    VirtualGroup(id = 20L, providerId = 2L, name = "Kids", contentType = ContentType.LIVE)
                )
            )
        )

        val result = useCase(listOf(1L, 2L), ContentType.LIVE).first()

        assertThat(result).hasSize(3)
        assertThat(result[0].id).isEqualTo(VirtualCategoryIds.FAVORITES)
        assertThat(result[0].count).isEqualTo(1)
        assertThat(result.drop(1).map { it.name to it.count })
            .containsExactly("Sports" to 1, "Kids" to 2)
    }

    @Test
    fun returnsEmptyListForNoProviders() = runTest {
        val useCase = GetCustomCategories(FakeFavoriteRepository())

        val result = useCase(emptyList(), ContentType.LIVE).first()

        assertThat(result).isEmpty()
    }

    private class FakeFavoriteRepository(
        private val favorites: List<Favorite> = emptyList(),
        private val groups: List<VirtualGroup> = emptyList()
    ) : FavoriteRepository {
        override fun getFavorites(providerId: Long, contentType: ContentType?): Flow<List<Favorite>> =
            getFavorites(listOf(providerId), contentType)

        override fun getFavorites(providerIds: List<Long>, contentType: ContentType?): Flow<List<Favorite>> =
            flowOf(
                favorites.filter { favorite ->
                    favorite.providerId in providerIds && (contentType == null || favorite.contentType == contentType)
                }
            )

        override fun getFavoritesIncludingGroups(providerId: Long, contentType: ContentType): Flow<List<Favorite>> =
            flowOf(
                favorites.filter { favorite ->
                    favorite.providerId == providerId && favorite.contentType == contentType
                }
            )

        override fun getFavoritesByGroup(groupId: Long): Flow<List<Favorite>> =
            flowOf(favorites.filter { it.groupId == groupId })

        override fun getGroups(providerId: Long, contentType: ContentType): Flow<List<VirtualGroup>> =
            getGroups(listOf(providerId), contentType)

        override fun getGroups(providerIds: List<Long>, contentType: ContentType): Flow<List<VirtualGroup>> =
            flowOf(groups.filter { group -> group.providerId in providerIds && group.contentType == contentType })

        override fun getGlobalFavoriteCount(providerId: Long, contentType: ContentType): Flow<Int> =
            error("Not used in test")

        override fun getGroupFavoriteCounts(providerId: Long, contentType: ContentType): Flow<Map<Long, Int>> =
            error("Not used in test")

        override fun getGroupFavoriteCounts(providerIds: List<Long>, contentType: ContentType): Flow<Map<Long, Int>> =
            error("Not used in test")

        override suspend fun addFavorite(providerId: Long, contentId: Long, contentType: ContentType, groupId: Long?) =
            error("Not used in test")

        override suspend fun removeFavorite(providerId: Long, contentId: Long, contentType: ContentType, groupId: Long?) =
            error("Not used in test")

        override suspend fun moveFavoriteToGroup(
            providerId: Long,
            contentId: Long,
            contentType: ContentType,
            fromGroupId: Long?,
            targetGroupId: Long?
        ) = error("Not used in test")

        override suspend fun mergeGroupInto(sourceGroupId: Long, targetGroupId: Long) =
            error("Not used in test")

        override suspend fun reorderFavorites(favorites: List<Favorite>) = error("Not used in test")

        override suspend fun isFavorite(providerId: Long, contentId: Long, contentType: ContentType): Boolean =
            error("Not used in test")

        override suspend fun getGroupMemberships(providerId: Long, contentId: Long, contentType: ContentType): List<Long> =
            error("Not used in test")

        override suspend fun createGroup(providerId: Long, name: String, iconEmoji: String?, contentType: ContentType) =
            error("Not used in test")

        override suspend fun deleteGroup(groupId: Long) = error("Not used in test")

        override suspend fun renameGroup(groupId: Long, newName: String) = error("Not used in test")
    }
}