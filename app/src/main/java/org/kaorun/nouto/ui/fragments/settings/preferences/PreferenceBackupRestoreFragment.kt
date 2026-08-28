package org.kaorun.nouto.ui.fragments.settings.preferences

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import org.kaorun.nouto.R
import org.kaorun.nouto.data.NoteDatabase
import org.kaorun.nouto.ui.MainActivity
import org.kaorun.nouto.ui.components.Snackbars
import org.kaorun.nouto.ui.fragments.base.PreferenceBaseFragment
import org.kaorun.nouto.ui.fragments.settings.SettingsBackupRestoreFragmentDirections
import org.kaorun.nouto.ui.utils.FileUtils

class PreferenceBackupRestoreFragment : PreferenceBaseFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        val importFilePicker: ActivityResultLauncher<String> = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                FileUtils.readFromFile(
                    context = requireContext(),
                    uri = uri
                ).onSuccess { (title, content) ->
                    findNavController().navigate(
                        SettingsBackupRestoreFragmentDirections
                            .actionSettingsBackupRestoreFragmentToNoteFragment(
                                noteId = -1,
                                title = title,
                                content = content
                            )
                    )
                }.onFailure {
                    Snackbars.showSnackbarNoAction(
                        view = requireView().rootView,
                        anchorView = null,
                        message = ""
                    )
                }
            }
        }

        val backupFilePicker = registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/zip")
        ) { uri ->
            uri?.let {
                lifecycleScope.launch {
                    val result = org.kaorun.nouto.ui.utils.BackupRestoreUtils.createFullBackup(requireContext(), it)
                    if (result.isSuccess) {
                        Snackbar.make(requireView(), R.string.backup_database_success, Snackbar.LENGTH_SHORT).show()
                    } else {
                        Snackbar.make(requireView(), "${getString(R.string.error_occured)}: ${result.exceptionOrNull()?.message}", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }

        val restoreFilePicker = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                lifecycleScope.launch {
                    val result = org.kaorun.nouto.ui.utils.BackupRestoreUtils.restoreFullBackup(requireContext(), it)
                    if (result.isSuccess) {
                        MaterialAlertDialogBuilder(requireContext())
                            .setCancelable(false)
                            .setIcon(R.drawable.restart_alt_24px)
                            .setTitle(R.string.restart_app)
                            .setMessage(R.string.restart_app_summary)
                            .setPositiveButton(R.string.restart) { _, _ ->
                                val intent = Intent(requireActivity(), MainActivity::class.java).putExtra("restore_success", true)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                startActivity(intent)
                            }
                            .show()
                    } else {
                        Snackbar.make(requireView(), "${getString(R.string.error_occured)}: ${result.exceptionOrNull()?.message}", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }

        setPreferencesFromResource(R.xml.preferences_backup_restore, rootKey)

        findPreference<Preference>("export")?.setOnPreferenceClickListener {
            findNavController().navigate(SettingsBackupRestoreFragmentDirections.actionSettingsBackupRestoreFragmentToNoteExportFragment())
            true
        }

        findPreference<Preference>("import")?.setOnPreferenceClickListener {
            importFilePicker.launch("application/json")
            true
        }

        findPreference<Preference>("backup")?.setOnPreferenceClickListener {
            val formatter = java.text.SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", java.util.Locale.getDefault())
            val dateString = formatter.format(java.util.Date())
            backupFilePicker.launch("nouto_backup_$dateString.zip")
            true
        }

        findPreference<Preference>("restore")?.setOnPreferenceClickListener {
            restoreFilePicker.launch("application/zip")
            true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        textView = LayoutInflater.from(requireContext())
            .inflate(R.layout.text_backup_restore_description, listView, false)
        imageView = LayoutInflater.from(requireContext())
            .inflate(R.layout.illustration_backup_restore, listView, false)
        super.onViewCreated(view, savedInstanceState)
    }
}