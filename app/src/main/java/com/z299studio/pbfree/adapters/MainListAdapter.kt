package com.z299studio.pbfree.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.z299studio.pbfree.HomeFragmentDirections
import com.z299studio.pbfree.R
import com.z299studio.pbfree.databinding.MainItemBinding
import com.z299studio.pbfree.viewmodels.ItemViewModel
import com.z299studio.pbfree.viewmodels.MainItemViewModel

class MainListAdapter(private var items: List<MainItemViewModel>, private val selected: ItemViewModel) :
    RecyclerView.Adapter<MainListAdapter.ViewHolder>() {

    class ViewHolder (private val binding: MainItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MainItemViewModel, clickListener: View.OnClickListener) {
            binding.item = item
            binding.itemIconText.visibility = View.VISIBLE
            binding.clickListener = clickListener
            binding.executePendingBindings()
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.main_item,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position]) {
            val item = items[holder.bindingAdapterPosition]
            selected.from(item.index)
            val direction = HomeFragmentDirections.action2Detail(item.title)
            it.findNavController().navigate(direction)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(items: List<MainItemViewModel>) {
        this.items = items
        notifyDataSetChanged()
    }
}