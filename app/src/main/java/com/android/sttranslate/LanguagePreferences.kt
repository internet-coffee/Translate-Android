package com.android.sttranslate

import android.content.Context
import androidx.core.content.edit

object LanguagePreferences {
    private const val PREF_NAME = "st_translate_prefs"
    private const val KEY_SOURCE_LANG = "source_lang_code"
    private const val KEY_TARGET_LANG = "target_lang_code"

    // 預設值
    private const val DEFAULT_SOURCE = "en"
    private const val DEFAULT_TARGET = "zh-TW"

    // 取得來源語言
    fun getSourceLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SOURCE_LANG, DEFAULT_SOURCE) ?: DEFAULT_SOURCE
    }

    // 儲存來源語言
    fun saveSourceLanguage(context: Context, code: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_SOURCE_LANG, code) }
    }

    // 取得目標語言
    fun getTargetLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_TARGET_LANG, DEFAULT_TARGET) ?: DEFAULT_TARGET
    }

    // 儲存目標語言
    fun saveTargetLanguage(context: Context, code: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_TARGET_LANG, code) }
    }
}