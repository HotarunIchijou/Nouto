package org.kaorun.nouto.ui.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.transition.TransitionManager
import com.google.android.material.navigation.NavigationView
import com.google.android.material.sidesheet.SideSheetDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.kaorun.nouto.R
import org.kaorun.nouto.data.Folder
import org.kaorun.nouto.data.HomeItem
import org.kaorun.nouto.data.LibraryResource
import org.kaorun.nouto.data.Note
import org.kaorun.nouto.databinding.FragmentMainBinding
import org.kaorun.nouto.ui.adapter.HomeAdapter
import org.kaorun.nouto.ui.components.FabMenuController
import org.kaorun.nouto.ui.components.ItemOptionsBottomSheet
import org.kaorun.nouto.ui.components.LibraryDialogs
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
    private lateinit var homeAdapter: HomeAdapter
    private val viewModel: NotesViewModel by navGraphViewModels(R.id.nav_graph)
    private val searchViewModel: SearchViewModel by viewModels()

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            viewModel.addFiles(uris, null, requireContext()) {
                val count = uris.size
                val msg = if (count == 1) {
                    getString(R.string.file_imported_message)
                } else {
                    getString(R.string.files_imported_message, count)
                }
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.composeFabView)
                    .show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.viewTreeObserver.addOnPreDrawListener(object : android.view.ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                val hasData = viewModel.displayedHomeItems.value != null
                if (hasData) {
                    view.viewTreeObserver.removeOnPreDrawListener(this)
                }
                return hasData
            }
        })

        showRestoreSnackbar()
        observeHomeItems()
        setupRecyclerView()
        setupFabMenu()
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
                .setAnchorView(binding.composeFabView)
                .show()
            intent.removeExtra("restore_success")
        }
    }

    private fun observeHomeItems() {
        viewModel.displayedHomeItems.observe(viewLifecycleOwner) { items ->
            homeAdapter.submitList(items.toList()) {
                binding.recyclerView.post {
                    binding.recyclerView.invalidateItemDecorations()
                }
            }
            val isSearching = !viewModel.searchQuery.value.isNullOrBlank()
            binding.recyclerView.isInvisible = items.isEmpty() && isSearching
            binding.notesEmptyLayout.root.isVisible = items.isEmpty() && !isSearching
            binding.nothingFoundLayout.root.isVisible = items.isEmpty() && isSearching
        }

        viewModel.pendingDelete.observe(viewLifecycleOwner) { note ->
            note ?: return@observe
            setupSnackbar(note)
            viewModel.clearPendingDelete()
        }
    }

    private fun setupRecyclerView() {
        homeAdapter = HomeAdapter(
            onItemClick = { item -> handleItemClick(item) },
            onItemLongClick = { item -> handleItemLongClick(item) },
            onDeleteClick = { item -> handleDeleteItem(item) }
        )
        binding.recyclerView.adapter = homeAdapter
    }

    private val fabExpandedState = androidx.compose.runtime.mutableStateOf(false)

    private fun setupFabMenu() {
        binding.composeFabView.setContent {
            val expanded by fabExpandedState
            org.kaorun.nouto.ui.components.NoutoFabMenu(
                isExpanded = expanded,
                onExpandedChange = { fabExpandedState.value = it },
                onCreateNote = { openNoteFragment(null) },
                onCreateFolder = { showCreateFolderDialog() },
                onImportFiles = { filePickerLauncher.launch(arrayOf("*/*")) },
                onAddLink = { showAddLinkDialog() }
            )
        }
    }

    private fun showCreateFolderDialog() {
        LibraryDialogs.showCreateFolderDialog(requireContext(), resources) { folderName ->
            viewModel.createFolder(folderName, null)
        }
    }

    private fun showAddLinkDialog() {
        LibraryDialogs.showAddLinkDialog(requireContext(), resources) { url, name ->
            viewModel.addLink(url, name, null)
        }
    }

    private fun handleItemClick(item: HomeItem) {
        when (item) {
            is HomeItem.FolderItem -> {
                openFolderFragment(item.folder.id, item.folder.name)
            }
            is HomeItem.NoteItem -> {
                openNoteFragment(item.note.id)
            }
            is HomeItem.ResourceItem -> {
                openResource(item.resource)
            }
        }
    }

    private fun handleItemLongClick(item: HomeItem) {
        val bottomSheet = ItemOptionsBottomSheet.newInstance(item)
        bottomSheet.onPinToggle = { note ->
            viewModel.togglePinNote(note)
        }
        bottomSheet.onRename = { targetItem ->
            handleRenameItem(targetItem)
        }
        bottomSheet.onMove = { targetItem ->
            handleMoveItem(targetItem)
        }
        bottomSheet.onDelete = { targetItem ->
            handleDeleteItem(targetItem)
        }
        bottomSheet.show(childFragmentManager, "ItemOptionsBottomSheet")
    }

    private fun handleRenameItem(item: HomeItem) {
        when (item) {
            is HomeItem.FolderItem -> {
                LibraryDialogs.showRenameDialog(
                    requireContext(),
                    resources,
                    currentName = item.folder.name,
                    isFolder = true
                ) { newName ->
                    viewModel.renameFolder(item.folder, newName)
                }
            }
            is HomeItem.ResourceItem -> {
                LibraryDialogs.showRenameDialog(
                    requireContext(),
                    resources,
                    currentName = item.resource.displayName,
                    isFolder = false
                ) { newName ->
                    viewModel.renameResource(item.resource, newName)
                }
            }
            is HomeItem.NoteItem -> {}
        }
    }

    private fun handleMoveItem(item: HomeItem) {
        viewLifecycleOwner.lifecycleScope.launch {
            val allFolders = viewModel.getAllFolders()
            when (item) {
                is HomeItem.FolderItem -> {
                    LibraryDialogs.showMoveDialog(
                        requireContext(),
                        resources,
                        allFolders = allFolders,
                        currentFolderId = item.folder.parentFolderId,
                        excludeFolderId = item.folder.id
                    ) { targetFolderId ->
                        viewModel.moveFolder(item.folder, targetFolderId)
                    }
                }
                is HomeItem.NoteItem -> {
                    LibraryDialogs.showMoveDialog(
                        requireContext(),
                        resources,
                        allFolders = allFolders,
                        currentFolderId = item.note.folderId
                    ) { targetFolderId ->
                        viewModel.moveNote(item.note, targetFolderId)
                    }
                }
                is HomeItem.ResourceItem -> {
                    LibraryDialogs.showMoveDialog(
                        requireContext(),
                        resources,
                        allFolders = allFolders,
                        currentFolderId = item.resource.folderId
                    ) { targetFolderId ->
                        viewModel.moveResource(item.resource, targetFolderId)
                    }
                }
            }
        }
    }

    private fun handleDeleteItem(item: HomeItem) {
        when (item) {
            is HomeItem.FolderItem -> {
                viewModel.deleteFolder(item.folder)
                Snackbars.showSnackbarWithUndo(
                    view = binding.root,
                    anchorView = binding.composeFabView,
                    message = getString(R.string.folder_deleted_message),
                    undoAction = { viewModel.restoreFolder(item.folder) }
                )
            }
            is HomeItem.NoteItem -> {
                setupSnackbar(item.note)
            }
            is HomeItem.ResourceItem -> {
                viewModel.deleteResource(item.resource)
                Snackbars.showSnackbarWithUndo(
                    view = binding.root,
                    anchorView = binding.composeFabView,
                    message = getString(R.string.resource_deleted_message),
                    undoAction = { viewModel.restoreResource(item.resource) }
                )
            }
        }
    }

    private fun openResource(resource: LibraryResource) {
        try {
            when (resource.resourceType) {
                "LINK" -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(resource.uriString))
                    startActivity(intent)
                }
                else -> {
                    val uri = Uri.parse(resource.uriString)
                    val mimeType = resource.mimeType ?: "*/*"
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(intent)
                }
            }
        } catch (e: ActivityNotFoundException) {
            Snackbar.make(binding.root, getString(R.string.no_app_to_open_resource), Snackbar.LENGTH_SHORT)
                .setAnchorView(binding.composeFabView)
                .show()
        } catch (e: SecurityException) {
            Snackbar.make(binding.root, getString(R.string.resource_access_error), Snackbar.LENGTH_SHORT)
                .setAnchorView(binding.composeFabView)
                .show()
        } catch (e: Exception) {
            Snackbar.make(binding.root, getString(R.string.error_occured), Snackbar.LENGTH_SHORT)
                .setAnchorView(binding.composeFabView)
                .show()
        }
    }

    private fun setupListeners() {
        setupNavigationButton()
    }

    private fun setupSideSheet() {
        val navSheet = org.kaorun.nouto.ui.components.NavigationBottomSheet()
        navSheet.onTrashClick = { openTrashFragment() }
        navSheet.onSettingsClick = { openSettingsFragment() }
        navSheet.show(parentFragmentManager, "NavigationBottomSheet")
    }

    private fun setupInsets() {
        val fabMargin = resources.getDimensionPixelSize(R.dimen.fab_margin)
        InsetsHandler.applyViewInsets(
            binding.appBarLayout,
            isTopPaddingEnabled = true,
            isBottomPaddingEnabled = false
        )
        // Pad the RecyclerView so items start below the floating search bar
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerView) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            val searchBarHeight = 88 * resources.displayMetrics.density
            v.setPadding(
                systemBars.left,
                systemBars.top + searchBarHeight.toInt(),
                systemBars.right,
                systemBars.bottom
            )
            insets
        }
        InsetsHandler.applyViewInsets(
            binding.searchRecyclerView,
            isTopPaddingEnabled = false,
            isBottomPaddingEnabled = true
        )
        InsetsHandler.applyViewInsets(binding.composeFabView, fabMargin)
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
                if (fabExpandedState.value) {
                    fabExpandedState.value = false
                    return
                }
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
            binding.composeFabView,
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
            anchorView = binding.composeFabView,
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

    private fun openFolderFragment(folderId: Long, folderName: String) {
        TransitionManager.endTransitions(binding.root.parent as ViewGroup)
        findNavController().navigate(
            MainFragmentDirections.actionMainFragmentToLibraryFragment(
                folderId = folderId,
                folderName = folderName
            )
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
        binding.recyclerView.layoutManager =
            androidx.recyclerview.widget.StaggeredGridLayoutManager(1, androidx.recyclerview.widget.RecyclerView.VERTICAL)

        repeat(binding.recyclerView.itemDecorationCount) {
            binding.recyclerView.removeItemDecorationAt(0)
        }

        binding.recyclerView.addItemDecoration(
            org.kaorun.nouto.ui.utils.MarginItemDecoration(
                resources.getDimensionPixelSize(
                    R.dimen.recycler_view_outer_margin
                ),
                resources.getDimensionPixelSize(
                    R.dimen.recycler_view_inner_margin
                ),
                1
            )
        )

        binding.recyclerView.clipChildren = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}