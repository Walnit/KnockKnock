package com.example.knockknock.database

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.knockknock.structures.KnockMessage
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*

object MessageDatabase {

    private var messagesArray: Array<KnockMessage> = arrayOf()
    private var currentTarget: String? = null

    fun getMessages(name: String, context: Context) : Array<KnockMessage>? {

        if (name == currentTarget && messagesArray.isNotEmpty()) {
            return messagesArray
        } else {

            currentTarget = name
            messagesArray = arrayOf()

            val masterKey =
                MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            val securePreferences = EncryptedSharedPreferences.create(
                context,
                "db_secure",
                MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            // Get the correct file to read
            val fileToRead = securePreferences.getString(name, null)

            if (fileToRead != null) {
                val encryptedFile = EncryptedFile.Builder(
                    context,
                    File(context.filesDir, fileToRead),
                    masterKey,
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                ).build()

                var stringJSONArray = JSONArray()

                if (File(context.filesDir, fileToRead).exists()) {
                    val inputStream = encryptedFile.openFileInput()
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    var nextByte: Int = inputStream.read()
                    while (nextByte != -1) {
                        byteArrayOutputStream.write(nextByte)
                        nextByte = inputStream.read()
                    }

                    stringJSONArray = JSONArray(String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8))
                }

                for (i in 0 until stringJSONArray.length()) {
                    messagesArray = messagesArray.plus(Json.decodeFromString<KnockMessage>(stringJSONArray.getString(i)))

                }

                return messagesArray

            } else {
                return null
            }
        }

    }

    fun writeMessages(name: String, messages: Array<KnockMessage>, context: Context) {

        val masterKey =
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        val securePreferences = EncryptedSharedPreferences.create(
            context,
            "db_secure",
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        // Get correct file to write
        var fileToRead = securePreferences.getString(name, null)
        if (fileToRead == null) {
            fileToRead = UUID.randomUUID().toString()
            securePreferences.edit().putString(name, fileToRead).apply()
        }
        val encryptedFile = EncryptedFile.Builder(
            context,
            File(context.filesDir, fileToRead),
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        // Append new messages to the list of old messages

        var existingStringMessages = JSONArray()

        if (File(context.filesDir, fileToRead).exists()) {
            val inputStream = encryptedFile.openFileInput()
            val byteArrayOutputStream = ByteArrayOutputStream()
            var nextByte: Int = inputStream.read()
            while (nextByte != -1) {
                byteArrayOutputStream.write(nextByte)
                nextByte = inputStream.read()
            }
            existingStringMessages = JSONArray(String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8))
        }

        if (name == currentTarget) {
            messages.forEach { message ->
                messagesArray = messagesArray.plus(message)
                existingStringMessages.put(Json.encodeToString(message))
            }
        } else {
            messages.forEach { message ->
                existingStringMessages.put(Json.encodeToString(message))
            }
        }


        // Write new messages to disk

        File(context.filesDir, fileToRead).delete() // Delete so we can make changes, encryptedfile sucks

        val fileContent = existingStringMessages.toString()
            .toByteArray(StandardCharsets.UTF_8)
        encryptedFile.openFileOutput().apply {
            write(fileContent)
            flush()
            close()
        }

    }
}