package com.example.gulcinmobile.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsDialog(
    currentLanguage: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val languages = mapOf(
        "tr" to "Türkçe",
        "en" to "English",
        "es" to "Español",
        "fr" to "Français",
        "de" to "Deutsch",
        "ar" to "العربية",
        "ja" to "日本語",
        "ru" to "Русский",
        "zh" to "简体中文"
    )

    var selectedLang by remember { mutableStateOf(currentLanguage) }

    // Dile göre metinleri al
    val localStrings = strings[currentLanguage] ?: strings["en"] ?: mapOf()

    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(onClick = { onSave(selectedLang) }) {
                Text(localStrings["save"] ?: "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(localStrings["cancel"] ?: "Cancel")
            }
        },
        title = { Text(localStrings["language_selection"] ?: "Select Language") },
        text = {
            Column {
                languages.forEach { (code, name) ->
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedLang == code,
                            onClick = { selectedLang = code }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = name)
                    }
                }
            }
        }
    )
}