// NewsDataCollector.kt
package com.example.friend

import android.content.Context
import org.jsoup.Jsoup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.net.URLEncoder

class NewsDataCollector(private val context: Context) {

    // Predefined trending categories
    private val trendingCategories = listOf(
        "Technology", "Politics", "Sports", "Entertainment",
        "Business", "Health", "Science", "Environment"
    )

    suspend fun fetchTrendingDashboard(): DashboardData = withContext(Dispatchers.IO) {
        val trendingData = mutableListOf<TrendingCategory>()
        var totalPositive = 0f
        var totalNegative = 0f
        var totalItems = 0

        val mostPositiveCat = mutableMapOf<String, Float>()
        val mostNegativeCat = mutableMapOf<String, Float>()

        // Fetch news for each trending category
        for (category in trendingCategories) {
            try {
                val newsItems = fetchNewsAboutTopic(category).take(8) // Limit items per category
                if (newsItems.isNotEmpty()) {
                    // Analyze sentiment for this category
                    val sentiment = analyzeCategorySentiment(newsItems)
                    trendingData.add(
                        TrendingCategory(
                            name = category,
                            newsItems = newsItems,
                            averagePositive = sentiment.first,
                            averageNegative = sentiment.second,
                            totalItems = newsItems.size
                        )
                    )

                    totalPositive += sentiment.first
                    totalNegative += sentiment.second
                    totalItems += newsItems.size

                    mostPositiveCat[category] = sentiment.first
                    mostNegativeCat[category] = sentiment.second
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Calculate overall sentiment
        val avgPositive = if (trendingData.isNotEmpty()) totalPositive / trendingData.size else 0f
        val avgNegative = if (trendingData.isNotEmpty()) totalNegative / trendingData.size else 0f

        val mostPositiveCategory = mostPositiveCat.maxByOrNull { it.value }?.key ?: "N/A"
        val mostNegativeCategory = mostNegativeCat.maxByOrNull { it.value }?.key ?: "N/A"

        val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        val lastUpdated = dateFormat.format(Date())

        return@withContext DashboardData(
            trendingCategories = trendingData.sortedByDescending { it.averagePositive },
            overallSentiment = OverallSentiment(
                totalPositive = avgPositive,
                totalNegative = avgNegative,
                totalItems = totalItems,
                mostPositiveCategory = mostPositiveCategory,
                mostNegativeCategory = mostNegativeCategory
            ),
            lastUpdated = lastUpdated
        )
    }

    private suspend fun analyzeCategorySentiment(newsItems: List<NewsItem>): Pair<Float, Float> {
        var totalPositive = 0f
        var totalNegative = 0f
        var analyzedCount = 0

        for (item in newsItems) {
            val combinedText = "${item.title}. ${item.content}"
            if (combinedText.length > 20) {
                try {
                    // Simple sentiment analysis (you can enhance this with your classifier)
                    val sentiment = quickSentimentAnalysis(combinedText)
                    totalPositive += sentiment.first
                    totalNegative += sentiment.second
                    analyzedCount++
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return Pair(
            if (analyzedCount > 0) totalPositive / analyzedCount else 0f,
            if (analyzedCount > 0) totalNegative / analyzedCount else 0f
        )
    }

    private fun quickSentimentAnalysis(text: String): Pair<Float, Float> {
        val lowerText = text.lowercase()

        // Simple keyword-based sentiment (fallback when classifier is busy)
        val positiveWords = listOf("good", "great", "excellent", "positive", "win", "success", "happy", "best")
        val negativeWords = listOf("bad", "terrible", "negative", "loss", "fail", "sad", "worst", "crisis")

        var positiveScore = 0f
        var negativeScore = 0f

        positiveWords.forEach { word ->
            if (lowerText.contains(word)) positiveScore += 0.3f
        }

        negativeWords.forEach { word ->
            if (lowerText.contains(word)) negativeScore += 0.3f
        }

        // Normalize scores
        positiveScore = positiveScore.coerceAtMost(1f)
        negativeScore = negativeScore.coerceAtMost(1f)

        return Pair(positiveScore, negativeScore)
    }

    // ... keep your existing fetchNewsAboutTopic, scrapeGoogleNews, etc. methods
    suspend fun fetchNewsAboutTopic(topic: String): List<NewsItem> = withContext(Dispatchers.IO) {
        val newsItems = mutableListOf<NewsItem>()

        try {
            newsItems.addAll(scrapeGoogleNews(topic))
            newsItems.addAll(scrapeReddit(topic))
            newsItems.addAll(scrapeBingNews(topic))

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext newsItems.distinctBy { it.title }
    }

    private suspend fun scrapeGoogleNews(topic: String): List<NewsItem> = withContext(Dispatchers.IO) {
        val newsItems = mutableListOf<NewsItem>()

        try {
            val encodedTopic = URLEncoder.encode(topic, "UTF-8")
            val url = "https://news.google.com/search?q=$encodedTopic"

            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(15000)
                .get()

            // Google News structure
            val articles = doc.select("article")
            for (article in articles.take(15)) {
                val titleElement = article.select("h3 a")
                val title = titleElement.text()
                val sourceElement = article.select("div[data-n-tid]")
                val source = sourceElement.text()

                if (title.isNotBlank()) {
                    val content = "Source: $source - $title"
                    newsItems.add(NewsItem(title, content))
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext newsItems
    }

    private suspend fun scrapeReddit(topic: String): List<NewsItem> = withContext(Dispatchers.IO) {
        val newsItems = mutableListOf<NewsItem>()

        try {
            val encodedTopic = URLEncoder.encode(topic, "UTF-8")
            val url = "https://www.reddit.com/r/news/search/?q=$encodedTopic&restrict_sr=1"

            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(15000)
                .get()

            // Reddit post structure
            val posts = doc.select("h3._eYtD2XCVieq6emjKBH3m")
            val contents = doc.select("div._292iotee39Lmt0MkQZ2hPV")

            for (i in 0 until minOf(posts.size, 10)) {
                val title = posts[i].text()
                val content = contents.getOrNull(i)?.text() ?: "Discussion about $topic"
                if (title.isNotBlank()) {
                    newsItems.add(NewsItem(title, content))
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext newsItems
    }

    private suspend fun scrapeBingNews(topic: String): List<NewsItem> = withContext(Dispatchers.IO) {
        val newsItems = mutableListOf<NewsItem>()

        try {
            val encodedTopic = URLEncoder.encode(topic, "UTF-8")
            val url = "https://www.bing.com/news/search?q=$encodedTopic"

            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(15000)
                .get()

            // Bing News structure
            val cards = doc.select("div.news-card")
            for (card in cards.take(10)) {
                val titleElement = card.select("a.title")
                val title = titleElement.text()
                val descriptionElement = card.select("div.snippet")
                val description = descriptionElement.text()

                if (title.isNotBlank()) {
                    val content = if (description.isNotBlank()) description else "News about $topic"
                    newsItems.add(NewsItem(title, content))
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext newsItems
    }
}

data class NewsItem(val title: String, val content: String)