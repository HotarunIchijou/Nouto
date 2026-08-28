package org.kaorun.nouto.ui.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.transition.TransitionManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.kaorun.nouto.R
import org.kaorun.nouto.data.Folder
import org.kaorun.nouto.data.HomeItem
import org.kaorun.nouto.data.LibraryResource
import org.kaorun.nouto.data.Note
import org.kaorun.nouto.databinding.FragmentLibraryBinding
import org.kaorun.nouto.ui.adapter.HomeAdapter
import org.kaorun.nouto.ui.components.FabMenuController
import org.kaorun.nouto.ui.components.ItemOptionsBottomSheet
import org.kaorun.nouto.ui.components.LibraryDialogs
import org.kaorun.nouto.ui.components.Snackbars
import org.kaorun.nouto.ui.fragments.base.BaseFragment
import org.kaorun.nouto.ui.model.LayoutMode
import org.kaorun.nouto.ui.utils.InsetsHandler
import org.kaorun.nouto.ui.utils.MarginItemDecoration
import org.kaorun.nouto.viewmodel.LibraryViewModel

class LibraryFragment : BaseFragment(R.layout.fragment_library) {
    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!
    private lateinit var homeAdapter: HomeAdapter
    private val viewModel: LibraryViewModel by viewModels()
    private val args: LibraryFragmentArgs by navArgs()

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            val folderId = if (args.folderId == -1L) null else args.folderId
            viewModel.addFiles(uris, folderId, requireContext()) {
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
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val currentFolderId = if (args.folderId == -1L) null else args.folderId
        viewModel.setCurrentFolderId(currentFolderId)

        setupToolbar()
        setupRecyclerView()
        setupFabMenu()
        setupLayoutMode()
        setupInsets()
        setupBackCallback()
        observeItems()
    }

    private fun setupToolbar() {
        val title = args.folderName ?: getString(R.string.library)
        binding.toolbar.title = title
        binding.collapsingToolbar.title = title
        binding.collapsingToolbar.apply {
            setCollapsedTitleTypeface(createTypeface())
            setExpandedTitleTypeface(createTypeface())
        }

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            false
        }
    }

    private fun observeItems() {
        viewModel.libraryItems.observe(viewLifecycleOwner) { items ->
            homeAdapter.submitList(items) {
                binding.recyclerView.invalidateItemDecorations()
            }
            binding.emptyLayout.root.isVisible = items.isEmpty()
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
        val currentFolderId = if (args.folderId == -1L) null else args.folderId
        LibraryDialogs.showCreateFolderDialog(requireContext(), resources) { folderName ->
            viewModel.createFolder(folderName, currentFolderId)
        }
    }

    private fun showAddLinkDialog() {
        val currentFolderId = if (args.folderId == -1L) null else args.folderId
        LibraryDialogs.showAddLinkDialog(requireContext(), resources) { url, name ->
            viewModel.addLink(url, name, currentFolderId)
        }
    }

    private fun setupBackCallback() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (fabExpandedState.value) {
                    fabExpandedState.value = false
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun setupLayoutMode() {
        binding.recyclerView.layoutManager =
            androidx.recyclerview.widget.StaggeredGridLayoutManager(1, androidx.recyclerview.widget.RecyclerView.VERTICAL)

        repeat(binding.recyclerView.itemDecorationCount) {
            binding.recyclerView.removeItemDecorationAt(0)
        }

        binding.recyclerView.addItemDecoration(
            org.kaorun.nouto.ui.utils.MarginItemDecoration(
                resources.getDimensionPixelSize(R.dimen.recycler_view_outer_margin),
                resources.getDimensionPixelSize(R.dimen.recycler_view_inner_margin),
                1
            )
        )

        binding.recyclerView.clipChildren = false
    }

    private fun setupInsets() {
        val fabMargin = resources.getDimensionPixelSize(R.dimen.fab_margin)
        InsetsHandler.applyViewInsets(
            binding.appBarLayout,
            isTopPaddingEnabled = true,
            isBottomPaddingEnabled = false
        )
        InsetsHandler.applyViewInsets(binding.recyclerView, false)
        InsetsHandler.applyViewInsets(binding.composeFabView, fabMargin)
    }

    private fun handleItemClick(item: HomeItem) {
        when (item) {
            is HomeItem.FolderItem -> {
                TransitionManager.endTransitions(binding.root.parent as ViewGroup)
                findNavController().navigate(
                    LibraryFragmentDirections.actionLibraryFragmentSelf(
                        folderId = item.folder.id,
                        folderName = item.folder.name
                    )
                )
            }
            is HomeItem.NoteItem -> {
                openNoteFragment(item.note.id)
            }
            is HomeItem.ResourceItem -> {
                openResource(item.resource)
            }
        }
    }

    private fun openNoteFragment(noteId: Int?) {
        TransitionManager.endTransitions(binding.root.parent as ViewGroup)
        findNavController().navigate(
            LibraryFragmentDirections.actionGlobalNoteFragment(
                noteId = noteId ?: -1
            )
        )
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
                viewModel.markDeleted(item.note)
                Snackbars.showSnackbarWithUndo(
                    view = binding.root,
                    anchorView = binding.composeFabView,
                    message = getString(R.string.note_deleted_message),
                    undoAction = {
                        viewModel.unmarkDeleted(item.note)
                    }
                )
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
