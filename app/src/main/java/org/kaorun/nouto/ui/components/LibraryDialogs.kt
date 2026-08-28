package org.kaorun.nouto.ui.components

import android.content.Context
import android.content.res.Resources
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatEditText
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.kaorun.nouto.R
import org.kaorun.nouto.data.Folder

object LibraryDialogs {

    fun showCreateFolderDialog(
        context: Context,
        resources: Resources,
        onConfirm: (String) -> Unit
    ) {
        val inputLayout = TextInputLayout(context).apply {
            hint = resources.getString(R.string.folder_name_hint)
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setPadding(64, 16, 64, 0)
        }
        val editText = TextInputEditText(inputLayout.context).apply {
            isSingleLine = true
        }
        inputLayout.addView(editText)

        MaterialAlertDialogBuilder(context)
            .setIcon(R.drawable.create_new_folder_24px)
            .setTitle(R.string.new_folder)
            .setView(inputLayout)
            .setPositiveButton(R.string.create) { _, _ ->
                val name = editText.text?.toString()?.trim()
                if (!name.isNullOrBlank()) {
                    onConfirm(name)
                }
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()

        editText.requestFocus()
    }

    fun showAddLinkDialog(
        context: Context,
        resources: Resources,
        initialUrl: String? = null,
        onConfirm: (url: String, name: String?) -> Unit
    ) {
        val container = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(64, 16, 64, 0)
        }

        val urlInputLayout = TextInputLayout(context).apply {
            hint = resources.getString(R.string.link_url_hint)
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        }
        val urlEditText = TextInputEditText(urlInputLayout.context).apply {
            isSingleLine = true
            initialUrl?.let { setText(it) }
        }
        urlInputLayout.addView(urlEditText)

        val nameInputLayout = TextInputLayout(context).apply {
            hint = resources.getString(R.string.link_name_optional_hint)
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setPadding(0, 24, 0, 0)
        }
        val nameEditText = TextInputEditText(nameInputLayout.context).apply {
            isSingleLine = true
        }
        nameInputLayout.addView(nameEditText)

        container.addView(urlInputLayout)
        container.addView(nameInputLayout)

        MaterialAlertDialogBuilder(context)
            .setIcon(R.drawable.add_link_24px)
            .setTitle(R.string.add_link)
            .setView(container)
            .setPositiveButton(R.string.add) { _, _ ->
                val url = urlEditText.text?.toString()?.trim()
                val name = nameEditText.text?.toString()?.trim()
                if (!url.isNullOrBlank()) {
                    onConfirm(url, name)
                }
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()

        urlEditText.requestFocus()
    }

    fun showRenameDialog(
        context: Context,
        resources: Resources,
        currentName: String,
        isFolder: Boolean,
        onConfirm: (String) -> Unit
    ) {
        val inputLayout = TextInputLayout(context).apply {
            hint = resources.getString(if (isFolder) R.string.folder_name_hint else R.string.display_name_hint)
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setPadding(64, 16, 64, 0)
        }
        val editText = TextInputEditText(inputLayout.context).apply {
            isSingleLine = true
            setText(currentName)
            selectAll()
        }
        inputLayout.addView(editText)

        MaterialAlertDialogBuilder(context)
            .setIcon(R.drawable.edit_24px)
            .setTitle(R.string.rename)
            .setView(inputLayout)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = editText.text?.toString()?.trim()
                if (!name.isNullOrBlank()) {
                    onConfirm(name)
                }
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()

        editText.requestFocus()
    }

    fun showMoveDialog(
        context: Context,
        resources: Resources,
        allFolders: List<Folder>,
        currentFolderId: Long?,
        excludeFolderId: Long? = null,
        titleResId: Int = R.string.move_to_folder,
        onFolderSelected: (targetFolderId: Long?) -> Unit
    ) {
        // Filter out excluded folder and its descendants
        val availableFolders = allFolders.filter { it.id != excludeFolderId }

        val folderNames = mutableListOf<String>()
        val folderIds = mutableListOf<Long?>()

        folderNames.add(resources.getString(R.string.library_root))
        folderIds.add(null)

        availableFolders.forEach { folder ->
            folderNames.add(folder.name)
            folderIds.add(folder.id)
        }

        val checkedIndex = folderIds.indexOf(currentFolderId).coerceAtLeast(0)

        MaterialAlertDialogBuilder(context)
            .setIcon(R.drawable.drive_file_move_24px)
            .setTitle(titleResId)
            .setSingleChoiceItems(folderNames.toTypedArray(), checkedIndex) { dialog, which ->
                val selectedId = folderIds[which]
                onFolderSelected(selectedId)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    fun showDeleteConfirmDialog(
        context: Context,
        resources: Resources,
        itemName: String,
        isFolder: Boolean,
        onConfirm: () -> Unit
    ) {
        val title = if (isFolder) resources.getString(R.string.delete_folder_dialog_title)
        else resources.getString(R.string.delete_resource_dialog_title)
        val message = if (isFolder) resources.getString(R.string.delete_folder_dialog_message, itemName)
        else resources.getString(R.string.delete_resource_dialog_message, itemName)

        MaterialAlertDialogBuilder(context)
            .setIcon(R.drawable.delete_forever_24px)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton(resources.getString(R.string.delete)) { _, _ ->
                onConfirm()
            }
            .setPositiveButton(resources.getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
