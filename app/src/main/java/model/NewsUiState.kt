import com.example.gulcinmobile.model.GNewsArticle

data class NewsUiState(
    val articles: List<GNewsArticle> = emptyList(),
    val error: String = ""
)
