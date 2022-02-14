package com.z299studio.pbfree

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.z299studio.pbfree.adapters.IconAdapter
import com.z299studio.pbfree.databinding.DialogChooseIconBinding
import com.z299studio.pbfree.viewmodels.CategoryViewModel

class ChooseIconDialog : BottomSheetDialogFragment() {
    private var _binding: DialogChooseIconBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CategoryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        isCancelable = false
        _binding = DialogChooseIconBinding.inflate(inflater, container, false)
        binding.close.setOnClickListener {
            viewModel.icon = MainActivity.DEFAULT_ICON
            dismiss()
        }
        binding.icons.layoutManager = GridLayoutManager(requireContext(), resources.getInteger(R.integer.icon_dialog_span))
        val iconAdapter = IconAdapter(MainActivity.ICONS, viewModel)
        binding.icons.adapter = iconAdapter
        binding.ok.setOnClickListener { dismiss() }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}