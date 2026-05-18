package com.android.sttranslate

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class TranslateViewModel : ViewModel() {
    var inputText by mutableStateOf("")
    var resultText by mutableStateOf("")
    var isLoading by mutableStateOf(false)

    private var translationJob: Job? = null

    /**
     * 執行翻譯請求
     * @param source 來源語言代碼
     * @param target 目標語言代碼
     * @param text 要翻譯的文字
     */
    fun performTranslate(source: String, target: String, text: String = inputText) {
        if (text.isBlank()) return

        translationJob?.cancel()
        isLoading = true

        translationJob = viewModelScope.launch {
            try {
                val response = NetworkModule.api.translate(
                    source = source,
                    target = target,
                    query = text
                )
                resultText = response.translatedText
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                resultText = "ERROR_CONNECTION"
            } finally {
                isLoading = false
            }
        }
    }

    fun clearText() {
        inputText = ""
        resultText = ""
    }
}