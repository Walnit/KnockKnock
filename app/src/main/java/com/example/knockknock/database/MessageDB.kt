package com.example.knockknock.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.knockknock.utils.PrefsHelper
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(entities = [KnockMessage::class], version = 1)
abstract class MessageDB : RoomDatabase() {
    abstract fun messageDao(): MessageDao

    companion object {
        private var INSTANCE: MessageDB? = null
        private var INSTANCE_TARGET: String? = null
        fun getDatabase(target: String, context: Context): MessageDB {
            if (INSTANCE == null || INSTANCE_TARGET != target) {
                synchronized(this) {
                    val secure_prefs = PrefsHelper(context).openEncryptedPrefs("secure_prefs")
                    val passphrase: ByteArray = SQLiteDatabase.getBytes(secure_prefs.getString("db_pass", null)!!.toCharArray())
                    val factory = SupportFactory(passphrase)
                    INSTANCE = Room.databaseBuilder(
                        context,
                        MessageDB::class.java,
                        target
                    )
                        .openHelperFactory(factory)
                        .allowMainThreadQueries()
                        .build()
                    INSTANCE_TARGET = target
                }
            }
            return INSTANCE!!
        }
    }
}