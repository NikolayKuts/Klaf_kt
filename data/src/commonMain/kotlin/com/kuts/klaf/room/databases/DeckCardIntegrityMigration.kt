package com.kuts.klaf.room.databases

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

internal object DeckCardIntegrityMigration : Migration(8, 9) {

    override fun migrate(connection: SQLiteConnection) {
        connection.prepare(
            "SELECT cards.id, cards.deckId FROM cards " +
                "LEFT JOIN decks ON decks.id = cards.deckId " +
                "WHERE decks.id IS NULL LIMIT 1",
        ).use { statement ->
            if (statement.step()) {
                error("Cannot migrate orphan card ${statement.getLong(0)}: missing deck ${statement.getLong(1)}")
            }
        }

        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `cards_new` (" +
                "`deckId` INTEGER NOT NULL, " +
                "`nativeWord` TEXT NOT NULL, " +
                "`foreignWord` TEXT NOT NULL, " +
                "`ipa` TEXT NOT NULL, " +
                "`wordMeaningInsightsJson` TEXT NOT NULL, " +
                "`mnemonicJson` TEXT NOT NULL, " +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "FOREIGN KEY(`deckId`) REFERENCES `decks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
        connection.execSQL(
            "INSERT INTO `cards_new` (`deckId`, `nativeWord`, `foreignWord`, `ipa`, " +
                "`wordMeaningInsightsJson`, `mnemonicJson`, `id`) " +
                "SELECT `deckId`, `nativeWord`, `foreignWord`, `ipa`, " +
                "`wordMeaningInsightsJson`, `mnemonicJson`, `id` FROM `cards`",
        )
        connection.execSQL("DROP TABLE `cards`")
        connection.execSQL("ALTER TABLE `cards_new` RENAME TO `cards`")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_cards_deckId` ON `cards` (`deckId`)")
    }
}
