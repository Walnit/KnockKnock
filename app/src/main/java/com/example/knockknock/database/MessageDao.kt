package com.example.knockknock.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MessageDao {
    @Query("SELECT * FROM KNOCKMESSAGE")
    fun getAll(): List<KnockMessage>

    @Query("SELECT COUNT(*) FROM KNOCKMESSAGE")
    fun getSize(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg messages: KnockMessage)
}