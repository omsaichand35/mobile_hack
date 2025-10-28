// DashboardActivity.kt
package com.example.friend

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var newsCollector: NewsDataCollector
    private lateinit var dashboardAdapter: DashboardAdapter
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        newsCollector = NewsDataCollector(this)

        setupViews()
        loadDashboardData()
    }

    private fun setupViews() {
        val chatBtn = findViewById<Button>(R.id.chatBtn)
        val refreshBtn = findViewById<Button>(R.id.refreshBtn)
        val lastUpdated = findViewById<TextView>(R.id.lastUpdated)
        val overallSentiment = findViewById<TextView>(R.id.overallSentiment)
        val categoriesRecycler = findViewById<RecyclerView>(R.id.categoriesRecycler)

        // Setup recycler view
        dashboardAdapter = DashboardAdapter(emptyList()) { categoryName ->
            openChatWithCategory(categoryName)
        }
        categoriesRecycler.adapter = dashboardAdapter
        categoriesRecycler.layoutManager = LinearLayoutManager(this)

        refreshBtn.setOnClickListener {
            loadDashboardData()
        }
        chatBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun loadDashboardData() {
        coroutineScope.launch {
            findViewById<TextView>(R.id.loadingText).text = "🔄 Loading trending news..."

            try {
                val dashboardData = newsCollector.fetchTrendingDashboard()
                updateDashboardUI(dashboardData)
            } catch (e: Exception) {
                findViewById<TextView>(R.id.loadingText).text = "❌ Failed to load dashboard"
                e.printStackTrace()
            }
        }
    }

    private fun updateDashboardUI(data: DashboardData) {
        findViewById<TextView>(R.id.loadingText).text = "📊 Live Sentiment Dashboard"
        findViewById<TextView>(R.id.lastUpdated).text = "Last updated: ${data.lastUpdated}"

        // Update overall sentiment
        val sentiment = getOverallSentimentText(data.overallSentiment)
        findViewById<TextView>(R.id.overallSentiment).text = sentiment

        // Update categories
        dashboardAdapter.updateData(data.trendingCategories)
    }

    private fun getOverallSentimentText(sentiment: OverallSentiment): String {
        return """
            🌡️ Overall Public Mood
            ✅ Positive: ${"%.1f".format(sentiment.totalPositive * 100)}%
            ❌ Negative: ${"%.1f".format(sentiment.totalNegative * 100)}%
            📊 Sources: ${sentiment.totalItems}
            🎉 Most Positive: ${sentiment.mostPositiveCategory}
            😟 Most Negative: ${sentiment.mostNegativeCategory}
        """.trimIndent()
    }

    private fun openChatWithCategory(categoryName: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("AUTO_QUERY", "news about $categoryName")
        }
        startActivity(intent)
    }
}