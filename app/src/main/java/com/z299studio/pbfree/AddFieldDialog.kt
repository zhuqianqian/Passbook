package com.z299studio.pbfree

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.z299studio.pbfree.adapters.LabelItemsAdapter
import com.z299studio.pbfree.data.AccountRepository
import com.z299studio.pbfree.data.ValueType
import com.z299studio.pbfree.databinding.DialogAddFieldBinding
import com.z299studio.pbfree.viewmodels.FieldViewModel

class AddFieldDialog : BottomSheetDialogFragment() {
    private var _binding: DialogAddFieldBinding? = null

    private val binding get() = _binding!!
    private val addingField: FieldViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        isCancelable = false
        _binding = DialogAddFieldBinding.inflate(inflater, container, false)

        binding.field = addingField
        binding.close.setOnClickListener { dismiss() }
        binding.lifecycleOwner = this
        val suggestionAdapter = LabelItemsAdapter {
            binding.field?.key?.value = it
        }
        layoutLabels(suggestionAdapter)
        binding.suggestionLabels.adapter = suggestionAdapter
        binding.addButton.setOnClickListener {
            addingField.confirmed.value = true
            dismiss()
        }
        binding.onRadioClick = View.OnClickListener {
            val previousType = addingField.type.value
            addingField.type.value = when (it.id) {
                R.id.type_text -> ValueType.Text
                R.id.type_password -> ValueType.Password
                R.id.type_url -> ValueType.Url
                R.id.type_email -> ValueType.Email
                else -> ValueType.Text
            }
            if (previousType != addingField.type.value) {
                layoutLabels(suggestionAdapter)
            }
        }
        binding.editInput.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE && addingField.key.value?.isBlank() == false) {
                binding.addButton.performClick()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun layoutLabels(adapter: LabelItemsAdapter) {
        val rows = resources.getInteger(R.integer.suggest_label_columns)
        val labels = AccountRepository.get().getFieldKeys(addingField.type.value).take(rows * 2).toMutableList()
        if (labels.isEmpty()) {
            labels.add(when(addingField.type.value) {
                ValueType.Text -> getString(R.string.field_username)
                ValueType.Password -> getString(R.string.field_password)
                ValueType.Url -> getString(R.string.field_website)
                ValueType.Email -> getString(R.string.type_email)
                else -> ""
            })
        }
        adapter.setSuggestions(labels)
        val span = if (labels.size <= rows) { 1 } else { 2 }
        binding.suggestionLabels.layoutManager = StaggeredGridLayoutManager(span, RecyclerView.HORIZONTAL)

    }
}