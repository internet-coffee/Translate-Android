package com.android.sttranslate

// 翻譯資料來源
interface TranslateRepository {
    suspend fun translate(source: String, target: String, text: String): STTranslationResponse
}

// 透過 SimplyTranslateApi 實際呼叫網路
class SimplyTranslateRepository(
    private val api: SimplyTranslateApi = NetworkModule.api
) : TranslateRepository {
    override suspend fun translate(source: String, target: String, text: String): STTranslationResponse {
        return api.translate(source = source, target = target, query = text)
    }
}