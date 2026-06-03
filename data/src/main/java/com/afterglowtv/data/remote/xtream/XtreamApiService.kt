package com.afterglowtv.data.remote.xtream

import com.afterglowtv.data.remote.http.HttpRequestProfile
import com.afterglowtv.data.remote.dto.XtreamAuthResponse
import com.afterglowtv.data.remote.dto.XtreamCategory
import com.afterglowtv.data.remote.dto.XtreamEpgResponse
import com.afterglowtv.data.remote.dto.XtreamSeriesInfoResponse
import com.afterglowtv.data.remote.dto.XtreamSeriesItem
import com.afterglowtv.data.remote.dto.XtreamStream
import com.afterglowtv.data.remote.dto.XtreamVodInfoResponse

/**
 * Xtream player API abstraction.
 */
interface XtreamApiService {
    suspend fun authenticate(
        endpoint: String,
        requestProfile: HttpRequestProfile = HttpRequestProfile()
    ): XtreamAuthResponse

    suspend fun getLiveCategories(
        endpoint: String,
        requestProfile: HttpRequestProfile = HttpRequestProfile()
    ): List<XtreamCategory>

    suspend fun getLiveStreams(
        endpoint: String,
        requestProfile: HttpRequestProfile = HttpRequestProfile()
    ): List<XtreamStream>

    suspend fun getVodCategories(
        endpoint: String,
        requestProfile: HttpRequestProfile = HttpRequestProfile()
    ): List<XtreamCategory>

    suspend fun getVodStreams(
        endpoint: String,
        requestProfile: HttpRequestProfile = HttpRequestProfile()
    ): List<XtreamStream>

    suspend fun streamVodStreams(
        endpoint: String,
        requestProfile: HttpRequestProfile = HttpRequestProfile(),
        onItem: suspend (XtreamStream) -> Unit
    ): Int =
        getVodStreams(endpoint, requestProfile).also { streams ->
            streams.forEach { stream -> onItem(stream) }
        }.size

    suspend fun getVodInfo(
        endpoint: String,
        requestProfile: HttpRequestProfile = HttpRequestProfile()
    ): XtreamVodInfoResponse

    suspend fun getSeriesCategories(
        endpoint: String,
        requestProfile: HttpRequestProfile = HttpRequestProfile()
    ): List<XtreamCategory>

    suspend fun getSeriesList(
        endpoint: String,
        requestProfile: HttpRequestProfile = HttpRequestProfile()
    ): List<XtreamSeriesItem>

    suspend fun streamSeriesList(
        endpoint: String,
        requestProfile: HttpRequestProfile = HttpRequestProfile(),
        onItem: suspend (XtreamSeriesItem) -> Unit
    ): Int =
        getSeriesList(endpoint, requestProfile).also { items ->
            items.forEach { item -> onItem(item) }
        }.size

    suspend fun getSeriesInfo(
        endpoint: String,
        requestProfile: HttpRequestProfile = HttpRequestProfile()
    ): XtreamSeriesInfoResponse

    suspend fun getShortEpg(
        endpoint: String,
        requestProfile: HttpRequestProfile = HttpRequestProfile()
    ): XtreamEpgResponse

    suspend fun getFullEpg(
        endpoint: String,
        requestProfile: HttpRequestProfile = HttpRequestProfile()
    ): XtreamEpgResponse
}
