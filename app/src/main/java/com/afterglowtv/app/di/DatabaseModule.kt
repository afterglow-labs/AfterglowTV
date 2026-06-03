package com.afterglowtv.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.afterglowtv.app.BuildConfig
import com.afterglowtv.data.local.AfterglowTVDatabase
import com.afterglowtv.data.local.dao.*
import com.afterglowtv.data.local.dao.ChannelPreferenceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private const val DEBUG_SLOW_QUERY_THRESHOLD_MS = 100L

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AfterglowTVDatabase =
        Room.databaseBuilder(
            context,
            AfterglowTVDatabase::class.java,
            "afterglowtv.db"
        )
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .openHelperFactory(
                if (BuildConfig.DEBUG) {
                    SlowQueryLoggingOpenHelperFactory(
                        delegate = FrameworkSQLiteOpenHelperFactory(),
                        slowQueryThresholdMs = DEBUG_SLOW_QUERY_THRESHOLD_MS
                    )
                } else {
                    FrameworkSQLiteOpenHelperFactory()
                }
            )
            .addMigrations(
                AfterglowTVDatabase.MIGRATION_1_2,
                AfterglowTVDatabase.MIGRATION_2_3,
                AfterglowTVDatabase.MIGRATION_3_4,
                AfterglowTVDatabase.MIGRATION_4_5,
                AfterglowTVDatabase.MIGRATION_5_6,
                AfterglowTVDatabase.MIGRATION_6_7,
                AfterglowTVDatabase.MIGRATION_7_8,
                AfterglowTVDatabase.MIGRATION_8_9,
                AfterglowTVDatabase.MIGRATION_9_10,
                AfterglowTVDatabase.MIGRATION_10_11,
                AfterglowTVDatabase.MIGRATION_11_12,
                AfterglowTVDatabase.MIGRATION_12_13,
                AfterglowTVDatabase.MIGRATION_13_14,
                AfterglowTVDatabase.MIGRATION_14_15,
                AfterglowTVDatabase.MIGRATION_15_16,
                AfterglowTVDatabase.MIGRATION_16_17,
                AfterglowTVDatabase.MIGRATION_17_18,
                AfterglowTVDatabase.MIGRATION_18_19,
                AfterglowTVDatabase.MIGRATION_19_20,
                AfterglowTVDatabase.MIGRATION_20_21,
                AfterglowTVDatabase.MIGRATION_21_22,
                AfterglowTVDatabase.MIGRATION_22_23,
                AfterglowTVDatabase.MIGRATION_23_24,
                AfterglowTVDatabase.MIGRATION_24_25,
                AfterglowTVDatabase.MIGRATION_25_26,
                AfterglowTVDatabase.MIGRATION_26_27,
                AfterglowTVDatabase.MIGRATION_27_28,
                AfterglowTVDatabase.MIGRATION_28_29,
                AfterglowTVDatabase.MIGRATION_29_30,
                AfterglowTVDatabase.MIGRATION_30_31,
                AfterglowTVDatabase.MIGRATION_31_32,
                AfterglowTVDatabase.MIGRATION_32_33,
                AfterglowTVDatabase.MIGRATION_33_34,
                AfterglowTVDatabase.MIGRATION_34_35,
                AfterglowTVDatabase.MIGRATION_35_36,
                AfterglowTVDatabase.MIGRATION_36_37,
                AfterglowTVDatabase.MIGRATION_37_38,
                AfterglowTVDatabase.MIGRATION_38_39,
                AfterglowTVDatabase.MIGRATION_39_40,
                AfterglowTVDatabase.MIGRATION_40_41,
                AfterglowTVDatabase.MIGRATION_41_42,
                AfterglowTVDatabase.MIGRATION_42_43,
                AfterglowTVDatabase.MIGRATION_43_44,
                AfterglowTVDatabase.MIGRATION_44_45,
                AfterglowTVDatabase.MIGRATION_45_46,
                AfterglowTVDatabase.MIGRATION_46_47,
                AfterglowTVDatabase.MIGRATION_47_48,
                AfterglowTVDatabase.MIGRATION_48_49,
                AfterglowTVDatabase.MIGRATION_49_50,
                AfterglowTVDatabase.MIGRATION_50_51,
                AfterglowTVDatabase.MIGRATION_51_52,
                AfterglowTVDatabase.MIGRATION_52_53,
                AfterglowTVDatabase.MIGRATION_53_54,
                AfterglowTVDatabase.MIGRATION_54_55,
                AfterglowTVDatabase.MIGRATION_55_56,
                AfterglowTVDatabase.MIGRATION_56_57
            )
            // NOTE: fallbackToDestructiveMigration() intentionally removed.
            // All future schema changes MUST add a corresponding Migration in AfterglowTVDatabase.
            .build()

    @Provides fun provideProviderDao(db: AfterglowTVDatabase): ProviderDao = db.providerDao()
    @Provides fun provideChannelDao(db: AfterglowTVDatabase): ChannelDao = db.channelDao()
    @Provides fun provideChannelPreferenceDao(db: AfterglowTVDatabase): ChannelPreferenceDao = db.channelPreferenceDao()
    @Provides fun provideMovieDao(db: AfterglowTVDatabase): MovieDao = db.movieDao()
    @Provides fun provideSeriesDao(db: AfterglowTVDatabase): SeriesDao = db.seriesDao()
    @Provides fun provideEpisodeDao(db: AfterglowTVDatabase): EpisodeDao = db.episodeDao()
    @Provides fun provideCategoryDao(db: AfterglowTVDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideCatalogSyncDao(db: AfterglowTVDatabase): CatalogSyncDao = db.catalogSyncDao()
    @Provides fun provideProgramDao(db: AfterglowTVDatabase): ProgramDao = db.programDao()
    @Provides fun provideFavoriteDao(db: AfterglowTVDatabase): FavoriteDao = db.favoriteDao()
    @Provides fun provideVirtualGroupDao(db: AfterglowTVDatabase): VirtualGroupDao = db.virtualGroupDao()
    @Provides fun providePlaybackHistoryDao(db: AfterglowTVDatabase): PlaybackHistoryDao = db.playbackHistoryDao()
    @Provides fun provideTmdbIdentityDao(db: AfterglowTVDatabase): TmdbIdentityDao = db.tmdbIdentityDao()
    @Provides fun provideSearchHistoryDao(db: AfterglowTVDatabase): SearchHistoryDao = db.searchHistoryDao()
    @Provides fun provideSearchDao(db: AfterglowTVDatabase): SearchDao = db.searchDao()
    @Provides fun provideSyncMetadataDao(db: AfterglowTVDatabase): SyncMetadataDao = db.syncMetadataDao()
    @Provides fun provideMovieCategoryHydrationDao(db: AfterglowTVDatabase): MovieCategoryHydrationDao = db.movieCategoryHydrationDao()
    @Provides fun provideSeriesCategoryHydrationDao(db: AfterglowTVDatabase): SeriesCategoryHydrationDao = db.seriesCategoryHydrationDao()
    @Provides fun provideEpgSourceDao(db: AfterglowTVDatabase): EpgSourceDao = db.epgSourceDao()
    @Provides fun provideProviderEpgSourceDao(db: AfterglowTVDatabase): ProviderEpgSourceDao = db.providerEpgSourceDao()
    @Provides fun provideEpgChannelDao(db: AfterglowTVDatabase): EpgChannelDao = db.epgChannelDao()
    @Provides fun provideEpgProgrammeDao(db: AfterglowTVDatabase): EpgProgrammeDao = db.epgProgrammeDao()
    @Provides fun provideChannelEpgMappingDao(db: AfterglowTVDatabase): ChannelEpgMappingDao = db.channelEpgMappingDao()
    @Provides fun provideCombinedM3uProfileDao(db: AfterglowTVDatabase): CombinedM3uProfileDao = db.combinedM3uProfileDao()
    @Provides fun provideCombinedM3uProfileMemberDao(db: AfterglowTVDatabase): CombinedM3uProfileMemberDao = db.combinedM3uProfileMemberDao()
    @Provides fun provideRecordingScheduleDao(db: AfterglowTVDatabase): RecordingScheduleDao = db.recordingScheduleDao()
    @Provides fun provideRecordingRunDao(db: AfterglowTVDatabase): RecordingRunDao = db.recordingRunDao()
    @Provides fun provideProgramReminderDao(db: AfterglowTVDatabase): ProgramReminderDao = db.programReminderDao()
    @Provides fun provideRecordingStorageDao(db: AfterglowTVDatabase): RecordingStorageDao = db.recordingStorageDao()
    @Provides fun providePlaybackCompatibilityDao(db: AfterglowTVDatabase): PlaybackCompatibilityDao = db.playbackCompatibilityDao()
    @Provides fun provideXtreamContentIndexDao(db: AfterglowTVDatabase): XtreamContentIndexDao = db.xtreamContentIndexDao()
    @Provides fun provideXtreamIndexJobDao(db: AfterglowTVDatabase): XtreamIndexJobDao = db.xtreamIndexJobDao()
    @Provides fun provideXtreamLiveOnboardingDao(db: AfterglowTVDatabase): XtreamLiveOnboardingDao = db.xtreamLiveOnboardingDao()
    @Provides fun provideLocalMediaLibraryDao(db: AfterglowTVDatabase): LocalMediaLibraryDao = db.localMediaLibraryDao()
    @Provides fun provideLocalMediaItemDao(db: AfterglowTVDatabase): LocalMediaItemDao = db.localMediaItemDao()
    @Provides fun provideLocalMediaChannelDao(db: AfterglowTVDatabase): LocalMediaChannelDao = db.localMediaChannelDao()
    @Provides fun provideLocalMediaProgramDao(db: AfterglowTVDatabase): LocalMediaProgramDao = db.localMediaProgramDao()
    @Provides fun provideAdultCacheDao(db: AfterglowTVDatabase): AdultCacheDao = db.adultCacheDao()
}
