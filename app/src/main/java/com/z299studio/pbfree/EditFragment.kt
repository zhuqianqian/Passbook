package com.z299studio.pbfree

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.z299studio.pbfree.adapters.EditListAdapter
import com.z299studio.pbfree.data.AccountRepository
import com.z299studio.pbfree.data.Entry
import com.z299studio.pbfree.databinding.FragmentEditBinding
import com.z299studio.pbfree.viewmodels.FieldViewModel
import com.z299studio.pbfree.viewmodels.ItemViewModel
import com.z299studio.pbfree.viewmodels.PassGenViewModel

class EditFragment : Fragment() {

    private var _binding: FragmentEditBinding? = null
    private val binding get() = _binding!!

    private lateinit var editListAdapter: EditListAdapter
    private val itemViewModel: ItemViewModel by activityViewModels()
    private val addingField: FieldViewModel by activityViewModels()
    private val passwordGenerator: PassGenViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditBinding.inflate(inflater, container, false)
        val root: View = binding.root
        binding.item = itemViewModel
        editListAdapter = EditListAdapter(itemViewModel.values.value!!, passwordGenerator)
        binding.fieldList.adapter = editListAdapter
        binding.fieldList.layoutManager = LinearLayoutManager(context)
        binding.save.setOnClickListener {
            val savingEntry = Entry(itemViewModel.title, itemViewModel.category, editListAdapter.getValues())
            if (itemViewModel.index < 0) {
                AccountRepository.get().add(savingEntry)
            } else {
                AccountRepository.get().set(itemViewModel.index, savingEntry)
            }
            AccountRepository.get().saveData(requireContext())
            findNavController().popBackStack()
        }
        binding.addField.setOnClickListener {
            addingField.reset()
            findNavController().navigate(R.id.action_open_add_field_dialog)
        }
        binding.category.setOnClickListener {
            it.showContextMenu()
        }
        val nameIdMap = AccountRepository.get().getAllCategories().map {it.name to it.icon}.toMap()
        binding.hasCategories = nameIdMap.isNotEmpty()
        binding.category.setImageResource(MainActivity.getIcon(nameIdMap[itemViewModel.category]?:-1))
        binding.category.setOnCreateContextMenuListener { menu, _, _ ->
            menu.setHeaderTitle(R.string.select_category)
            AccountRepository.get().getAllCategories().forEach {
                val menuItem = menu.add(0, it.icon, 0, it.name)
                menuItem.setIcon(MainActivity.getIcon(it.icon))
            }
        }
        addingField.confirmed.observe(viewLifecycleOwner) {
            if (it) {
                editListAdapter.add(addingField.key.value, addingField.type.value)
                addingField.reset()
            }
        }
        passwordGenerator.confirmed.observe(viewLifecycleOwner) {
            if (it) {
                editListAdapter.updatePasswordAt(passwordGenerator.index, passwordGenerator.password.value)
                passwordGenerator.confirmed.value = false
            }
        }
        binding.lifecycleOwner = viewLifecycleOwner
        return root
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        itemViewModel.category = item.title.toString()
        itemViewModel.categoryIcon.value = MainActivity.getIcon(item.itemId)
        return super.onContextItemSelected(item)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}