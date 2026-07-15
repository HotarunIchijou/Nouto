package org.kaorun.nouto.ui.fragments

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import androidx.preference.PreferenceManager
import androidx.transition.TransitionManager
import com.google.android.material.transition.MaterialFade
import com.onegravity.rteditor.RTManager
import com.onegravity.rteditor.api.RTApi
import com.onegravity.rteditor.api.RTMediaFactoryImpl
import com.onegravity.rteditor.api.RTProxyImpl
import com.onegravity.rteditor.api.format.RTFormat
import org.kaorun.nouto.R
import org.kaorun.nouto.data.Note
import org.kaorun.nouto.databinding.FragmentNoteBinding
import org.kaorun.nouto.ui.components.DeleteDialog
import org.kaorun.nouto.ui.components.Snackbars
import org.kaorun.nouto.ui.components.TextStyleFloatingToolbar
import org.kaorun.nouto.ui.fragments.base.BaseFragment
import org.kaorun.nouto.ui.utils.FileUtils
import org.kaorun.nouto.ui.utils.InsetsHandler
import org.kaorun.nouto.viewmodel.NotesViewModel

class NoteFragment : BaseFragment(R.layout.fragment_note) {
    private lateinit var rtManager: RTManager
    private var _binding: FragmentNoteBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NotesViewModel by navGraphViewModels(R.id.nav_graph)
    private val args: NoteFragmentArgs by navArgs()
    private val filePicker = setupFilePicker()
    private var note: Note? = null
    private var isDeleting = false
    private var isSaved = false
    private var isRestoring = false
    private var isPinned = false
    private var isImporting = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRtEditor(savedInstanceState)
        setupNote()
        setupInsets()
        setupToolbar()
        setupListeners()
    }

    private fun setupRtEditor(savedInstanceState: Bundle?) {
        val rtApi = RTApi(
            requireContext(),
            RTProxyImpl(requireActivity()),
            RTMediaFactoryImpl(requireContext(), true)
        )

        rtManager = RTManager(rtApi, savedInstanceState)
        rtManager.registerEditor(binding.noteTitle, true)
        rtManager.registerEditor(binding.noteContent, true)
    }

    private fun setupNote() {
        val noteId = args.noteId

        if (noteId != -1) {
            val isInViewOnlyMode = PreferenceManager
                .getDefaultSharedPreferences(requireContext())
                .getBoolean("is_view_only_mode", false)

            viewModel.getNote(noteId).observe(viewLifecycleOwner) { existingNote ->
                existingNote?.let {
                    note = it
                    binding.noteTitle.setRichTextEditing(true, it.title)
                    binding.noteContent.setRichTextEditing(true, it.content)
                    binding.noteTitle.setSelection(binding.noteTitle.text?.length ?: 0)
                    isPinned = it.isPinned
                    updateUIState(note, !isRestoring, isInViewOnlyMode)
                }
            }
        } else {
            if (args.title != null || args.content != null) {
                isImporting = true
                binding.noteTitle.setRichTextEditing(true, args.title)
                binding.noteContent.setRichTextEditing(true, args.content)
                updateUIState(null, false)
            } else {
                binding.noteTitle.setRichTextEditing(true, null)
                binding.noteContent.setRichTextEditing(true, null)
                updateUIState(null, true)
            }
        }
    }

    private fun setupToolbar() {
        val textStyleFloatingToolbar = TextStyleFloatingToolbar(
            rtManager,
            binding.styleBold,
            binding.styleItalic,
            binding.styleUnderline,
            listOf(binding.noteTitle, binding.noteContent)
        )
        textStyleFloatingToolbar.setupFloatingToolbar()
    }

    private fun setupListeners() {
        setupChangeListener()

        binding.scrollView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            with(binding.buttonEditNote) {
                if (isExtended && scrollY > oldScrollY) shrink()
                if (!isExtended && scrollY < oldScrollY) extend()
                if (scrollY == 0) extend()
            }
        }

        binding.buttonEditNote.setOnClickListener {
            updateUIState(note, isShowKeyboard = true, isInViewOnlyMode = false)
        }

        binding.buttonSave.setOnClickListener {
            closeNoteFragment()
        }

        binding.topAppBar.setNavigationOnClickListener {
            closeNoteFragment()
        }

        binding.buttonDeletePermanently.setOnClickListener {
            DeleteDialog.show(requireContext(), resources) {
                isDeleting = true
                viewModel.deleteNote(note!!)
                closeNoteFragment()
            }
        }

        binding.buttonRestore.setOnClickListener {
            isRestoring = true
            binding.floatingToolbar.isVisible = true
            viewModel.unmarkDeleted(note!!)
            setupRestoreSnackbar(note!!)
        }

        binding.buttonBack.setOnClickListener {
            closeNoteFragment()
        }

        binding.buttonImport.setOnClickListener {
            isImporting = false
            setupImportSnackbar()
            closeNoteFragment()
        }

        binding.buttonMenu.setOnClickListener { view ->
            val wrapper = ContextThemeWrapper(requireContext(), R.style.ThemeOverlay_Popup)
            val popup = PopupMenu(wrapper, view)
            popup.menuInflater.inflate(R.menu.appbar, popup.menu)

            val pinMenuItem = popup.menu.findItem(R.id.pin)
            if (isPinned) {
                pinMenuItem.title = getString(R.string.unpin)
                pinMenuItem.setIcon(R.drawable.keep_off_24px)
            } else {
                pinMenuItem.title = getString(R.string.pin)
                pinMenuItem.setIcon(R.drawable.keep_24px)
            }

            popup.setForceShowIcon(true)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.pin -> {
                        setupPinSnackbar()
                        isPinned = !isPinned
                        true
                    }
                    R.id.export -> {
                        exportNote()
                        true
                    }
                    R.id.delete -> {
                        note?.let {
                            isDeleting = true
                            viewModel.setPendingDelete(it)
                            closeNoteFragment()
                        } ?: run {
                            DeleteDialog.show(requireContext(), resources) {
                                isDeleting = true
                                closeNoteFragment()
                            }
                        }
                        true
                    }
                    else -> false
                }
            }

            popup.show()
        }
    }

    private fun setupRestoreSnackbar(note: Note) {
        Snackbars.showSnackbarWithUndo(
            view = binding.root,
            anchorView = binding.floatingToolbar,
            message = getString(R.string.note_restored_message),
            undoAction = {
                viewModel.markDeleted(note)
            }
        )
    }

    private fun setupPinSnackbar() {
        val messageRes = if (isPinned) {
            R.string.note_unpinned_message
        } else R.string.note_pinned_message

        Snackbars.showSnackbarWithUndo(
            view = binding.root,
            anchorView = binding.floatingToolbar,
            message = getString(messageRes),
            undoAction = {
                isPinned = !isPinned
            }
        )
    }

    private fun setupExportSnackbar(e: String? = null) {
        val message = e?.let {
            "${getString(R.string.error_occured)}: $e"
        } ?: run {
            getString(R.string.export_success)
        }
        Snackbars.showSnackbarNoAction(
            view = binding.root,
            anchorView = binding.floatingToolbar,
            message = message
        )
    }

    private fun setupImportSnackbar() {
        val activityView = requireActivity().findViewById<View>(android.R.id.content)
        Snackbars.showSnackbarNoAction(
            view = activityView,
            anchorView = null,
            message = getString(R.string.note_imported_message)
        )
    }

    private fun setupChangeListener() {
        val isChanged = {
            (binding.noteTitle.getText(RTFormat.HTML).orEmpty() != (note?.title ?: "") &&
            binding.noteTitle.getText(RTFormat.HTML).toPlainText().isNotEmpty()) ||
            (binding.noteContent.getText(RTFormat.HTML).orEmpty() != (note?.content ?: "") &&
            binding.noteContent.getText(RTFormat.HTML).toPlainText().isNotEmpty())
        }

        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val isChanged = {
                    val title = binding.noteTitle.getText(RTFormat.HTML)
                    val content = binding.noteContent.getText(RTFormat.HTML)
                    (title != (note?.title ?: "") && title.toPlainText().isNotEmpty()) ||
                    (content != (note?.content ?: "") && content.toPlainText().isNotEmpty())
                }
                setButtonVisible(isChanged())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        listOf(binding.styleBold, binding.styleItalic, binding.styleUnderline).forEach {
            it.addOnCheckedChangeListener {  _, _ ->
                binding.root.post {
                    setButtonVisible(isChanged())
                }
            }
        }

        listOf(binding.noteTitle, binding.noteContent).forEach {
            it.addTextChangedListener(watcher)
        }
    }

    private fun setButtonVisible(isChanged: Boolean) {
        val fade = MaterialFade().apply {
            duration = if (isChanged) 150L else 84L
        }
        TransitionManager.beginDelayedTransition(binding.root, fade)
        binding.buttonSave.visibility = if (isChanged) View.VISIBLE else View.INVISIBLE
    }

    private fun setupInsets() {
        InsetsHandler.applyViewInsets(
            view = binding.appBarLayout,
            isTopPaddingEnabled = true,
            isBottomPaddingEnabled = false,
        )
        InsetsHandler.applyViewInsets(
            view = binding.contentContainer,
            isTopPaddingEnabled = false,
            isBottomPaddingEnabled = true
        )
        InsetsHandler.applyViewInsets(
            view = binding.noteContent,
            isTopPaddingEnabled = false,
            isBottomPaddingEnabled = true,
            isHorizontalPaddingEnabled = false,
            additionalPadding = resources.getDimensionPixelSize(R.dimen.note_container_bottom_margin)
        )
        InsetsHandler.applyViewInsets(
            view = binding.buttonGroupImport,
            isTopPaddingEnabled = true,
            isBottomPaddingEnabled = true,
            isHorizontalPaddingEnabled = true,
            isAdditionalTopPaddingEnabled = true,
            isAdditionalHorizontalPaddingEnabled = true,
            isTopSystemPaddingEnabled = false,
            additionalPadding = resources.getDimensionPixelSize(R.dimen.button_group_import_padding)
        )
        InsetsHandler.applyViewInsets(
            binding.buttonEditNote,
            resources.getDimensionPixelSize(R.dimen.fab_margin)
        )
        InsetsHandler.applyImeInsets(binding.buttonEditNote)
        InsetsHandler.applyImeInsets(binding.scrollView)

//        (enterTransition as? Transition)?.addListener(
//            object : Transition.TransitionListener {
//                override fun onTransitionEnd(transition: Transition) {
//                    InsetsHandler.applyImeInsets(binding.floatingToolbar)
//                }
//                override fun onTransitionStart(transition: Transition) {}
//                override fun onTransitionCancel(transition: Transition) {
//                    InsetsHandler.applyImeInsets(binding.floatingToolbar)
//                }
//                override fun onTransitionPause(transition: Transition) {}
//                override fun onTransitionResume(transition: Transition) {}
//            }
//        ) ?: InsetsHandler.applyImeInsets(binding.floatingToolbar)

        InsetsHandler.applyImeInsets(binding.floatingToolbar)
    }

    private fun updateUIState(
        note: Note?,
        isShowKeyboard: Boolean,
        isInViewOnlyMode: Boolean = false
    ) {
        val isDeleted = !isDeleting && (note?.isDeleted ?: false)
        val isEditable = !isInViewOnlyMode && !isImporting && !isDeleted

        listOf(binding.noteTitle, binding.noteContent).forEach {
            it.isEnabled = !isDeleted
            it.isFocusable = isEditable
            it.isFocusableInTouchMode = isEditable
            it.isClickable = isEditable
            it.isCursorVisible = isEditable
        }

        binding.buttonEditNote.isVisible = !isDeleted && !isImporting
        binding.buttonMenu.isVisible = !isImporting && !isDeleted
        binding.floatingToolbar.isVisible = isEditable
        binding.buttonGroup.isVisible = isDeleted && !isImporting
        binding.buttonGroupMenu.isVisible = !isDeleted && !isImporting

        if (isEditable) {
            binding.buttonEditNote.shrink()
            binding.buttonEditNote.hide()
        }

        val isShowKeyboardPreference = PreferenceManager
            .getDefaultSharedPreferences(requireContext())
            .getBoolean("is_show_keyboard", true)

        if (isEditable && isShowKeyboard && isShowKeyboardPreference) {
            showKeyboard()
        }

        if (isImporting) {
            if (args.title.isNullOrBlank()) {
                binding.noteTitle.hint = getString(R.string.empty_title)
            }
            if (args.content.isNullOrBlank()) {
                binding.noteContent.hint = getString(R.string.empty_content)
            }
            binding.buttonGroupImport.isVisible = true
            binding.topAppBar.navigationIcon = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.close_24px
            )
        } else {
            binding.buttonGroupImport.isVisible = false
            binding.topAppBar.navigationIcon = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.arrow_back_24px
            )
        }
    }
    private fun saveNote() {
        val htmlTitle = binding.noteTitle.getText(RTFormat.HTML)
        val htmlContent = binding.noteContent.getText(RTFormat.HTML)
        val title = if (htmlTitle.toPlainText().isBlank()) "" else htmlTitle.trimHtml()
        val content = if (htmlContent.toPlainText().isBlank()) "" else htmlContent.trimHtml()
        val time = System.currentTimeMillis()


        note?.let {
            if (note!!.title != htmlTitle || note!!.content != htmlContent || it.isPinned != isPinned) {
                viewModel.updateNote(
                    note!!.copy(title = title, content = content, time = time, isPinned = isPinned)
                )
            }
        } ?: run {
            if (htmlTitle.toPlainText().isNotBlank() || htmlContent.toPlainText().isNotBlank()) {
                viewModel.addNote(title = title, content = content, time = time, isPinned = isPinned)
            }
        }
    }

    private fun showKeyboard() {
        binding.noteTitle.requestFocus()
        WindowCompat.getInsetsController(requireActivity().window, binding.noteTitle)
            .show(WindowInsetsCompat.Type.ime())
    }

    private fun setupFilePicker(): ActivityResultLauncher<String?> = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            saveNote()
            isSaved = true

            val title = binding.noteTitle.getText(RTFormat.HTML).orEmpty()
            val content = binding.noteContent.getText(RTFormat.HTML).orEmpty()

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

    private fun exportNote() {
        val title = binding.noteTitle.getText(RTFormat.HTML).toPlainText()
        val content = binding.noteContent.getText(RTFormat.HTML).toPlainText()
        val defaultFileName = if (title.isNotBlank()) {
            "${title.take(20)}.json"
        } else {
            "${content.take(20)}.json"
        }

        filePicker.launch(defaultFileName)
    }

    private fun closeNoteFragment() {
        TransitionManager.endTransitions(binding.root.parent as ViewGroup)
        findNavController().popBackStack()
    }

    private fun String.trimHtml() = replace(Regex("(<br\\s*/?>|\\s)+$"), "").trim()

    private fun String.toPlainText() = Html.fromHtml(this, Html.FROM_HTML_MODE_COMPACT).toString().trim()

    override fun onStop() {
        super.onStop()
        if (!isDeleting && !isImporting && !isSaved) {
            saveNote()
            isSaved = true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        rtManager.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rtManager.onDestroy(requireActivity().isFinishing)
        _binding = null
    }
}