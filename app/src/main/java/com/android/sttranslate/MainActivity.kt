package com.android.sttranslate

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.android.sttranslate.ui.theme.STTranslateTheme
import kotlinx.coroutines.launch

object UIConfig {
    val HorizontalStart = 16.dp  // 左側文字起點
    val IconAreaWidth = 56.dp    // 右側 Icon 的寬度
    val VerticalPadding = 12.dp  // 上下間距
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            STTranslateTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RTranslatorStyleScreen()
                }
            }
        }
    }
}

@Composable
fun RTranslatorStyleScreen() {
    val context = LocalContext.current
    var sourceLangCode by remember { mutableStateOf(LanguagePreferences.getSourceLanguage(context)) }
    var targetLangCode by remember { mutableStateOf(LanguagePreferences.getTargetLanguage(context)) }
    var inputText by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    fun performTranslate() {
        if (inputText.isBlank() || isLoading) return
        focusManager.clearFocus()
        isLoading = true
        scope.launch {
            try {
                val response = NetworkModule.api.translate(
                    source = sourceLangCode,
                    target = targetLangCode,
                    query = inputText
                )
                resultText = response.translatedText
            } catch (_: Exception) {
                Toast.makeText(
                    context,
                    context.getString(R.string.error_connection),
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(sourceLangCode, targetLangCode) {
        if (inputText.isNotBlank()) {
            performTranslate()
        }
    }

    Scaffold(
        topBar = {
            LanguageSelectionBar(
                sourceLang = sourceLangCode,
                targetLang = targetLangCode,
                onSourceClick = {
                    sourceLangCode = it; LanguagePreferences.saveSourceLanguage(
                    context,
                    it
                )
                },
                onTargetClick = {
                    targetLangCode = it; LanguagePreferences.saveTargetLanguage(
                    context,
                    it
                )
                },
                onSwap = {
                    val swapped = swapLanguages(sourceLangCode, targetLangCode)
                    sourceLangCode = swapped.source
                    targetLangCode = swapped.target
                    if (resultText.isNotBlank()) {
                        inputText = resultText
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { performTranslate() },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                else Icon(Icons.Default.Translate, contentDescription = null)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            InputArea(
                modifier = Modifier.weight(0.4f),
                inputText = inputText,
                onValueChange = { inputText = it },
                onTranslate = { performTranslate() },
                onClear = { inputText = ""; resultText = "" }
            )

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth(0.2f) // 縮短為螢幕寬度的 20%
                    .align(Alignment.CenterHorizontally),
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            ResultArea(
                modifier = Modifier.weight(0.6f),
                resultText = resultText
            )
        }
    }
}

@Composable
fun InputArea(
    modifier: Modifier,
    inputText: String,
    onValueChange: (String) -> Unit,
    onTranslate: () -> Unit,
    onClear: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    Box(modifier = modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface)) {
        BasicTextField(
            value = inputText,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = UIConfig.HorizontalStart,
                    end = UIConfig.IconAreaWidth,
                    top = UIConfig.VerticalPadding,
                    bottom = UIConfig.VerticalPadding
                ),
            textStyle = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.onSurface),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onTranslate() }),
            decorationBox = { innerTextField ->
                if (inputText.isEmpty()) {
                    Text(
                        stringResource(R.string.input_placeholder),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                innerTextField()
            }
        )

        Column(modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(end = 4.dp)) {
            IconButton(onClick = {
                clipboardManager.getText()?.let { onValueChange(it.text) }
            }) {
                Icon(
                    Icons.Default.ContentPaste,
                    contentDescription = stringResource(R.string.action_paste),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            if (inputText.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.action_clear),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun ResultArea(modifier: Modifier, resultText: String) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(
                    start = UIConfig.HorizontalStart,
                    end = UIConfig.IconAreaWidth,
                    top = UIConfig.VerticalPadding,
                    bottom = UIConfig.VerticalPadding
                )
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = resultText.ifBlank { stringResource(R.string.translation_empty) },
                style = MaterialTheme.typography.headlineSmall,
                color = if (resultText.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = 0.5f
                )
                else MaterialTheme.colorScheme.onSurface
            )
        }

        if (resultText.isNotEmpty()) {
            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(resultText))
                    Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT)
                        .show()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// === 頂部語言選擇欄 ===
@Composable
fun LanguageSelectionBar(
    sourceLang: String,
    targetLang: String,
    onSourceClick: (String) -> Unit,
    onTargetClick: (String) -> Unit,
    onSwap: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                LanguagePickerButton(
                    currentLangName = stringResource(
                        SUPPORTED_LANGUAGES[sourceLang] ?: R.string.lang_detect
                    ),
                    isSource = true,
                    onLangSelected = onSourceClick
                )
            }

            IconButton(onClick = onSwap) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                    contentDescription = "Swap Languages",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                LanguagePickerButton(
                    currentLangName = stringResource(
                        SUPPORTED_LANGUAGES[targetLang] ?: R.string.lang_zh_tw
                    ),
                    isSource = false,
                    onLangSelected = onTargetClick
                )
            }
        }
    }
}

@Composable
fun LanguagePickerButton(
    currentLangName: String,
    isSource: Boolean,
    onLangSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        ElevatedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .padding(5.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = currentLangName,
                modifier = Modifier
                    .basicMarquee()
                    .padding(vertical = 7.dp, horizontal = 10.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1,
                style = MaterialTheme.typography.labelLarge
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 300.dp)
        ) {
            SUPPORTED_LANGUAGES.forEach { (code, nameResId) ->
                if (!isSource && code == "auto") return@forEach
                DropdownMenuItem(
                    text = { Text(stringResource(nameResId)) },
                    onClick = { onLangSelected(code); expanded = false }
                )
            }
        }
    }
}