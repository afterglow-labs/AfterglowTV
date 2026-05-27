package com.afterglowtv.player.playback

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import com.afterglowtv.domain.model.SmbMediaSourceResolver
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2CreateOptions
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File
import java.io.IOException
import java.util.EnumSet
import java.util.concurrent.TimeUnit

@UnstableApi
internal class SmbMediaDataSource(
    private val resolver: SmbMediaSourceResolver
) : BaseDataSource(false) {
    private var uri: Uri? = null
    private var dataSpec: DataSpec? = null
    private var client: SMBClient? = null
    private var connection: Connection? = null
    private var session: Session? = null
    private var share: DiskShare? = null
    private var file: File? = null
    private var position: Long = 0L
    private var bytesRemaining: Long = C.LENGTH_UNSET.toLong()
    private var opened = false

    override fun open(dataSpec: DataSpec): Long {
        val resolved = resolver.resolve(dataSpec.uri.toString())
            ?: throw IOException("Network media source is not available.")
        transferInitializing(dataSpec)
        return try {
            val smbConfig = SmbConfig.builder()
                .withTimeout(SMB_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .withSoTimeout(SMB_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build()
            val smbClient = SMBClient(smbConfig)
            val smbConnection = smbClient.connect(resolved.host, resolved.port)
            val authentication = if (resolved.username.isBlank() && resolved.password.isBlank()) {
                AuthenticationContext.guest()
            } else {
                AuthenticationContext(
                    resolved.username,
                    resolved.password.toCharArray(),
                    resolved.domain.takeIf { it.isNotBlank() }
                )
            }
            val smbSession = smbConnection.authenticate(authentication)
            val diskShare = smbSession.connectShare(resolved.shareName) as? DiskShare
                ?: throw IOException("Network share is not a file share.")
            val smbFile = diskShare.openFile(
                resolved.path,
                EnumSet.of(AccessMask.GENERIC_READ),
                EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                EnumSet.of(
                    SMB2CreateOptions.FILE_NON_DIRECTORY_FILE,
                    SMB2CreateOptions.FILE_RANDOM_ACCESS
                )
            )

            client = smbClient
            connection = smbConnection
            session = smbSession
            share = diskShare
            file = smbFile
            uri = dataSpec.uri
            this.dataSpec = dataSpec
            position = dataSpec.position

            val fileSize = runCatching {
                diskShare.getFileInformation(resolved.path).standardInformation.endOfFile
            }.getOrDefault(C.LENGTH_UNSET.toLong())
            bytesRemaining = when {
                dataSpec.length != C.LENGTH_UNSET.toLong() -> dataSpec.length
                fileSize != C.LENGTH_UNSET.toLong() -> (fileSize - dataSpec.position).coerceAtLeast(0L)
                else -> C.LENGTH_UNSET.toLong()
            }
            opened = true
            transferStarted(dataSpec)
            bytesRemaining
        } catch (error: Throwable) {
            close()
            if (error is IOException) {
                throw error
            }
            throw IOException(error)
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        val bytesToRead = if (bytesRemaining == C.LENGTH_UNSET.toLong()) {
            length
        } else {
            length.coerceAtMost(bytesRemaining.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        }
        val read = file?.read(buffer, position, offset, bytesToRead) ?: C.RESULT_END_OF_INPUT
        if (read <= 0) return C.RESULT_END_OF_INPUT
        position += read
        if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
            bytesRemaining -= read
        }
        bytesTransferred(read)
        return read
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        uri = null
        dataSpec = null
        runCatching { file?.close() }
        runCatching { share?.close() }
        runCatching { session?.close() }
        runCatching { connection?.close() }
        runCatching { client?.close() }
        file = null
        share = null
        session = null
        connection = null
        client = null
        if (opened) {
            opened = false
            transferEnded()
        }
    }

    private companion object {
        const val SMB_TIMEOUT_SECONDS = 20L
    }
}
