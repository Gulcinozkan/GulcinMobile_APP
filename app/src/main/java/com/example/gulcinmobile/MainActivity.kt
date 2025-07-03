package com.example.gulcinmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GulcinMobileTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "welcome") {
                    composable("welcome") { WelcomeScreen(navController) }
                    composable("main") { SearchScreen() }
                }
            }
        }
    }
}

@Composable
fun SearchScreen(viewModel: NewsViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8E1))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = { viewModel.getTechNews() }) {
            Text("Yapay Zekâ ve Teknoloji Gelişmelerini Göster 💡")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.error.isNotEmpty()) {
            Text("Hata: ${uiState.error}", color = MaterialTheme.colorScheme.error)
        } else {
            uiState.articles.take(5).forEach { article ->
                var translatedTitle by remember { mutableStateOf<String?>(null) }
                var isTranslating by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(text = translatedTitle ?: article.title)
                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            isTranslating = true
                            viewModel.translateWithMicrosoft(article.title) { result ->
                                translatedTitle = result
                                isTranslating = false
                            }
                        },
                        enabled = !isTranslating && translatedTitle == null
                    ) {
                        Text(if (isTranslating) "Çevriliyor..." else "Çevir")
                    }
                }

            }
        }
    }
}