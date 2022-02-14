package com.z299studio.pbfree

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.z299studio.pbfree.adapters.MainListAdapter
import com.z299studio.pbfree.data.AccountRepository
import com.z299studio.pbfree.databinding.FragmentHomeBinding
import com.z299studio.pbfree.viewmodels.HomeViewModel
import com.z299studio.pbfree.viewmodels.ItemViewModel
import com.z299studio.pbfree.viewmodels.MainItemViewModel
import java.text.Collator

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var mainListAdapter: MainListAdapter
    private val homeViewModel: HomeViewModel by activityViewModels()
    private val selectedItemViewModel: ItemViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val mainItems = getAllEntriesForDisplay(homeViewModel.category.value)
        if (mainItems.isNotEmpty()) {
            mainItems[mainItems.size - 1].lastRow = true
        }
        binding.homeView = homeViewModel
        mainListAdapter = MainListAdapter(mainItems, selectedItemViewModel)
        binding.drawer.setOnClickListener {
            (requireActivity() as MainActivity).openDrawer()
        }
        binding.mainList.layoutManager = LinearLayoutManager(context)
        binding.mainList.adapter = ConcatAdapter(HeaderAdapter(inflater), mainListAdapter)
        binding.mainList.addOnLayoutChangeListener { v, _, _, _, bottom, _, _, _, oldBottom ->
            if (bottom != oldBottom)
            {
                val params = binding.searchBar.layoutParams as AppBarLayout.LayoutParams
                params.scrollFlags = if (v.canScrollVertically(1) || v.canScrollVertically(-1)) {
                    AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                } else AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                binding.searchBar.layoutParams = params
            }
        }
        homeViewModel.searchText.observe(viewLifecycleOwner) {
            mainListAdapter.setItems(getAllEntriesForDisplay(homeViewModel.category.value, it))
        }
        setCategoryChangeObserver()
        homeViewModel.empty.value = mainListAdapter.itemCount == 0
        binding.lifecycleOwner = viewLifecycleOwner
        (requireActivity() as MainActivity).startSync()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).checkSelectedDrawer()
    }

    private fun setCategoryChangeObserver() {
        homeViewModel.category.observe(viewLifecycleOwner) {
            mainListAdapter.setItems(getAllEntriesForDisplay(it))
            homeViewModel.empty.value = mainListAdapter.itemCount == 0
        }
    }

    private fun getAllEntriesForDisplay(categoryFilter: String?,
                                        query: String? = null): List<MainItemViewModel> {
        return AccountRepository.get().getAllEntries().mapIndexed { index, entry ->
            if (categoryFilter.isNullOrEmpty() || entry.category == categoryFilter) {
                if (entry.matches(query)) {
                    MainItemViewModel(index, entry)
                } else {
                    null
                }
            } else {
                null
            }
        }.filterNotNull().sortedWith { i1, i2 -> Collator.getInstance().compare(i1.title, i2.title) }
    }

    class HeaderAdapter(private val inflater: LayoutInflater) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            object : RecyclerView.ViewHolder(inflater.inflate(R.layout.placeholder, parent, false)) {}

        override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {}

        override fun getItemCount() = 1
    }
}