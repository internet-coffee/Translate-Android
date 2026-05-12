package com.android.sttranslate

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.android.sttranslate.ui.theme.STTranslateTheme

class ShareTranslateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // 獲取分享文字
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
        val processText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString() ?: ""

        val textToTranslate = sharedText.ifBlank { processText }

        if (textToTranslate.isBlank()) {
            finish()
            return
        }

        setContent {
            STTranslateTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { finish() },
                    contentAlignment = Alignment.Center
                ) {
                    TranslateDialogCard(
                        inputText = textToTranslate
                    )
                }
            }
        }
    }
}

@Composable
fun TranslateDialogCard(
    inputText: String,
) {
    // 狀態
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var sourceLangCode by remember { mutableStateOf(LanguagePreferences.getSourceLanguage(context)) }
    var targetLangCode by remember { mutableStateOf(LanguagePreferences.getTargetLanguage(context)) }
    var resultText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // 選單開關
    var isSourceMenuExpanded by remember { mutableStateOf(false) }
    var isTargetMenuExpanded by remember { mutableStateOf(false) }

    // 翻譯邏輯
    LaunchedEffect(sourceLangCode, targetLangCode) {
        isLoading = true
        try {
            val response = NetworkModule.api.translate(
                source = sourceLangCode,
                target = targetLangCode,
                query = inputText
            )
            resultText = response.translatedText
        } catch (_: Exception) {
            resultText = context.getString(R.string.error_connection)
        } finally {
            isLoading = false
        }
    }

    // === 主卡片 ===
    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .heightIn(max = 600.dp)
            .clickable(interactionSource = null, indication = null) {},
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall
                )
                if (isLoading) {
                    Spacer(modifier = Modifier.width(16.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 內容捲動區
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                // === 原文區塊 Header ===
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 語言選擇 (左側)
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickable { isSourceMenuExpanded = true }
                                .padding(end = 8.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(
                                    SUPPORTED_LANGUAGES[sourceLangCode] ?: R.string.lang_detect
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // 來源選單
                        DropdownMenu(
                            expanded = isSourceMenuExpanded,
                            onDismissRequest = { isSourceMenuExpanded = false },
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            SUPPORTED_LANGUAGES.forEach { (code, nameResId) ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(nameResId)) },
                                    onClick = {
                                        sourceLangCode = code
                                        isSourceMenuExpanded = false
                                        LanguagePreferences.saveSourceLanguage(context, code)
                                    }
                                )
                            }
                        }
                    }

                    // 右側功能鍵: 交換 & 複製
                    Row {
                        // 交換按鈕
                        IconButton(
                            onClick = {
                                val (newSource, newTarget) = swapLanguages(sourceLangCode, targetLangCode)

                                sourceLangCode = newSource
                                targetLangCode = newTarget

                                LanguagePreferences.saveSourceLanguage(context, newSource)
                                LanguagePreferences.saveTargetLanguage(context, newTarget)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                contentDescription = "Swap",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // 複製原文按鈕
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(inputText))

                                Toast.makeText(
                                    context,
                                    context.getString(R.string.copied),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Original",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 原文內容
                Text(
                    text = inputText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // 分隔線
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth(0.3f) // 只有中間 30% 長度
                        .align(Alignment.CenterHorizontally),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // === 譯文區塊 Header ===
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 目標語言選擇
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickable { isTargetMenuExpanded = true }
                                .padding(end = 8.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(
                                    SUPPORTED_LANGUAGES[targetLangCode] ?: R.string.lang_zh_tw
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // 目標選單
                        DropdownMenu(
                            expanded = isTargetMenuExpanded,
                            onDismissRequest = { isTargetMenuExpanded = false },
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            SUPPORTED_LANGUAGES.forEach { (code, nameResId) ->
                                if (code == "auto") return@forEach
                                DropdownMenuItem(
                                    text = { Text(stringResource(nameResId)) },
                                    onClick = {
                                        targetLangCode = code; isTargetMenuExpanded =
                                        false; LanguagePreferences.saveTargetLanguage(
                                        context,
                                        code
                                    )
                                    }
                                )
                            }
                        }
                    }

                    // 複製譯文按鈕
                    IconButton(
                        onClick = {
                            if (resultText.isNotEmpty()) {
                                clipboardManager.setText(AnnotatedString(resultText))

                                Toast.makeText(
                                    context,
                                    context.getString(R.string.copied),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Translated",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 譯文內容
                Box(modifier = Modifier.heightIn(min = 40.dp)) {
                    Text(
                        text = resultText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}