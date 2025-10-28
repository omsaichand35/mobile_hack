// Update MainActivity.kt
package com.example.friend

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var classifier: TextClassifierHelper
    private lateinit var newsCollector: NewsDataCollector
    private lateinit var dashboardBtn: Button // Declare the dashboard button

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val messageList = findViewById<RecyclerView>(R.id.messageList)
        val input = findViewById<EditText>(R.id.messageInput)
        val sendBtn = findViewById<Button>(R.id.sendButton)
        dashboardBtn = findViewById<Button>(R.id.dashboardBtn) // Initialize the dashboard button

        chatAdapter = ChatAdapter(messages)
        messageList.adapter = chatAdapter
        messageList.layoutManager = LinearLayoutManager(this)

        classifier = TextClassifierHelper(this) { pos, neg ->
            runOnUiThread {
                val botReply = generateSentimentResponse(pos, neg)
                addBotMessage(botReply)
            }
        }

        newsCollector = NewsDataCollector(this)

        // Set up dashboard button click listener
        dashboardBtn.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
        }

        sendBtn.setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isNotBlank()) {
                addUserMessage(text)
                input.setText("")

                when {
                    isTopicAnalysisRequest(text) -> {
                        val topic = extractTopic(text)
                        analyzeTopicSentiment(topic)
                    }
                    else -> {
                        classifier.classify(text)
                    }
                }
            }
        }

        // Check for auto-query from dashboard
        val autoQuery = intent.getStringExtra("AUTO_QUERY")
        if (!autoQuery.isNullOrBlank()) {
            input.setText(autoQuery)
            // Auto-send after a short delay
            Handler(Looper.getMainLooper()).postDelayed({
                sendBtn.performClick()
            }, 500)
        }

        // Welcome message
        addBotMessage("🤖 Hello! I can:\n• Analyze your personal mood\n• Analyze public sentiment on any topic\n\nTry: 'news about AI' or 'how do people feel about climate change?'")
    }

    // ... rest of your existing methods (isTopicAnalysisRequest, extractTopic, analyzeTopicSentiment, etc.)
    private fun isTopicAnalysisRequest(text: String): Boolean {
        val keywords = listOf(
            "news about", "sentiment of", "how people feel about",
            "public opinion on", "what people think about", "mood about",
            "how do people feel about", "analysis of", "coverage about"
        )
        return keywords.any { text.contains(it, ignoreCase = true) }
    }

    private fun extractTopic(text: String): String {
        val patterns = listOf(
            "news about (.+)".toRegex(RegexOption.IGNORE_CASE),
            "sentiment of (.+)".toRegex(RegexOption.IGNORE_CASE),
            "how people feel about (.+)".toRegex(RegexOption.IGNORE_CASE),
            "public opinion on (.+)".toRegex(RegexOption.IGNORE_CASE),
            "what people think about (.+)".toRegex(RegexOption.IGNORE_CASE),
            "mood about (.+)".toRegex(RegexOption.IGNORE_CASE),
            "how do people feel about (.+)".toRegex(RegexOption.IGNORE_CASE),
            "analysis of (.+)".toRegex(RegexOption.IGNORE_CASE),
            "coverage about (.+)".toRegex(RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }
        return text
    }

    private fun analyzeTopicSentiment(topic: String) {
        addBotMessage("🔍 Searching the web for '$topic'...")
        coroutineScope.launch {
            try {
                val newsItems = newsCollector.fetchNewsAboutTopic(topic)
                if (newsItems.isEmpty()) {
                    addBotMessage("❌ Couldn't find enough information about '$topic'. Try a more popular topic.")
                    return@launch
                }
                addBotMessage("📊 Found ${newsItems.size} sources. Analyzing sentiment...")
                classifier.analyzeTopicSentiment(newsItems) { result ->
                    runOnUiThread {
                        displayTopicAnalysis(topic, result)
                    }
                }
            } catch (e: Exception) {
                addBotMessage("❌ Error analyzing topic. Please check your internet connection.")
            }
        }
    }

    private fun displayTopicAnalysis(topic: String, result: TopicAnalysisResult) {
        if (result.averagePositive == -1f || result.itemsAnalyzed == 0) {
            addBotMessage("❌ Not enough data to analyze '$topic'.")
            return
        }

        val (sentiment, emoji) = getOverallSentiment(result.averagePositive, result.averageNegative)
        val analysisMessage = """
            📈 **Public Sentiment Analysis: '$topic'**
            
            🌡️ **Overall Mood**: $sentiment $emoji
            ✅ **Positive**: ${"%.1f".format(result.averagePositive * 100)}%
            ❌ **Negative**: ${"%.1f".format(result.averageNegative * 100)}%
            📊 **Sources**: ${result.itemsAnalyzed}
        """.trimIndent()

        addBotMessage(analysisMessage)
    }

    private fun getOverallSentiment(pos: Float, neg: Float): Pair<String, String> {
        return when {
            pos > 0.7 && neg < 0.3 -> Pair("Very Positive", "😊")
            pos > 0.6 -> Pair("Generally Positive", "🙂")
            pos > 0.5 -> Pair("Slightly Positive", "😐")
            neg > 0.7 && pos < 0.3 -> Pair("Very Negative", "😠")
            neg > 0.6 -> Pair("Generally Negative", "😟")
            neg > 0.5 -> Pair("Slightly Negative", "😐")
            pos > neg -> Pair("Mixed but Leaning Positive", "🤔")
            neg > pos -> Pair("Mixed but Leaning Negative", "🤔")
            else -> Pair("Neutral/Mixed", "😐")
        }
    }

    private fun generateSentimentResponse(pos: Float, neg: Float): String {
        return when {
            pos > 0.8 && neg < 0.2 -> "That's wonderful! 😄"
            pos > 0.6 && neg < 0.4 -> "Glad you're feeling good! 🙂"
            pos in 0.4..0.6 && neg in 0.4..0.6 -> "I'm here for you 😐"
            pos < 0.4 && neg > 0.6 -> "I'm sorry you're feeling this way 😟"
            pos < 0.2 && neg > 0.8 -> "Take a deep breath 😠"
            pos < 0.3 && neg < 0.3 -> "Would you like to talk more? 😕"
            pos > 0.7 && neg > 0.3 -> "Let's work through this together 😬"
            pos < 0.3 && neg > 0.7 -> "I'm here with you 😞"
            else -> "Tell me more about how you're feeling"
        }
    }

    private fun addUserMessage(text: String) {
        messages.add(ChatMessage(text, true))
        chatAdapter.notifyItemInserted(messages.size - 1)
        findViewById<RecyclerView>(R.id.messageList).scrollToPosition(messages.size - 1)
    }

    private fun addBotMessage(text: String) {
        messages.add(ChatMessage(text, false))
        chatAdapter.notifyItemInserted(messages.size - 1)
        findViewById<RecyclerView>(R.id.messageList).scrollToPosition(messages.size - 1)
    }
}