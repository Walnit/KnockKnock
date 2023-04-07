package com.example.knockknock.knockcode

import com.example.knockknock.KnockCodeFragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.absoluteValue

class KnockCode {
    val buttonSequence: MutableList<Short> = mutableListOf()
    val millisSequence: MutableList<Long> = mutableListOf()
    val knockSequence: MutableList<KnockInput> = mutableListOf()

    fun addInput(button: Short, millis: Long) {
        buttonSequence.add(button)
        millisSequence.add(millis)
    }
    fun finishInput() : Array<KnockInput> {
        val delaySequence: MutableList<Short> = mutableListOf(0)
        for (i in 1 until millisSequence.size) {
            delaySequence.add((millisSequence[i] - millisSequence[i-1]).toShort())
        }

        var minDelay: Short = 1000

        delaySequence.forEach { sh ->
            if (sh > 30) {
                minDelay = minOf(minDelay, sh)
            }
        }

        minDelay = (minDelay / 2).toShort()

        delaySequence.forEachIndexed { index, sh ->
            if (index > 0 && sh <= 30) {
                knockSequence.last().button = knockSequence.last().button.plus(buttonSequence[index])
            } else {
                knockSequence.add(KnockInput(setOf(buttonSequence[index]), (sh/minDelay).toShort()))
            }
        }

        return knockSequence.toTypedArray()

    }

    fun toJson() : String {
        val stringList: ArrayList<String> = arrayListOf()
        knockSequence.forEach {
            stringList.add(Json.encodeToString(it))
        }

        return Gson().toJson(stringList)
    }

    fun fromJson(json: String) {
        class Token : TypeToken<ArrayList<String>>()
        val stringList: MutableList<String> = Gson().fromJson(json, Token().type)

        stringList.forEach { string ->
            knockSequence.add(Json.decodeFromString<KnockInput>(string))
        }

    }

    override fun equals(other: Any?): Boolean {
        if (other !is KnockCode) return false

        knockSequence.forEachIndexed { index, knockInput ->
            if (knockInput.button != other.knockSequence[index].button) return false
            if ((knockInput.delay - other.knockSequence[index].delay).absoluteValue > 1) return false
        }

        return true
    }

    override fun toString(): String {
        return "KnockCode($knockSequence)"
    }
}