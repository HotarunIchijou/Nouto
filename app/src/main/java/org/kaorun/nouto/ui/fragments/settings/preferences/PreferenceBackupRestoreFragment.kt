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
import de.raphaelebner.roomdatabasebackup.core.RoomBackup
import org.kaorun.nouto.R
import org.kaorun.nouto.data.NoteDatabase
import org.kaorun.nouto.ui.MainActivity
import org.kaorun.nouto.ui.components.Snackbars
import org.kaorun.nouto.ui.fragments.base.PreferenceBaseFragment
import org.kaorun.nouto.ui.fragments.settings.SettingsBackupRestoreFragmentDirections
import org.kaorun.nouto.ui.utils.FileUtils

class PreferenceBackupRestoreFragment : PreferenceBaseFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val databaseBackup = (requireActivity() as MainActivity).roomBackup
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
            databaseBackup
                .database(NoteDatabase.getDatabase(requireContext()))
                .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_DIALOG)
                .apply {
                    onCompleteListener { success, message, exitCode ->
                        if (success) {
                            Snackbar
                                .make(
                                    requireView(),
                                    "${getString(R.string.backup_database_success)} ($exitCode)",
                                    Snackbar.LENGTH_SHORT
                                )
                                .show()
                        } else if (exitCode == 3) {
                            return@onCompleteListener
                        } else {
                            Snackbar
                                .make(
                                    requireView(),
                                    "${getString(R.string.error_occured)}: $message (Exit code $exitCode)",
                                    Snackbar.LENGTH_SHORT
                                )
                                .show()
                        }
                    }
                }
                .backup()
            true
        }

        findPreference<Preference>("restore")?.setOnPreferenceClickListener {
            databaseBackup
                .database(NoteDatabase.getDatabase(requireContext()))
                .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_DIALOG)
                .apply {
                    onCompleteListener { success, message, exitCode ->
                        if (success) {
                            MaterialAlertDialogBuilder(requireContext())
                                .setCancelable(false)
                                .setIcon(R.drawable.restart_alt_24px)
                                .setTitle(R.string.restart_app)
                                .setMessage(R.string.restart_app_summary)
                                .setPositiveButton(R.string.restart) { _, _ ->
                                    restartApp(
                                        Intent(
                                            requireActivity(),
                                            MainActivity::class.java
                                        ).putExtra("restore_success", true)
                                    )
                                }
                                .show()
                        } else if (exitCode == 2) {
                            return@onCompleteListener
                        } else {
                            Snackbar
                                .make(
                                    requireView(),
                                    "${getString(R.string.error_occured)}: $message (Exit code $exitCode)",
                                    Snackbar.LENGTH_SHORT
                                )
                                .show()
                        }
                    }
                }
                .restore()
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