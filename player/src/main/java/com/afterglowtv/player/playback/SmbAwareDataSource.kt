package com.afterglowtv.player.playback

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import com.afterglowtv.domain.model.SmbMediaSourceResolver

@UnstableApi
internal class SmbAwareDataSource(
    private val defaultDataSource: DataSource,
    private val smbMediaSourceResolver: SmbMediaSourceResolver
) : DataSource {
    private val transferListeners = mutableListOf<TransferListener>()
    private var activeDataSource: DataSource? = null

    override fun addTransferListener(transferListener: TransferListener) {
        defaultDataSource.addTransferListener(transferListener)
        transferListeners += transferListener
    }

    override fun open(dataSpec: DataSpec): Long {
        val dataSource = if (dataSpec.uri.scheme.equals("smb", ignoreCase = true)) {
            SmbMediaDataSource(smbMediaSourceResolver).also { smb ->
                transferListeners.forEach(smb::addTransferListener)
            }
        } else {
            defaultDataSource
        }
        activeDataSource = dataSource
        return dataSource.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        activeDataSource?.read(buffer, offset, length) ?: 0

    override fun getUri(): Uri? = activeDataSource?.uri

    override fun getResponseHeaders(): Map<String, List<String>> =
        activeDataSource?.responseHeaders.orEmpty()

    override fun close() {
        activeDataSource?.close()
        activeDataSource = null
    }
}

@UnstableApi
internal class SmbAwareDataSourceFactory(
    private val defaultFactory: DataSource.Factory,
    private val smbMediaSourceResolver: SmbMediaSourceResolver
) : DataSource.Factory {
    override fun createDataSource(): DataSource =
        SmbAwareDataSource(
            defaultDataSource = defaultFactory.createDataSource(),
            smbMediaSourceResolver = smbMediaSourceResolver
        )
}
