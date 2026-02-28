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
import com.kuts.klaf.room.dao.IStorageSaveVersionDao
import com.kuts.klaf.room.entities.RoomCard
import com.kuts.klaf.room.entities.RoomDeck
import com.kuts.klaf.room.entities.RoomStorageSaveVersion

@Database(
    entities = [
        RoomDeck::class,
        RoomCard::class,
        RoomStorageSaveVersion::class
    ],
    version = 6,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)]
)
@ConstructedBy(KlafRoomDatabaseConstructor::class)
@TypeConverters(RoomDateConverter::class)
abstract class KlafRoomDatabase : RoomDatabase() {

    abstract fun deckDao(): IDeckDao
    abstract fun cardDao(): ICardDao
    abstract fun storageSaveVersionDao(): IStorageSaveVersionDao
}

@Suppress("KotlinNoActualForExpect")
expect object KlafRoomDatabaseConstructor : RoomDatabaseConstructor<KlafRoomDatabase> {

    override fun initialize(): KlafRoomDatabase
}
