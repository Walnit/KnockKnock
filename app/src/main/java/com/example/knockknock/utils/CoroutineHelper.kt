package com.example.knockknock.utils

import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CoroutineHelper {
    suspend fun textInputError(error: String, textInputLayout: TextInputLayout) {
        withContext(Dispatchers.Main) {
            textInputLayout.endIconMode =
                TextInputLayout.END_ICON_NONE
            textInputLayout.error = error
        }
    }
}