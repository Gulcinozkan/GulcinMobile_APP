package com.example.gulcinmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.gulcinmobile.ui.theme.GulcinMobileTheme
import com.example.gulcinmobile.viewmodel.NewsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.gulcinmobile.ui.components.NewsCard
import com.example.gulcinmobile.ui.components.SettingsDialog
import com.example.gulcinmobile.datastore.DataStoreManager
import com.example.gulcinmobile.ui.components.strings
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.runBlocking
import androidx.navigation.NavController
import androidx.lifecycle.ViewModelProvider
import com.example.gulcinmobile.viewmodel.NewsViewModelFactory


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GulcinMobileTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "welcome") {
                    composable("welcome") { WelcomeScreen(navController) }
                    composable("main") {
                        SearchScreen(navController = navController)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val viewModel: NewsViewModel = viewModel(
        factory = NewsViewModelFactory(context)
    )
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Track language changes
    var currentLanguage by remember { mutableStateOf(viewModel.selectedLanguageCode) }

    // Get localized strings based on selected language
    val localStrings = strings[currentLanguage] ?: strings["en"] ?: mapOf()

    var showDialog by remember { mutableStateOf(false) }

    Scaffold(Modifier.fillMaxSize(), {
        TopAppBar(
            title = { Text(localStrings["app_title"] ?: "AI News") },
            actions = {
                IconButton(onClick = {
                    showDialog = true
                }) {
                    Icon(Icons.Default.Settings, contentDescription = localStrings["settings"] ?: "Settings")
                }
            }
        )
    }) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(Color(0xFFFFF8E1))
                .padding(horizontal = 16.dp)
        ) {
            Button(
                onClick = { viewModel.getTechNews() },
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                Text(localStrings["show_news"] ?: "Show AI and Technology News 💡")
            }

            if (uiState.error.isNotEmpty()) {
                Text(
                    "${localStrings["error"] ?: "Error"}: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                // LazyColumn ile kaydırma özelliği ekliyoruz
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Tüm haberleri gösteriyoruz, take(5) kısıtlamasını kaldırdık
                    items(uiState.articles) { article ->
                        NewsCard(article = article)
                    }
                }
            }
        }

        if (showDialog) {
            SettingsDialog(
                currentLanguage = currentLanguage,
                onDismiss = { showDialog = false },
                onSave = { newLang ->
                    coroutineScope.launch {
                        viewModel.updateLanguage(newLang)
                        currentLanguage = newLang // Apply language change immediately
                        showDialog = false
                    }
                }
            )
        }
    }
}