package com.example.klaf.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.klaf.domain.pojo.Deck
import com.example.klaf.domain.pojo.TABLE_NAME

@Dao
interface DeckDao {

    @Query("SELECT * FROM $TABLE_NAME")
    fun getAll(): List<Deck>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDeck(deck: Deck)
}