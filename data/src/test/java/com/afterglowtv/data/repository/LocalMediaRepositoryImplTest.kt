package com.afterglowtv.data.repository

import android.content.Context
import com.afterglowtv.data.local.dao.LocalMediaItemDao
import com.afterglowtv.data.local.dao.LocalMediaLibraryDao
import com.afterglowtv.data.local.entity.LocalMediaLibraryEntity
import com.afterglowtv.data.security.CredentialCrypto
import com.afterglowtv.domain.model.LocalMediaLibrarySourceType
import com.afterglowtv.domain.model.Result
import com.afterglowtv.domain.model.SmbShareConfig
import com.afterglowtv.domain.model.SmbShareUri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class LocalMediaRepositoryImplTest {
    private val context: Context = mock()
    private val libraryDao: LocalMediaLibraryDao = mock()
    private val itemDao: LocalMediaItemDao = mock()
    private val credentialCrypto = object : CredentialCrypto {
        override fun encryptIfNeeded(value: String): String = "encrypted:$value"
        override fun decryptIfNeeded(value: String): String = value.removePrefix("encrypted:")
    }

    @Test
    fun `addSmbLibraryReference saves share without scanning items`() = runTest {
        val config = SmbShareConfig(
            host = "192.168.1.8",
            shareName = "Plex",
            path = "",
            displayName = "Plex",
            username = "corey",
            password = "secret"
        )
        val rootUri = SmbShareUri.buildRoot(SmbShareUri.fromConfig(config))
        val saved = LocalMediaLibraryEntity(
            id = 42L,
            name = "Plex",
            rootUri = rootUri,
            sourceType = LocalMediaLibrarySourceType.SMB,
            displayName = "Plex",
            smbHost = "192.168.1.8",
            smbPort = SmbShareUri.DEFAULT_PORT,
            smbShare = "Plex",
            smbUsername = "corey",
            smbPassword = "encrypted:secret"
        )
        whenever(libraryDao.getLibraryByRootUri(rootUri)).thenReturn(null)
        whenever(libraryDao.upsertLibrary(any())).thenReturn(saved.id)
        whenever(libraryDao.getLibrary(saved.id)).thenReturn(saved)

        val result = repository().addSmbLibraryReference(config)

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data.id).isEqualTo(saved.id)
        verify(itemDao, never()).upsertItems(any())
        verify(itemDao, never()).deleteStaleByLibrary(any(), any())
    }

    private fun repository() = LocalMediaRepositoryImpl(
        context = context,
        libraryDao = libraryDao,
        itemDao = itemDao,
        credentialCrypto = credentialCrypto
    )
}
