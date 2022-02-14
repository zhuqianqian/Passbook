package com.z299studio.pbfree.adapters

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.z299studio.pbfree.R
import com.z299studio.pbfree.data.AccountRepository
import com.z299studio.pbfree.data.Category
import com.z299studio.pbfree.databinding.CategoryBinding
import com.z299studio.pbfree.viewmodels.CategoryViewModel

class CategoryListAdapter (private val values: MutableList<Category>)
    : RecyclerView.Adapter<CategoryListAdapter.ViewHolder>() {

    class ViewHolder(private val binding: CategoryBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: CategoryViewModel, removeCategory: View.OnClickListener){
            binding.removeCategory = removeCategory
            binding.category = category
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.category,
                parent,
                false )
        )

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(CategoryViewModel(values[position])) {
            removeCategory(holder.bindingAdapterPosition)
        }
    }

    override fun getItemCount(): Int = values.size

    private fun removeCategory(position: Int) {
        AccountRepository.get().deleteCategory(values[position].name)
        values.removeAt(position)
        notifyItemRemoved(position)
    }

    fun add(category: Category) {
        if (AccountRepository.get().addCategory(category)) {
            this.values.add(category)
            notifyItemInserted(itemCount)
        }
    }

}