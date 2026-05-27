package com.afterglowtv.app.di

import com.afterglowtv.data.local.DatabaseTransactionRunner
import com.afterglowtv.data.local.RoomDatabaseTransactionRunner
import com.afterglowtv.data.preferences.PreferencesRepository
import com.afterglowtv.data.security.AndroidKeystoreCredentialCrypto
import com.afterglowtv.data.security.CredentialCrypto
import com.afterglowtv.data.sync.ProviderSyncStateReaderImpl
import com.afterglowtv.data.validation.ProviderSetupInputValidatorImpl
import com.afterglowtv.domain.manager.ParentalPinVerifier
import com.afterglowtv.domain.manager.ProviderSetupInputValidator
import com.afterglowtv.domain.manager.ProviderSyncStateReader
import com.afterglowtv.data.repository.*
import com.afterglowtv.domain.manager.ParentalControlSessionStore
import com.afterglowtv.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindProviderRepository(impl: ProviderRepositoryImpl): ProviderRepository

    @Binds @Singleton
    abstract fun bindAdultGuideCacheRepository(impl: AdultGuideCacheRepositoryImpl): AdultGuideCacheRepository

    @Binds @Singleton
    abstract fun bindChannelRepository(impl: ChannelRepositoryImpl): ChannelRepository

    @Binds @Singleton
    abstract fun bindCombinedM3uRepository(impl: CombinedM3uRepositoryImpl): CombinedM3uRepository

    @Binds @Singleton
    abstract fun bindMovieRepository(impl: MovieRepositoryImpl): MovieRepository

    @Binds @Singleton
    abstract fun bindSeriesRepository(impl: SeriesRepositoryImpl): SeriesRepository

    @Binds @Singleton
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository

    @Binds @Singleton
    abstract fun bindEpgRepository(impl: EpgRepositoryImpl): EpgRepository

    @Binds @Singleton
    abstract fun bindEpgSourceRepository(impl: EpgSourceRepositoryImpl): EpgSourceRepository

    @Binds @Singleton
    abstract fun bindFavoriteRepository(impl: FavoriteRepositoryImpl): FavoriteRepository

    @Binds @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds @Singleton
    abstract fun bindPlaybackHistoryRepository(impl: PlaybackHistoryRepositoryImpl): PlaybackHistoryRepository

    @Binds @Singleton
    abstract fun bindExternalRatingsRepository(impl: ExternalRatingsRepositoryImpl): ExternalRatingsRepository

    @Binds @Singleton
    abstract fun bindSyncMetadataRepository(impl: SyncMetadataRepositoryImpl): SyncMetadataRepository

    @Binds @Singleton
    abstract fun bindPlaybackCompatibilityRepository(impl: PlaybackCompatibilityRepositoryImpl): PlaybackCompatibilityRepository

    @Binds @Singleton
    abstract fun bindLocalMediaRepository(impl: LocalMediaRepositoryImpl): LocalMediaRepository

    @Binds @Singleton
    abstract fun bindSmbMediaSourceResolver(impl: LocalMediaRepositoryImpl): com.afterglowtv.domain.model.SmbMediaSourceResolver

    @Binds @Singleton
    abstract fun bindDatabaseTransactionRunner(impl: RoomDatabaseTransactionRunner): DatabaseTransactionRunner

    @Binds @Singleton
    abstract fun bindBackupManager(impl: com.afterglowtv.data.manager.BackupManagerImpl): com.afterglowtv.domain.manager.BackupManager

    @Binds @Singleton
    abstract fun bindRecordingManager(impl: com.afterglowtv.data.manager.RecordingManagerImpl): com.afterglowtv.domain.manager.RecordingManager

    @Binds @Singleton
    abstract fun bindProgramReminderManager(impl: com.afterglowtv.data.manager.ProgramReminderManagerImpl): com.afterglowtv.domain.manager.ProgramReminderManager

    @Binds @Singleton
    abstract fun bindParentalControlSessionStore(impl: PreferencesRepository): ParentalControlSessionStore

    @Binds @Singleton
    abstract fun bindParentalPinVerifier(impl: PreferencesRepository): ParentalPinVerifier

    @Binds @Singleton
    abstract fun bindProviderSetupInputValidator(impl: ProviderSetupInputValidatorImpl): ProviderSetupInputValidator

    @Binds @Singleton
    abstract fun bindProviderSyncStateReader(impl: ProviderSyncStateReaderImpl): ProviderSyncStateReader

    @Binds @Singleton
    abstract fun bindCredentialCrypto(impl: AndroidKeystoreCredentialCrypto): CredentialCrypto

    companion object {
        @Provides
        @Singleton
        fun provideRepositoryCoroutineScope(): CoroutineScope {
            return CoroutineScope(SupervisorJob() + Dispatchers.Default)
        }

        @Provides
        @Singleton
        fun provideM3uParser(): com.afterglowtv.data.parser.M3uParser {
            return com.afterglowtv.data.parser.M3uParser()
        }
    }
}
