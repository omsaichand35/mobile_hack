// DashboardAdapter.kt
package com.example.friend

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class DashboardAdapter(
    private var categories: List<TrendingCategory>,
    private val onCategoryClick: (String) -> Unit
) : RecyclerView.Adapter<DashboardAdapter.CategoryViewHolder>() {

    fun updateData(newCategories: List<TrendingCategory>) {
        this.categories = newCategories
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount() = categories.size

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryName: TextView = itemView.findViewById(R.id.categoryName)
        private val positiveScore: TextView = itemView.findViewById(R.id.positiveScore)
        private val negativeScore: TextView = itemView.findViewById(R.id.negativeScore)
        private val itemCount: TextView = itemView.findViewById(R.id.itemCount)
        private val topNews: TextView = itemView.findViewById(R.id.topNews)

        fun bind(category: TrendingCategory) {
            categoryName.text = category.name
            positiveScore.text = "✅ ${"%.1f".format(category.averagePositive * 100)}%"
            negativeScore.text = "❌ ${"%.1f".format(category.averageNegative * 100)}%"
            itemCount.text = "📊 ${category.totalItems} news"

            // Show top news headline
            val topHeadline = category.newsItems.firstOrNull()?.title ?: "No news"
            topNews.text = "📰 ${topHeadline.take(60)}..."

            // Color code based on sentiment
            val backgroundColor = when {
                category.averagePositive > 0.6 -> R.color.positive_light
                category.averageNegative > 0.6 -> R.color.negative_light
                else -> R.color.neutral_light
            }

            itemView.setBackgroundColor(
                ContextCompat.getColor(itemView.context, backgroundColor)
            )

            itemView.setOnClickListener {
                onCategoryClick(category.name)
            }
        }
    }
}