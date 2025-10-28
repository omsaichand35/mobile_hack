// NewsDataCollector.kt
package com.example.friend

import android.content.Context
import org.jsoup.Jsoup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

class NewsDataCollector(private val context: Context) {

    suspend fun fetchNewsAboutTopic(topic: String): List<NewsItem> = withContext(Dispatchers.IO) {
        val newsItems = mutableListOf<NewsItem>()

        try {
            // Scrape from multiple sources for better coverage
            newsItems.addAll(scrapeGoogleNews(topic))
            newsItems.addAll(scrapeReddit(topic))
            newsItems.addAll(scrapeBingNews(topic))

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext newsItems.distinctBy { it.title } // Remove duplicates
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