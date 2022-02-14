package com.z299studio.pbfree

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.z299studio.pbfree.databinding.DialogPasswordGeneratorBinding
import com.z299studio.pbfree.viewmodels.PassGenViewModel

class GeneratePassDialog : BottomSheetDialogFragment() {
    private var _binding: DialogPasswordGeneratorBinding? = null
    private val binding get() = _binding!!
    private val options: PassGenViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        isCancelable = false
        _binding = DialogPasswordGeneratorBinding.inflate(inflater, container, false)
        binding.options = options
        val onOptionsChanged = View.OnClickListener {
            options.password.value = PassGenViewModel.randomString(options)
        }
        options.password.value = PassGenViewModel.randomString(options)
        binding.close.setOnClickListener { dismiss() }
        binding.lifecycleOwner = viewLifecycleOwner
        binding.onOptionChanged = onOptionsChanged
        binding.ok.setOnClickListener {
            options.confirmed.value = true
            dismiss()
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}