package com.example.friend

import android.content.Context
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.text.textclassifier.TextClassifier
import com.google.mediapipe.tasks.text.textclassifier.TextClassifier.TextClassifierOptions
import com.google.mediapipe.tasks.text.textclassifier.TextClassifierResult
import java.util.concurrent.ScheduledThreadPoolExecutor

class TextClassifierHelper(
    context: Context,
    private val listener: (Float, Float) -> Unit
) {
    private val executor = ScheduledThreadPoolExecutor(1)
    private val textClassifier: TextClassifier

    init {
        val baseOptions = BaseOptions.builder()
            .setDelegate(Delegate.CPU)
            .setModelAssetPath("text_classifier.tflite")
            .build()
        val options = TextClassifierOptions.builder()
            .setBaseOptions(baseOptions)
            .build()

        textClassifier = TextClassifier.createFromOptions(context, options)
    }

    fun classify(text: String) {
        executor.execute {
            try {
                val result = textClassifier.classify(text)
                processClassificationResult(result) { pos, neg ->
                    // This check is important, as the text might be too short to be classified.
                    if (pos != 0f || neg != 0f) {
                        listener(pos, neg)
                    } else {
                        // Handle cases where no classification was made.
                        listener(0f, 0f) // Or some other default/error state
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace() // 👈 this will print crash info in Logcat
                listener(-1f, -1f)
            }
        }
    }
    fun analyzeTopicSentiment(newsItems: List<NewsItem>, callback: (TopicAnalysisResult) -> Unit) {
        executor.execute {
            try {
                var totalPositive = 0f
                var totalNegative = 0f
                var analyzedCount = 0
                val sentimentBreakdown = mutableListOf<ItemSentiment>()
                val confidenceScores = mutableListOf<Float>()

                for (item in newsItems) {
                    val combinedText = "${item.title}. ${item.content}"
                    if (combinedText.length > 20) { // Only analyze substantial content
                        val result = textClassifier.classify(combinedText)

                        var pos = 0f; var neg = 0f
                        processClassificationResult(result) { p, n ->
                            pos = p; neg = n
                        }

                        val confidence = maxOf(pos, neg)

                        // Only count confident classifications
                        if (confidence > 0.3) {
                            totalPositive += pos
                            totalNegative += neg
                            analyzedCount++
                            confidenceScores.add(confidence)

                            sentimentBreakdown.add(
                                ItemSentiment(
                                    item.title,
                                    pos,
                                    neg,
                                    combinedText.substring(0, minOf(80, combinedText.length)) + "...",
                                    confidence
                                )
                            )
                        }
                    }
                }

                val avgPositive = if (analyzedCount > 0) totalPositive / analyzedCount else 0f
                val avgNegative = if (analyzedCount > 0) totalNegative / analyzedCount else 0f
                val avgConfidence = if (confidenceScores.isNotEmpty())
                    confidenceScores.average().toFloat() else 0f

                // Sort by most positive/negative for insights
                val topPositive = sentimentBreakdown.sortedByDescending { it.positiveScore }.take(3)
                val topNegative = sentimentBreakdown.sortedByDescending { it.negativeScore }.take(3)

                callback(
                    TopicAnalysisResult(
                        avgPositive,
                        avgNegative,
                        analyzedCount,
                        sentimentBreakdown,
                        topPositive,
                        topNegative,
                        avgConfidence
                    )
                )

            } catch (e: Exception) {
                e.printStackTrace()
                callback(TopicAnalysisResult(-1f, -1f, 0, emptyList(), emptyList(), emptyList(), 0f))
            }
        }
    }

    private fun processClassificationResult(
        result: TextClassifierResult,
        callback: (Float, Float) -> Unit
    ) {
        val topCategory = result.classificationResult().classifications().firstOrNull()
        var pos = 0f
        var neg = 0f

        topCategory?.categories()?.forEach { category ->
            when(category.categoryName().lowercase()) { // Use lowercase() for consistency
                "positive" -> pos = category.score()
                "negative" -> neg = category.score()
            }
        }
        callback(pos, neg)
    }
}

data class TopicAnalysisResult(
    val averagePositive: Float,
    val averageNegative: Float,
    val itemsAnalyzed: Int,
    val breakdown: List<ItemSentiment>,
    val topPositive: List<ItemSentiment>,
    val topNegative: List<ItemSentiment>,
    val averageConfidence: Float
)

data class ItemSentiment(
    val title: String,
    val positiveScore: Float,
    val negativeScore: Float,
    val preview: String,
    val confidence: Float
)
