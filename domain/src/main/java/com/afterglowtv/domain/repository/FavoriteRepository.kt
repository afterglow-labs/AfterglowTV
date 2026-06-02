package com.afterglowtv.domain.repository

import com.afterglowtv.domain.model.ContentType
import com.afterglowtv.domain.model.Favorite
import com.afterglowtv.domain.model.Result
import com.afterglowtv.domain.model.VirtualGroup
import kotlinx.coroutines.flow.Flow

interface FavoriteRepository {
    fun getFavorites(providerId: Long, contentType: ContentType? = null): Flow<List<Favorite>>
    fun getFavorites(providerIds: List<Long>, contentType: ContentType? = null): Flow<List<Favorite>>
    /**
     * Returns every favorite for the provider/content type, including those assigned to a
     * virtual group. Use this when group membership matters (e.g. VOD group counts); use
     * [getFavorites] when only ungrouped "global" favorites are needed.
     */
    fun getFavoritesIncludingGroups(providerId: Long, contentType: ContentType): Flow<List<Favorite>>
    fun getFavoritesByGroup(groupId: Long): Flow<List<Favorite>>
    fun getGroups(providerId: Long, contentType: ContentType): Flow<List<VirtualGroup>>
    fun getGroups(providerIds: List<Long>, contentType: ContentType): Flow<List<VirtualGroup>>

    fun getGlobalFavoriteCount(providerId: Long, contentType: ContentType): Flow<Int>
    fun getGroupFavoriteCounts(providerId: Long, contentType: ContentType): Flow<Map<Long, Int>>
    fun getGroupFavoriteCounts(providerIds: List<Long>, contentType: ContentType): Flow<Map<Long, Int>>

    suspend fun addFavorite(providerId: Long, contentId: Long, contentType: ContentType, groupId: Long? = null): Result<Unit>
    suspend fun removeFavorite(providerId: Long, contentId: Long, contentType: ContentType, groupId: Long? = null): Result<Unit>
    suspend fun moveFavoriteToGroup(
        providerId: Long,
        contentId: Long,
        contentType: ContentType,
        fromGroupId: Long?,
        targetGroupId: Long?
    ): Result<Unit>
    suspend fun mergeGroupInto(sourceGroupId: Long, targetGroupId: Long): Result<Unit>
    
    suspend fun reorderFavorites(favorites: List<Favorite>): Result<Unit>
    suspend fun isFavorite(providerId: Long, contentId: Long, contentType: ContentType): Boolean
    
    suspend fun getGroupMemberships(providerId: Long, contentId: Long, contentType: ContentType): List<Long>

    suspend fun createGroup(providerId: Long, name: String, iconEmoji: String? = null, contentType: ContentType): Result<VirtualGroup>
    suspend fun deleteGroup(groupId: Long): Result<Unit>
    suspend fun renameGroup(groupId: Long, newName: String): Result<Unit>
}
