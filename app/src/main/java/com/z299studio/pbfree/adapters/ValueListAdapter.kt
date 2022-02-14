package com.z299studio.pbfree.adapters

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.method.PasswordTransformationMethod
import android.text.method.SingleLineTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.z299studio.pbfree.R
import com.z299studio.pbfree.data.ValueType
import com.z299studio.pbfree.databinding.ItemDetailViewBinding
import com.z299studio.pbfree.viewmodels.FieldViewModel

class ValueListAdapter(private val values: List<FieldViewModel>) : RecyclerView.Adapter<ValueListAdapter.ViewHolder>()  {

    class ViewHolder (private val binding: ItemDetailViewBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(field: FieldViewModel) {
            binding.field = field
            if (field.type.value == ValueType.Password || field.type.value == ValueType.Pin) {
                binding.fieldValue.transformationMethod = PasswordTransformationMethod.getInstance()
            }
            binding.fieldCard.setOnClickListener {
                if (field.type.value == ValueType.Password || field.type.value == ValueType.Pin) {
                    if (binding.fieldValue.transformationMethod == PasswordTransformationMethod.getInstance()) {
                        binding.fieldValue.transformationMethod = SingleLineTransformationMethod.getInstance()
                    } else {
                        binding.fieldValue.transformationMethod = PasswordTransformationMethod.getInstance()
                    }
                }
            }
            binding.executePendingBindings()
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_detail_view,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(values[position])
        holder.itemView.setOnLongClickListener {
            copyToClipboard(it,
                values[holder.bindingAdapterPosition].key.value ?: "",
                values[holder.bindingAdapterPosition].value)
            true
        }
    }

    override fun getItemCount(): Int {
        return values.size
    }

    private fun copyToClipboard(view: View, label: String, text: String) {
        val clipboardManager = view.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText(label, text)
        clipboardManager.setPrimaryClip(clipData)
        Toast.makeText(view.context, R.string.text_copied, Toast.LENGTH_SHORT).show()
    }
}