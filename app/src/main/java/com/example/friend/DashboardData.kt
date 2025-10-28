// DashboardData.kt
package com.example.friend

data class TrendingCategory(
    val name: String,
    val newsItems: List<NewsItem>,
    val averagePositive: Float,
    val averageNegative: Float,
    val totalItems: Int
)

data class DashboardData(
    val trendingCategories: List<TrendingCategory>,
    val overallSentiment: OverallSentiment,
    val lastUpdated: String
)

data class OverallSentiment(
    val totalPositive: Float,
    val totalNegative: Float,
    val totalItems: Int,
    val mostPositiveCategory: String,
    val mostNegativeCategory: String
)