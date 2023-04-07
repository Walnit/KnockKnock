package com.example.knockknock.knockcode

@kotlinx.serialization.Serializable
data class KnockInput(
    var button: Set<Short>,
    var delay: Short
) {
    override fun toString(): String {
        return "KnockInput(Buttons=${button.joinToString()}, delay=$delay)"
    }
}
