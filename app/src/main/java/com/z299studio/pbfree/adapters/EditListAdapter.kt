package com.z299studio.pbfree.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.z299studio.pbfree.R
import com.z299studio.pbfree.data.ValueTuple
import com.z299studio.pbfree.data.ValueType
import com.z299studio.pbfree.databinding.ItemDetailEditBinding
import com.z299studio.pbfree.viewmodels.FieldViewModel
import com.z299studio.pbfree.viewmodels.PassGenViewModel

class EditListAdapter(private val values: MutableList<FieldViewModel>,
                      private val passwordGenerator: PassGenViewModel)
    : RecyclerView.Adapter<EditListAdapter.ViewHolder>() {

    class ViewHolder (private val binding: ItemDetailEditBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(field: FieldViewModel, rowRemover: View.OnClickListener, onGeneratePassword: View.OnClickListener){
            binding.field = field
            binding.removeField = rowRemover
            binding.generatePassword = onGeneratePassword
            binding.executePendingBindings()
        }
    }

    fun updatePasswordAt(position: Int, password: String?) {
        if (position >= 0 && password != null) {
            val field = this.values[position]
            field.value = password
            this.notifyItemChanged(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_detail_edit,
                parent,
                false ))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(values[position], {
            values.removeAt(holder.bindingAdapterPosition)
            holder.itemView.clearFocus()
            notifyItemRemoved(holder.bindingAdapterPosition)
        }, {
            passwordGenerator.index = holder.bindingAdapterPosition
            passwordGenerator.cancelValue = values[holder.bindingAdapterPosition].value
            it.findNavController().navigate(R.id.generatePassDialog)
        })
    }

    override fun getItemCount(): Int {
        return values.size
    }

    fun add(key: String?, type: ValueType?) {
        if (key != null && type != null) {
            this.values.add(FieldViewModel(ValueTuple(key, "", type)))
            notifyItemInserted(itemCount)
        }
    }

    fun getValues(): MutableList<ValueTuple> {
        return values.map {
            ValueTuple(it.key.value?:"", it.value, it.type.value?:ValueType.Text)
        }.toMutableList()
    }

}