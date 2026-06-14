package org.kaorun.nouto.ui.fragments

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.transition.TransitionManager
import com.google.android.material.navigation.NavigationView
import com.google.android.material.sidesheet.SideSheetDialog
import com.google.android.material.snackbar.Snackbar
import org.kaorun.nouto.R
import org.kaorun.nouto.data.Note
import org.kaorun.nouto.databinding.FragmentMainBinding
import org.kaorun.nouto.ui.adapter.NoteAdapter
import org.kaorun.nouto.ui.components.MainSearchView
import org.kaorun.nouto.ui.components.Snackbars
import org.kaorun.nouto.ui.fragments.base.BaseFragment
import org.kaorun.nouto.ui.model.LayoutMode
import org.kaorun.nouto.ui.utils.InsetsHandler
import org.kaorun.nouto.ui.utils.MarginItemDecoration
import org.kaorun.nouto.viewmodel.NotesViewModel
import org.kaorun.nouto.viewmodel.SearchViewModel

class MainFragment : BaseFragment(R.layout.fragment_main) {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var noteAdapter: NoteAdapter
    private val viewModel: NotesViewModel by navGraphViewModels(R.id.nav_graph)
    private val searchViewModel: SearchViewModel by viewModels()
    private var layoutMode = LayoutMode.LINEAR

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        showRestoreSnackbar()
        observeNotes()
        setupRecyclerView()
        setupLayoutMode()
        setupSearchView()
        setupListeners()
        setupInsets()
    }

    private fun showRestoreSnackbar() {
        val intent = requireActivity().intent
        if (intent.getBooleanExtra("restore_success", false)) {
            Snackbar
                .make(
                    binding.root,
                    getString(R.string.restore_database_success),
                    Snackbar.LENGTH_SHORT
                )
                .setAnchorView(binding.fab)
                .show()
            intent.removeExtra("restore_success")
        }
    }

    private fun observeNotes() {
        viewModel.displayedNotes.observe(viewLifecycleOwner) { notes ->
            val newNote = notes.firstOrNull { it !in noteAdapter.currentList }
            noteAdapter.submitList(notes.toList()) {
                newNote?.let {
                    binding.recyclerView.smoothScrollToPosition(notes.indexOf(it))
                }
                binding.recyclerView.post {
                    binding.recyclerView.invalidateItemDecorations()
                }
            }
            val isSearching = !viewModel.searchQuery.value.isNullOrBlank()
            binding.recyclerView.isInvisible = notes.isEmpty() && isSearching
            binding.notesEmptyLayout.root.isVisible = notes.isEmpty() && !isSearching
            binding.nothingFoundLayout.root.isVisible = notes.isEmpty() && isSearching
        }

        viewModel.pendingDelete.observe(viewLifecycleOwner) { note ->
            note ?: return@observe
            setupSnackbar(note)
            viewModel.clearPendingDelete()
        }
    }

    private fun setupRecyclerView() {
        noteAdapter = NoteAdapter(
            onItemClick = { note -> openNoteFragment(note.id) },
            onDeleteClick = { note ->
                setupSnackbar(note)
            }
        )
        binding.recyclerView.adapter = noteAdapter
    }

    private fun setupListeners() {
        setupNavigationButton()

        binding.fab.setOnClickListener {
            openNoteFragment(null)
        }

        binding.layoutButton.setOnClickListener {
            switchLayout()
        }
    }

    private fun setupSideSheet() {
        val sideSheetDialog = SideSheetDialog(requireContext())
        with(sideSheetDialog) {
            setContentView(R.layout.side_sheet)
            setFitsSystemWindows(false)
            show()
            setSheetEdge(Gravity.START)
        }

        sideSheetDialog.window?.let { window ->
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                window.navigationBarColor = Color.TRANSPARENT
                window.statusBarColor = Color.TRANSPARENT
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val activityAppearance = requireActivity().window.insetsController
                    ?.systemBarsAppearance ?: 0
                window.insetsController?.setSystemBarsAppearance(
                    activityAppearance,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            } else {
                val isLightMode =
                    AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_NO
                WindowInsetsControllerCompat(
                    window,
                    window.decorView
                ).isAppearanceLightStatusBars = isLightMode
            }
        }

        val navigationView =
            sideSheetDialog.findViewById<NavigationView>(R.id.navigation_view)!!
        InsetsHandler.applyViewInsets(navigationView)

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.trash -> {
                    sideSheetDialog.dismiss()
                    openTrashFragment()
                }
                R.id.settings -> {
                    sideSheetDialog.dismiss()
                    openSettingsFragment()
                }
                else -> sideSheetDialog.dismiss()
            }
            true
        }
    }

    private fun setupInsets() {
        val fabMargin = resources.getDimensionPixelSize(R.dimen.fab_margin)
        InsetsHandler.applyViewInsets(
            binding.appBarLayout,
            isTopPaddingEnabled = true,
            isBottomPaddingEnabled = false
        )
        InsetsHandler.applyViewInsets(binding.recyclerView, false)
        InsetsHandler.applyViewInsets(
            binding.searchRecyclerView,
            isTopPaddingEnabled = false,
            isBottomPaddingEnabled = true
        )
        InsetsHandler.applyViewInsets(binding.fab, fabMargin)
    }

    private fun setupNavigationButton() {
        viewModel.searchQuery.observe(viewLifecycleOwner) { query ->
            if (query.isNullOrBlank()) {
                binding.navigationButton.setIconResource(R.drawable.menu_24px)
                binding.navigationButton.setOnClickListener { setupSideSheet() }
            } else {
                binding.navigationButton.setIconResource(R.drawable.arrow_back_24px)
                binding.navigationButton.setOnClickListener {
                    viewModel.setSearchQuery(null)
                    binding.searchBar.clearText()
                }
            }
        }
    }

    private fun setupSearchView() {
        val searchResetCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                viewModel.setSearchQuery(null)
                binding.searchBar.clearText()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            searchResetCallback
        )

        viewModel.searchQuery.observe(viewLifecycleOwner) { query ->
            searchResetCallback.isEnabled = !query.isNullOrBlank()
        }
        MainSearchView(
            binding.searchView,
            binding.searchBar,
            binding.fab,
            binding.searchRecyclerView,
            binding.searchSuggestionsEmptyLayout.root,
            viewModel,
            searchViewModel,
            resources,
            viewLifecycleOwner,
            requireActivity()
        )
    }

    private fun setupSnackbar(note: Note) {
       viewModel.markDeleted(note)
        Snackbars.showSnackbarWithUndo(
            view = binding.root,
            anchorView = binding.fab,
            message = getString(R.string.note_deleted_message),
            undoAction = {
                viewModel.unmarkDeleted(note)
            }
        )
    }

    private fun openNoteFragment(noteId: Int?) {
        TransitionManager.endTransitions(binding.root.parent as ViewGroup)
        findNavController().navigate(
            MainFragmentDirections.actionMainFragmentToNoteFragment(noteId ?: -1)
        )
    }

    private fun openTrashFragment() {
        TransitionManager.endTransitions(binding.root.parent as ViewGroup)
        findNavController().navigate(
            MainFragmentDirections.actionMainFragmentToTrashFragment()
        )
    }

    private fun openSettingsFragment() {
        TransitionManager.endTransitions(binding.root.parent as ViewGroup)
        findNavController().navigate(
            MainFragmentDirections.actionMainFragmentToSettingsMainFragment()
        )
    }

    private fun setupLayoutMode() {
        viewModel.isGridMode.observe(viewLifecycleOwner) { isGrid ->
            layoutMode = if (isGrid) LayoutMode.GRID else LayoutMode.LINEAR
            applyLayoutMode()
        }
    }

    private fun applyLayoutMode() {
        binding.layoutButton.setIconResource(
            if (layoutMode == LayoutMode.GRID) R.drawable.view_agenda_24px
            else R.drawable.grid_view_24px
        )

        binding.recyclerView.layoutManager =
            StaggeredGridLayoutManager(layoutMode.spanCount, RecyclerView.VERTICAL)

        repeat(binding.recyclerView.itemDecorationCount) {
            binding.recyclerView.removeItemDecorationAt(0)
        }

        binding.recyclerView.addItemDecoration(
            MarginItemDecoration(
                resources.getDimensionPixelSize(
                    R.dimen.recycler_view_outer_margin
                ),
                resources.getDimensionPixelSize(
                    R.dimen.recycler_view_inner_margin
                ),
                layoutMode.spanCount
            )
        )

        binding.recyclerView.clipChildren = layoutMode == LayoutMode.GRID
    }

    private fun switchLayout() {
        val newMode = layoutMode != LayoutMode.GRID
        viewModel.setGridMode(newMode)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}