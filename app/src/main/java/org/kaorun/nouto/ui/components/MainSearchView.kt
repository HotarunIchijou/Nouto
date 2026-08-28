package org.kaorun.nouto.ui.components

import android.content.res.Resources
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import org.kaorun.nouto.R
import org.kaorun.nouto.ui.adapter.SearchAdapter
import org.kaorun.nouto.ui.model.LayoutMode
import org.kaorun.nouto.ui.utils.MarginItemDecoration
import org.kaorun.nouto.viewmodel.NotesViewModel
import org.kaorun.nouto.viewmodel.SearchViewModel

class MainSearchView(
    private val searchView: SearchView,
    private val searchBar: SearchBar,
    private val fab: View,
    private val searchRecyclerView: RecyclerView,
    private val searchSuggestionsEmptyLayout: View,
    private val viewModel: NotesViewModel,
    private val searchViewModel: SearchViewModel,
    private val resources: Resources,
    private val lifecycleOwner: LifecycleOwner,
    private val activity: FragmentActivity,
) {
    private lateinit var searchAdapter: SearchAdapter
    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            searchView.hide()
        }
    }

    init {
        searchView.setupWithSearchBar(searchBar)
        observeViewModel(lifecycleOwner)
        setupSearchView()
        setupRecyclerView()
    }

    private fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        searchViewModel.searchHistory.observe(lifecycleOwner) { history ->
            searchSuggestionsEmptyLayout.isVisible = history.isNullOrEmpty()
            searchAdapter.submitList(history)
        }
    }

    private fun setupSearchView() {
        activity.onBackPressedDispatcher.addCallback(lifecycleOwner, backCallback)
        searchView.addTransitionListener { _, _, newState ->
            when (newState) {
                SearchView.TransitionState.SHOWING -> {
                    backCallback.isEnabled = true
                    fab.isVisible = false
                }
                SearchView.TransitionState.HIDING -> {
                    backCallback.isEnabled = false
                    fab.isVisible = true
                }
                else -> Unit
            }
        }

        searchView.editText.setOnEditorActionListener { textView, _, _ ->
            val query = textView.text.toString().trim()
            submitSearchQuery(query)
            searchViewModel.addToHistory(query)
            true
        }
    }

    private fun setupRecyclerView() {
        searchAdapter = SearchAdapter(
            onItemClick = { query ->
                submitSearchQuery(query)
            },
            onDeleteClick = { query ->
                searchViewModel.removeFromHistory(query)
            }
        )
        searchRecyclerView.apply {
            adapter = searchAdapter
            layoutManager = LinearLayoutManager(activity)
        }

        searchRecyclerView.addItemDecoration(
            MarginItemDecoration(
                resources.getDimensionPixelSize(
                    R.dimen.recycler_view_outer_margin
                ),
                resources.getDimensionPixelSize(
                    R.dimen.recycler_view_segmented_list_margin
                ),
                LayoutMode.LINEAR.spanCount
            )
        )
    }

    private fun submitSearchQuery(
        query: String,
        searchBar: SearchBar = this.searchBar,
        searchView: SearchView = this.searchView
    ) {
        viewModel.setSearchQuery(query)
        searchBar.setText(query)
        searchView.setText(query)
        searchView.hide()
    }
}