package org.kaorun.nouto.ui.fragments.settings

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.navGraphViewModels
import org.kaorun.nouto.R
import org.kaorun.nouto.databinding.FragmentNoteExportBinding
import org.kaorun.nouto.ui.adapter.NoteExportAdapter
import org.kaorun.nouto.ui.components.Snackbars
import org.kaorun.nouto.ui.fragments.base.BaseFragment
import org.kaorun.nouto.ui.utils.FileUtils
import org.kaorun.nouto.ui.utils.InsetsHandler
import org.kaorun.nouto.ui.utils.MarginItemDecoration
import org.kaorun.nouto.viewmodel.NotesViewModel

class SettingsNoteExportFragment: BaseFragment(R.layout.fragment_note_export) {
    private var _binding: FragmentNoteExportBinding? = null
    private lateinit var noteExportAdapter: NoteExportAdapter
    private val viewModel: NotesViewModel by navGraphViewModels(R.id.nav_graph)
    private val binding get() = _binding!!

    private var title: String? = null
    private var content: String? = null

    private val filePicker = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            FileUtils.writeNoteToFile(
                context = requireContext(),
                uri = it,
                title = title,
                content = content
            ).onSuccess {
                setupExportSnackbar(null)
            }.onFailure { e ->
                setupExportSnackbar(e.localizedMessage)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteExportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerView()
        observeNotes()
        setupInsets()
    }

    private fun observeNotes() {
        viewModel.displayedNotes.observe(viewLifecycleOwner) { notes ->
            noteExportAdapter.submitList(notes.toList()) {
                binding.recyclerView.post {
                    binding.recyclerView.invalidateItemDecorations()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        noteExportAdapter = NoteExportAdapter(
            onItemClick = { note ->
                title = note.title
                content = note.content
                val defaultFileName = if (!title.isNullOrBlank()) {
                    "${title!!.take(20)}.json"
                } else {
                    "${content!!.take(20)}.json"
                }
                filePicker.launch(defaultFileName)
            },
        )
        binding.recyclerView.adapter = noteExportAdapter
        repeat(binding.recyclerView.itemDecorationCount) {
            binding.recyclerView.removeItemDecorationAt(0)
        }

        binding.recyclerView.addItemDecoration(
            MarginItemDecoration(
                outerSpaceSize = resources.getDimensionPixelSize(
                    R.dimen.recycler_view_outer_margin
                ),
                innerSpaceSize = resources.getDimensionPixelSize(
                    R.dimen.recycler_view_inner_margin
                ),
                spanCount = 1
            )
        )

    }

    private fun setupInsets() {
        InsetsHandler.applyViewInsets(
            binding.appBarLayout,
            isTopPaddingEnabled = true,
            isBottomPaddingEnabled = false
        )
        InsetsHandler.applyViewInsets(binding.recyclerView, false)
    }

    private fun setupExportSnackbar(e: String? = null) {
        val message = e?.let {
            "${getString(R.string.error_occured)}: $e"
        } ?: run {
            getString(R.string.export_success)
        }
        Snackbars.showSnackbarNoAction(
            view = binding.root,
            anchorView = null,
            message = message
        )
    }
}