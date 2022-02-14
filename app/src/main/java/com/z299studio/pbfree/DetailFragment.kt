package com.z299studio.pbfree

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.z299studio.pbfree.adapters.ValueListAdapter
import com.z299studio.pbfree.data.AccountRepository
import com.z299studio.pbfree.databinding.FragmentDetailBinding
import com.z299studio.pbfree.viewmodels.ItemViewModel

class DetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!
    private val itemViewModel: ItemViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        val root: View = binding.root
        itemViewModel.from(itemViewModel.index)
        binding.list.layoutManager = LinearLayoutManager(context)
        binding.list.adapter = ValueListAdapter(itemViewModel.values.value!!)
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        activity?.menuInflater?.inflate(R.menu.detail, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_delete -> {
                AlertDialog.Builder(requireContext()).setTitle(R.string.action_delete)
                    .setMessage(R.string.delete_ask)
                    .setNegativeButton(android.R.string.cancel) {_, _ -> }
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        AccountRepository.get().delete(itemViewModel.index)
                        AccountRepository.get().saveData(requireContext())
                        activity?.onBackPressed()
                    }
                    .show()
            }
            R.id.action_edit -> {
                findNavController().navigate(DetailFragmentDirections.action2Edit())
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}