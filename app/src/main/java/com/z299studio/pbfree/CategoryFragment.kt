package com.z299studio.pbfree

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.z299studio.pbfree.adapters.CategoryListAdapter
import com.z299studio.pbfree.data.AccountRepository
import com.z299studio.pbfree.databinding.FragmentCategoryBinding
import com.z299studio.pbfree.viewmodels.CategoryViewModel

class CategoryFragment : Fragment() {

    private var _binding: FragmentCategoryBinding? = null
    private val binding get() = _binding!!

    private val categoryViewModel: CategoryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        binding.newCategory = categoryViewModel
        val categoryListAdapter = CategoryListAdapter(AccountRepository.get().getAllCategories().toMutableList())
        binding.categoryList.adapter = categoryListAdapter
        binding.categoryList.layoutManager = LinearLayoutManager(context)
        binding.nameEdit.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE) {
                binding.ok.performClick()
            }
            return@setOnEditorActionListener true
        }
        binding.ok.setOnClickListener {
            if (categoryViewModel.name.value.isNullOrBlank()) {
                return@setOnClickListener
            }
            val inputMethodManager = it.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
            categoryListAdapter.add(categoryViewModel.toCategory())
            categoryViewModel.name.value = ""
            categoryViewModel.icon = -1
        }
        binding.icon.setOnClickListener {
            findNavController().navigate(R.id.chooseIconDialog)
        }

        binding.lifecycleOwner = this.viewLifecycleOwner
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        AccountRepository.get().saveData(requireContext())
        _binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        super.onCreateOptionsMenu(menu, inflater)
    }
}