package com.z299studio.pbfree.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.z299studio.pbfree.R
import com.z299studio.pbfree.databinding.LabelBinding

typealias OnSuggestionPick = (String) -> Unit

class LabelItemsAdapter(private val listener: OnSuggestionPick)
    : RecyclerView.Adapter<LabelItemsAdapter.ViewHolder>()  {

    private lateinit var suggestions: List<String>

    class ViewHolder (private val binding: LabelBinding, private val listener: OnSuggestionPick)
        : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.setClickListener {
                binding.suggestion?.let { suggestion ->
                    listener(suggestion)
                }
            }
        }

        fun bind(suggestion: String) {
            binding.suggestion = suggestion
            binding.executePendingBindings()
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.label,
                parent,
                false
            ), listener
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(suggestions[position])
    }

    override fun getItemCount(): Int {
        return suggestions.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setSuggestions(suggestions: List<String>) {
        this.suggestions = suggestions
        notifyDataSetChanged()
    }
}