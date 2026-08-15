package com.android.sttranslate


import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import retrofit2.HttpException
import java.io.IOException

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

sealed interface TranslateResult {
    /** 尚未翻譯或已被清空 */
    data object Empty : TranslateResult

    /** 翻譯成功 */
    data class Success(val text: String) : TranslateResult

    /** 翻譯失敗 */
    data class Error(val type: TranslateErrorType) : TranslateResult
}

enum class TranslateErrorType {
    /** 無網路連線或連線逾時 */
    NETWORK,
    /** 伺服器錯誤 */
    SERVER,
    /** 未預期的錯誤 */
    UNKNOWN
}

fun TranslateErrorType.messageResId(): Int = when (this) {
    TranslateErrorType.NETWORK -> R.string.error_connection
    TranslateErrorType.SERVER -> R.string.error_server
    TranslateErrorType.UNKNOWN -> R.string.error_unknown
}


class TranslateViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: TranslateRepository = SimplyTranslateRepository()
) : AndroidViewModel(application) {

    private val appContext: Context = application.applicationContext

    var inputText by mutableStateOf("")
        private set
    var translateResult: TranslateResult by mutableStateOf(TranslateResult.Empty)
        private set
    var isLoading by mutableStateOf(false)
        private set

    var sourceLangCode by mutableStateOf(LanguagePreferences.getSourceLanguage(appContext))
        private set
    var targetLangCode by mutableStateOf(LanguagePreferences.getTargetLanguage(appContext))
        private set

    private var translationJob: Job? = null

    fun onInputTextChanged(text: String) {
        inputText = text
    }

    fun onSourceLanguageSelected(code: String) {
        sourceLangCode = code
        LanguagePreferences.saveSourceLanguage(appContext, code)
        if (inputText.isNotBlank()) {
            performTranslate(code, targetLangCode)
        }
    }

    fun onTargetLanguageSelected(code: String) {
        targetLangCode = code
        LanguagePreferences.saveTargetLanguage(appContext, code)
        if (inputText.isNotBlank()) {
            performTranslate(sourceLangCode, code)
        }
    }

    fun onSwapLanguages() {
        val swapped = swapLanguages(sourceLangCode, targetLangCode)
        sourceLangCode = swapped.source
        targetLangCode = swapped.target
        LanguagePreferences.saveSourceLanguage(appContext, swapped.source)
        LanguagePreferences.saveTargetLanguage(appContext, swapped.target)
    }


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
                val response = repository.translate(
                    source = source,
                    target = target,
                    text = text
                )
                translateResult = TranslateResult.Success(response.translatedText)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                translateResult = TranslateResult.Error(
                    type = when (e) {
                        is IOException -> TranslateErrorType.NETWORK
                        is HttpException -> TranslateErrorType.SERVER
                        else -> TranslateErrorType.UNKNOWN
                    }
                )
            } finally {
                isLoading = false
            }
        }
    }

    fun clearText() {
        inputText = ""
        translateResult = TranslateResult.Empty
    }
}