package com.example.friend

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private companion object {
        const val SENT = 1
        const val RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        // If message was sent by me, use SENT layout, else RECEIVED
        return if (messages[position].isUser) SENT else RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layout = if (viewType == SENT)
            R.layout.item_message_user   // bubble on right
        else
            R.layout.item_message_bot    // bubble on left

        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun getItemCount() = messages.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        (holder as MessageViewHolder).bind(message)
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(msg: ChatMessage) {
            itemView.findViewById<TextView>(R.id.messageText).text = msg.text
        }
    }
}
