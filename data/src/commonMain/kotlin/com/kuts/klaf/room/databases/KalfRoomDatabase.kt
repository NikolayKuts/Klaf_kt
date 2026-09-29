package com.kuts.klaf.room.databases

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import com.kuts.klaf.room.converters.RoomDateConverter
import com.kuts.klaf.room.dao.ICardDao
import com.kuts.klaf.room.dao.IDeckDao
import com.kuts.klaf.room.dao.IIgnoredVocabularyWordDao
import com.kuts.klaf.room.dao.IPendingSyncOperationDao

import com.kuts.klaf.room.dao.IStorageSaveVersionDao
import com.kuts.klaf.room.dao.ISyncConflictSnapshotDao
import com.kuts.klaf.room.dao.ISyncCheckpointDao
import com.kuts.klaf.room.dao.IVocabularySourceDao
import com.kuts.klaf.room.dao.IVocabularySourceItemDao
import com.kuts.klaf.room.entities.RoomCard
import com.kuts.klaf.room.entities.RoomDeck
import com.kuts.klaf.room.entities.RoomIgnoredVocabularyWord
import com.kuts.klaf.room.entities.RoomPendingSyncOperation

import com.kuts.klaf.room.entities.RoomStorageSaveVersion
import com.kuts.klaf.room.entities.RoomSyncConflictSnapshot
import com.kuts.klaf.room.entities.RoomSyncCheckpoint
import com.kuts.klaf.room.entities.RoomVocabularySource
import com.kuts.klaf.room.entities.RoomVocabularySourceItem

@Database(
    entities = [
        RoomDeck::class,
        RoomCard::class,
        RoomStorageSaveVersion::class,
        RoomVocabularySource::class,
        RoomVocabularySourceItem::class,
        RoomIgnoredVocabularyWord::class,
        RoomPendingSyncOperation::class,
        RoomSyncCheckpoint::class,
        RoomSyncConflictSnapshot::class,
    ],
    version = 16,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 10, to = 11),
        AutoMigration(from = 11, to = 12),
        AutoMigration(from = 12, to = 13),
        AutoMigration(from = 13, to = 14),
        AutoMigration(from = 14, to = 15),
    ],
)
@ConstructedBy(KlafRoomDatabaseConstructor::class)
@TypeConverters(RoomDateConverter::class)
abstract class KlafRoomDatabase : RoomDatabase() {

    abstract fun deckDao(): IDeckDao
    abstract fun cardDao(): ICardDao
    abstract fun storageSaveVersionDao(): IStorageSaveVersionDao
    abstract fun vocabularySourceDao(): IVocabularySourceDao
    abstract fun vocabularySourceItemDao(): IVocabularySourceItemDao
    abstract fun ignoredVocabularyWordDao(): IIgnoredVocabularyWordDao
    abstract fun pendingSyncOperationDao(): IPendingSyncOperationDao

    abstract fun syncCheckpointDao(): ISyncCheckpointDao
    abstract fun syncConflictSnapshotDao(): ISyncConflictSnapshotDao
}

@Suppress("KotlinNoActualForExpect")
expect object KlafRoomDatabaseConstructor : RoomDatabaseConstructor<KlafRoomDatabase> {

    override fun initialize(): KlafRoomDatabase
}
