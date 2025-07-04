package com.example.gulcinmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.runBlocking
import androidx.navigation.NavController


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val dataStoreManager = DataStoreManager(applicationContext)
        var languageLoaded = false
        var languageCode = "tr"


        runBlocking {
            languageCode = dataStoreManager.getLanguage()
            languageLoaded = true
        }

        setContent {
            GulcinMobileTheme {
                val navController = rememberNavController()

                if (languageLoaded) {
                    NavHost(navController = navController, startDestination = "welcome") {
                        composable("welcome") { WelcomeScreen(navController) }
                        composable("main") {
                            SearchScreen(languageCode = languageCode, navController = navController)
                        }

                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    languageCode: String,
    navController: NavController,
    viewModel: NewsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Dil seçimi viewModel'a geçiliyor
    LaunchedEffect(languageCode) {
        viewModel.selectedLanguageCode = languageCode
    }
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }

    var showDialog by remember { mutableStateOf(false) }


    Scaffold(Modifier.fillMaxSize(), {
        TopAppBar(
            title = { Text("AI News") },
            actions = {
                IconButton(onClick = {
                    showDialog = true
                }) {
                    Icon(Icons.Default.Settings, contentDescription = "Ayarlar")
                }

            }
        )
    }

    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(Color(0xFFFFF8E1))
                .padding(16.dp)
        ) {

            Button(onClick = { viewModel.getTechNews() }) {
                Text("Yapay Zekâ ve Teknoloji Gelişmelerini Göster 💡")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.error.isNotEmpty()) {
                Text("Hata: ${uiState.error}", color = MaterialTheme.colorScheme.error)
            } else {
                uiState.articles.take(5).forEach { article ->
                    NewsCard(article = article)
                }

            }

        }
        if (showDialog) {
            SettingsDialog(
                currentLanguage = languageCode,
                onDismiss = { showDialog = false },
                onSave = { newLang ->
                    coroutineScope.launch {
                        dataStoreManager.saveLanguage(newLang)
                        showDialog = false
                        navController.popBackStack()
                        navController.navigate("main")
                    }
                }
            )
        }
    }
}