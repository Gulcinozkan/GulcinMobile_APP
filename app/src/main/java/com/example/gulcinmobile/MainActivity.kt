package com.example.gulcinmobile


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.gulcinmobile.R
import com.example.gulcinmobile.ui.components.NewsCard
import com.example.gulcinmobile.ui.components.SettingsDialog
import com.example.gulcinmobile.ui.components.strings
import com.example.gulcinmobile.ui.theme.GulcinMobileTheme
import com.example.gulcinmobile.viewmodel.NewsViewModel
import com.example.gulcinmobile.viewmodel.NewsViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder


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
                    composable("ai_news") {
                        NewsScreen(navController = navController, category = "ai")
                    }
                    composable(
                        route = "news_detail/{encodedUrl}",
                        arguments = listOf(navArgument("encodedUrl") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val encodedUrl = backStackEntry.arguments?.getString("encodedUrl") ?: ""
                        NewsDetailScreen(navController = navController, newsUrl = encodedUrl)
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

    // LaunchedEffect to load language when screen first opens
    LaunchedEffect(key1 = Unit) {
        val savedLanguage = viewModel.loadSavedLanguage()
        currentLanguage = savedLanguage
    }

    // Get localized strings based on selected language
    val localStrings = strings[currentLanguage] ?: strings["en"] ?: mapOf()

    var showDialog by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()


    // Drawer menü öğeleri
    val drawerItems = listOf(
        DrawerItem(
            title = localStrings["home"] ?: "Ana Menü",
            route = "main",
            icon = Icons.Default.Home,
            contentDescription = "Ana Menü"
        ),
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
            title = localStrings["ai_news"] ?: "AI News",
            route = "ai_news",
            icon = Icons.Default.SmartToy,
            contentDescription = "AI News"
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
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                drawerItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = item.contentDescription) },
                        label = { Text(item.title) },
                        selected = currentRoute == item.route,
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



    ){
        // Ana ekran içeriği
        Scaffold(
            topBar = {

                TopAppBar(
                    title = { Text("GulcinMobile", color = Color.Black) },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = localStrings["menu"] ?: "Menu",
                                tint = Color.Black
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            showDialog = true
                        }) {
                            Icon(Icons.Default.Settings,
                                contentDescription = localStrings["settings"] ?: "Settings",
                                tint = Color.Black
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFF5F5DC),
                        titleContentColor = Color.Black,
                        navigationIconContentColor = Color.Black,
                        actionIconContentColor = Color.Black
                    )
                )
            }
        ) { paddingValues ->

            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .background(Color(0xFFFFF8E1))
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = localStrings["welcome_title"] ?: "Welcome to GulcinMobile",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                            fontSize = 54.sp
                        ),
                        color = Color(0xFF1B5E20),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 64.dp)
                    )

                    Text(
                        text = localStrings["welcome_description"] ?: "Follow the latest developments in every category from AI to politics, from technology to sports by clicking on the topic that interests you from the menu above, and be instantly informed about the world agenda.",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 18.sp
                        ),
                        color = Color(0xFF33691E),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))


                Image(
                    painter = painterResource(id = R.drawable.main_menu),
                    contentDescription = "Gulcin Mobile Logo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(700.dp)
                        .padding(bottom = 32.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            }


            if (showDialog) {
                SettingsDialog(
                    currentLanguage = currentLanguage,
                    onDismiss = { showDialog = false },
                    onSave = { newLang ->
                        coroutineScope.launch {
                            viewModel.updateLanguage(newLang)
                            currentLanguage = newLang
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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Haberlerin gösterilip gösterilmeyeceğini kontrol eden state
    var showNews by remember { mutableStateOf(false) }

// Haberleri yükleyen ve gösteren fonksiyon
    val loadAndShowNews = {
        when (category) {
            "general" -> viewModel.getGeneralNews()
            "tech" -> viewModel.getTechNews()
            "political" -> viewModel.getPoliticalNews()
            "sports" -> viewModel.getSportsNews()
            "business" -> viewModel.getBusinessNews()
            "art" -> viewModel.getArtNews()
            "entertainment" -> viewModel.getEntertainmentNews()
            "ai" -> viewModel.getAINews()
        }
        showNews = true
    }

    // Kategori başlığını belirle
    val categoryTitle = when (category) {
        "general" -> localStrings["general_news"] ?: "General News"
        "tech" -> localStrings["tech_news"] ?: "Technology News"
        "political" -> localStrings["political_news"] ?: "Political News"
        "sports" -> localStrings["sports_news"] ?: "Sports News"
        "business" -> localStrings["business_news"] ?: "Business News"
        "art" -> localStrings["art_news"] ?: "Art News"
        "entertainment" -> localStrings["entertainment_news"] ?: "Entertainment News"
        "ai" -> localStrings["ai_news"] ?: "AI News"
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
        "ai" -> localStrings["show_ai_news"] ?: "Show Latest AI News"
        else -> localStrings["show_news"] ?: "Show Latest News"
    }
    // Drawer menü öğeleri
    val drawerItems = listOf(
        DrawerItem(
            title = localStrings["home"] ?: "Ana Menü",
            route = "main",
            icon = Icons.Default.Home,
            contentDescription = "Ana Menü"
        ),
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
            title = localStrings["ai_news"] ?: "AI News",
            route = "ai_news",
            icon = Icons.Default.SmartToy,
            contentDescription = "AI News"
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
    // LaunchedEffect(category) {
    //     when (category) {
    //         "general" -> viewModel.getGeneralNews()
    //         "tech" -> viewModel.getTechNews()
    //         "political" -> viewModel.getPoliticalNews()
    //         "sports" -> viewModel.getSportsNews()
    //         "business" -> viewModel.getBusinessNews()
    //         "art" -> viewModel.getArtNews()
    //         "entertainment" -> viewModel.getEntertainmentNews()
    //         "ai" -> viewModel.getAINews()
    //     }
    // }


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
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                drawerItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = item.contentDescription) },
                        label = { Text(item.title) },
                        selected = currentRoute == item.route,
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
    )        {
        // Haber ekranı içeriği
        Scaffold(
            topBar = {

                TopAppBar(
                    title = { Text("GulcinMobile", color = Color.Black) },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = localStrings["menu"] ?: "Menu",
                                tint = Color.Black
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            showDialog = true
                        }) {
                            Icon(Icons.Default.Settings,
                                contentDescription = localStrings["settings"] ?: "Settings",
                                tint = Color.Black
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFF5F5DC), // Arka plan renginden biraz daha koyu sarı
                        titleContentColor = Color.Black,
                        navigationIconContentColor = Color.Black,
                        actionIconContentColor = Color.Black
                    )
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
                Spacer(modifier = Modifier.weight(3f))
                Button(
                    onClick = loadAndShowNews,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
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
                } else if (uiState.isLoading) {
                    // Yükleme göstergesi
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = localStrings["loading"] ?: "Loading...",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                } else if (showNews) {
                    // Haberler gösterilecek
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.articles) { article ->
                            NewsCard(
                                article = article,
                                onItemClick = {
                                    // URL'yi kodlayarak detay sayfasına yönlendirme
                                    val encodedUrl = URLEncoder.encode(article.url, "UTF-8")
                                    navController.navigate("news_detail/$encodedUrl")
                                }
                            )
                        }
                    }
                } else {
                    // Kategori görseli gösterilecek
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val imageRes = when (category) {
                            "general" -> R.drawable.general_news
                            "tech" -> R.drawable.tech_news
                            "political" -> R.drawable.political_news
                            "sports" -> R.drawable.sports_news
                            "business" -> R.drawable.business_news
                            "art" -> R.drawable.art_news
                            "entertainment" -> R.drawable.entertainment_news
                            "ai" -> R.drawable.ai_news
                            else -> R.drawable.main_menu
                        }



                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 1.dp)
                        ) {

                            Image(
                                painter = painterResource(id = imageRes),
                                contentDescription = categoryTitle,
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .height(500.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )

                            Spacer(modifier = Modifier.height(3.dp))


                            Text(
                                text = localStrings["tap_to_see_news"] ?: "Click the button to see the news.",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
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
                            showNews = false
                            viewModel.updateLanguage(newLang)
                            currentLanguage = newLang // Apply language change immediately
                            showDialog = false

                            delay(500)
                            loadAndShowNews()
                        }
                    }
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsDetailScreen(
    navController: NavController,
    newsUrl: String
) {
    val context = LocalContext.current
    val viewModel: NewsViewModel = viewModel(
        factory = NewsViewModelFactory(context)
    )
    val uiState by viewModel.uiState.collectAsState()

    // URL'yi çözümle
    val decodedUrl = remember(newsUrl) {
        try {
            URLDecoder.decode(newsUrl, "UTF-8")
        } catch (e: Exception) {
            Log.e("NewsDetailScreen", "URL decode error: ${e.message ?: "Unknown error"}")
            newsUrl
        }
    }

    // Seçilen dile göre yerelleştirme
    val localStrings = strings[viewModel.selectedLanguageCode] ?: strings["en"] ?: mapOf()
    val detailTitle = localStrings["news_detail"] ?: "Haber Detayı"
    val sourceButtonText = localStrings["go_to_source"] ?: "Haber Kaynağına Git"
    val loadingText = localStrings["loading"] ?: "Yükleniyor..."
    val contentNotFoundText = localStrings["content_not_found"] ?: "İçerik bulunamadı"
    val noImageText = localStrings["no_image"] ?: "Görsel Yok"
    val goBackText = localStrings["go_back"] ?: "Geri Dön"

    // Haber detaylarını bul
    var newsArticle by remember { mutableStateOf<com.example.gulcinmobile.model.GNewsArticle?>(null) }

    // LaunchedEffect ile haber arama işlemini gerçekleştir
    LaunchedEffect(decodedUrl) {
        newsArticle = viewModel.findArticleByUrl(decodedUrl)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detailTitle) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                // Yükleniyor göstergesi
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(loadingText)
                }
            } else if (newsArticle != null) {
                // Haber detayları
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Haber başlığı
                    Text(
                        text = newsArticle!!.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Haber görseli yerine haber başlığını gösteren bir kutu
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = newsArticle!!.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // İçerik başlığı
                    Text(
                        text = "İçerik",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Haber açıklaması - Card içinde daha belirgin gösteriyoruz
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            text = newsArticle!!.description ?: contentNotFoundText,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Kaynak bilgisi
                    Text(
                        text = "Kaynak: ${Uri.parse(newsArticle!!.url).host}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Haber kaynağına git butonu - daha belirgin hale getiriyoruz
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(newsArticle!!.url))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            Icons.Filled.OpenInBrowser,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(sourceButtonText, style = MaterialTheme.typography.titleMedium)
                    }
                }
            } else {
                // İçerik bulunamadı durumu
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.Error,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = contentNotFoundText,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Haber detayları hafızada bulunamadı. Geri dönüp haberi tekrar seçmeyi deneyin.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(goBackText)
                    }
                }

            }
        }
    }
}