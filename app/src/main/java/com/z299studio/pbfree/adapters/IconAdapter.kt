package com.z299studio.pbfree.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.z299studio.pbfree.R
import com.z299studio.pbfree.databinding.IconBinding
import com.z299studio.pbfree.viewmodels.CategoryViewModel

class IconAdapter(@DrawableRes private var icons: Array<Int>, private val selected: CategoryViewModel)
    : RecyclerView.Adapter<IconAdapter.ViewHolder>()  {

    class ViewHolder (private val binding: IconBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(@DrawableRes resId: Int, selected: Boolean, onItemClick: View.OnClickListener) {
            binding.icon.setImageResource(resId)
            binding.icon.isSelected = selected
            binding.icon.setOnClickListener(onItemClick)
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.icon,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(icons[position], selected.icon == position) {
            if (selected.icon != holder.bindingAdapterPosition) {
                if (selected.icon >= 0) {
                    notifyItemChanged(selected.icon)
                }
                it.isSelected = true
                selected.icon = holder.bindingAdapterPosition
            }
        }
    }

    override fun getItemCount(): Int = icons.size
}