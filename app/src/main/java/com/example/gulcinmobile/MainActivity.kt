package com.example.gulcinmobile


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.gulcinmobile.ui.theme.GulcinMobileTheme
import com.example.gulcinmobile.viewmodel.NewsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.gulcinmobile.ui.components.NewsCard
import com.example.gulcinmobile.ui.components.SettingsDialog
import com.example.gulcinmobile.datastore.DataStoreManager
import com.example.gulcinmobile.ui.components.strings
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.runBlocking
import androidx.navigation.NavController
import androidx.lifecycle.ViewModelProvider
import com.example.gulcinmobile.viewmodel.NewsViewModelFactory
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GulcinMobileTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "welcome") {
                    composable("welcome") { WelcomeScreen(navController) }
                    composable("main") {
                        MainScreen(navController = navController)
                    }
                    composable("general_news") {
                        NewsScreen(navController = navController, category = "general")
                    }
                    composable("tech_news") {
                        NewsScreen(navController = navController, category = "tech")
                    }
                    composable("political_news") {
                        NewsScreen(navController = navController, category = "political")
                    }
                    composable("sports_news") {
                        NewsScreen(navController = navController, category = "sports")
                    }
                    composable("business_news") {
                        NewsScreen(navController = navController, category = "business")
                    }
                    composable("art_news") {
                        NewsScreen(navController = navController, category = "art")
                    }
                    composable("entertainment_news") {
                        NewsScreen(navController = navController, category = "entertainment")
                    }
                }
            }
        }
    }
}

// Menü öğeleri için veri sınıfı
data class DrawerItem(
    val title: String,
    val route: String,
    val icon: ImageVector,
    val contentDescription: String
)



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
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
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Drawer menü öğeleri
    val drawerItems = listOf(
        DrawerItem(
            title = localStrings["general_news"] ?: "General News",
            route = "general_news",
            icon = Icons.Outlined.Newspaper,
            contentDescription = "General News"
        ),
        DrawerItem(
            title = localStrings["tech_news"] ?: "Technology News",
            route = "tech_news",
            icon = Icons.Outlined.Computer,
            contentDescription = "Technology News"
        ),
        DrawerItem(
            title = localStrings["political_news"] ?: "Political News",
            route = "political_news",
            icon = Icons.Outlined.AccountBalance,
            contentDescription = "Political News"
        ),
        DrawerItem(
            title = localStrings["sports_news"] ?: "Sports News",
            route = "sports_news",
            icon = Icons.Outlined.SportsSoccer,
            contentDescription = "Sports News"
        ),
        DrawerItem(
            title = localStrings["business_news"] ?: "Business News",
            route = "business_news",
            icon = Icons.Outlined.Business,
            contentDescription = "Business News"
        ),
        DrawerItem(
            title = localStrings["art_news"] ?: "Art News",
            route = "art_news",
            icon = Icons.Outlined.Palette,
            contentDescription = "Art News"
        ),
        DrawerItem(
            title = localStrings["entertainment_news"] ?: "Entertainment News",
            route = "entertainment_news",
            icon = Icons.Outlined.Movie,
            contentDescription = "Entertainment News"
        )
    )

    // Drawer layout
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = localStrings["news_categories"] ?: "News Categories",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                // Menü öğeleri
                drawerItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = item.contentDescription) },
                        label = { Text(item.title) },
                        selected = false,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                navController.navigate(item.route)
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    ) {
        // Ana ekran içeriği
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(localStrings["app_title"] ?: "AI News") },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = localStrings["menu"] ?: "Menu"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            showDialog = true
                        }) {
                            Icon(Icons.Default.Settings, contentDescription = localStrings["settings"] ?: "Settings")
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
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = localStrings["welcome_message"] ?: "Welcome to AI News App!",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = localStrings["select_category"] ?: "Please select a news category from the menu",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // Hızlı erişim butonları
                Button(
                    onClick = { navController.navigate("tech_news") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(localStrings["tech_news"] ?: "Technology News")
                }

                Button(
                    onClick = { navController.navigate("general_news") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(localStrings["general_news"] ?: "General News")
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    navController: NavController,
    category: String
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
    var drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Kategori başlığını belirle
    val categoryTitle = when (category) {
        "general" -> localStrings["general_news"] ?: "General News"
        "tech" -> localStrings["tech_news"] ?: "Technology News"
        "political" -> localStrings["political_news"] ?: "Political News"
        "sports" -> localStrings["sports_news"] ?: "Sports News"
        "business" -> localStrings["business_news"] ?: "Business News"
        "art" -> localStrings["art_news"] ?: "Art News"
        "entertainment" -> localStrings["entertainment_news"] ?: "Entertainment News"
        else -> localStrings["news"] ?: "News"
    }

    // Kategori buton metni
    val buttonText = when (category) {
        "general" -> localStrings["show_general_news"] ?: "Show Latest General News"
        "tech" -> localStrings["show_tech_news"] ?: "Show Latest Technology News"
        "political" -> localStrings["show_political_news"] ?: "Show Latest Political News"
        "sports" -> localStrings["show_sports_news"] ?: "Show Latest Sports News"
        "business" -> localStrings["show_business_news"] ?: "Show Latest Business News"
        "art" -> localStrings["show_art_news"] ?: "Show Latest Art News"
        "entertainment" -> localStrings["show_entertainment_news"] ?: "Show Latest Entertainment News"
        else -> localStrings["show_news"] ?: "Show Latest News"
    }

    // Drawer menü öğeleri
    val drawerItems = listOf(
        DrawerItem(
            title = localStrings["general_news"] ?: "General News",
            route = "general_news",
            icon = Icons.Default.Newspaper,
            contentDescription = "General News"
        ),
        DrawerItem(
            title = localStrings["tech_news"] ?: "Technology News",
            route = "tech_news",
            icon = Icons.Default.Computer,
            contentDescription = "Technology News"
        ),
        DrawerItem(
            title = localStrings["political_news"] ?: "Political News",
            route = "political_news",
            icon = Icons.Default.AccountBalance,
            contentDescription = "Political News"
        ),
        DrawerItem(
            title = localStrings["sports_news"] ?: "Sports News",
            route = "sports_news",
            icon = Icons.Default.SportsSoccer,
            contentDescription = "Sports News"
        ),
        DrawerItem(
            title = localStrings["business_news"] ?: "Business News",
            route = "business_news",
            icon = Icons.Default.Business,
            contentDescription = "Business News"
        ),
        DrawerItem(
            title = localStrings["art_news"] ?: "Art News",
            route = "art_news",
            icon = Icons.Default.Palette,
            contentDescription = "Art News"
        ),
        DrawerItem(
            title = localStrings["entertainment_news"] ?: "Entertainment News",
            route = "entertainment_news",
            icon = Icons.Default.Movie,
            contentDescription = "Entertainment News"
        )
    )

    // Sayfaya girdiğimizde ilgili kategorinin haberlerini yükleme
    LaunchedEffect(category) {
        when (category) {
            "general" -> viewModel.getGeneralNews()
            "tech" -> viewModel.getTechNews()
            "political" -> viewModel.getPoliticalNews()
            "sports" -> viewModel.getSportsNews()
            "business" -> viewModel.getBusinessNews()
            "art" -> viewModel.getArtNews()
            "entertainment" -> viewModel.getEntertainmentNews()
        }
    }

    // Drawer layout
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = localStrings["news_categories"] ?: "News Categories",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                // Menü öğeleri
                drawerItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = item.contentDescription) },
                        label = { Text(item.title) },
                        selected = item.route == "${category}_news",
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                navController.navigate(item.route)
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    ) {
        // Ana ekran içeriği
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(categoryTitle) },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = localStrings["menu"] ?: "Menu"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            showDialog = true
                        }) {
                            Icon(Icons.Default.Settings, contentDescription = localStrings["settings"] ?: "Settings")
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
                    .padding(horizontal = 16.dp)
            ) {
                Button(
                    onClick = {
                        when (category) {
                            "general" -> viewModel.getGeneralNews()
                            "tech" -> viewModel.getTechNews()
                            "political" -> viewModel.getPoliticalNews()
                            "sports" -> viewModel.getSportsNews()
                            "business" -> viewModel.getBusinessNews()
                            "art" -> viewModel.getArtNews()
                            "entertainment" -> viewModel.getEntertainmentNews()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Text(buttonText)
                }

                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.error.isNotEmpty()) {
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
                        // Tüm haberleri gösteriyoruz
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
}