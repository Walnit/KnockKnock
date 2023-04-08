package com.example.knockknock.database

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.knockknock.utils.PrefsHelper
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*

object MessageDatabase {
    fun getMessages(name: String, context: Context) : Array<KnockMessage>? {
        val securePreferences = PrefsHelper(context).openEncryptedPrefs("db_secure")
        // Get the correct file to read
        if (securePreferences.contains(name)) {
            val dao = MessageDB.getDatabase(securePreferences.getString(name, null)!!, context).messageDao()
            return if (dao.getSize() > 0) {
                dao.getAll().toTypedArray()
            } else {
                // no previous chat history with name
                null
            }
        }
        // havent even chatted with them at all
        return null

    }

    fun writeMessages(name: String, messages: Array<KnockMessage>, context: Context) {

        val securePreferences = PrefsHelper(context).openEncryptedPrefs("db_secure")
        // Get the correct file to read
        if (!securePreferences.contains(name)) {
            securePreferences.edit().putString(name, UUID.randomUUID().toString()).commit()
        }

        val dao = MessageDB.getDatabase(securePreferences.getString(name, null)!!, context).messageDao()
        dao.insertAll(*messages)
    }
}