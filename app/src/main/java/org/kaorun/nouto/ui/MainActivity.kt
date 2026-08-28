package org.kaorun.nouto.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import de.raphaelebner.roomdatabasebackup.core.RoomBackup
import kotlinx.coroutines.launch
import org.kaorun.nouto.BuildConfig
import org.kaorun.nouto.NavGraphDirections
import org.kaorun.nouto.R
import org.kaorun.nouto.ui.components.FreeDroidWarnDialog
import org.kaorun.nouto.ui.components.ShareImportBottomSheet
import org.kaorun.nouto.ui.utils.BlackThemeHelper
import org.kaorun.nouto.ui.utils.ColorThemeHelper

class MainActivity : AppCompatActivity() {
    lateinit var roomBackup: RoomBackup
    private var warnDialog: android.app.Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ColorThemeHelper.apply(this)
        BlackThemeHelper.apply(this)
        roomBackup = RoomBackup(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        warnDialog = FreeDroidWarnDialog.show(this, BuildConfig.VERSION_CODE)
        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        if (action == Intent.ACTION_SEND) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)

            if (uri != null) {
                showShareImportSheet(arrayListOf(uri), null)
            } else if (!text.isNullOrBlank()) {
                showShareImportSheet(null, text)
            }
        } else if (action == Intent.ACTION_SEND_MULTIPLE) {
            val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
            }
            if (!uris.isNullOrEmpty()) {
                showShareImportSheet(ArrayList(uris), null)
            }
        }
    }

    private fun showShareImportSheet(uris: ArrayList<Uri>?, text: String?) {
        lifecycleScope.launch {
            val dao = org.kaorun.nouto.data.NoteDatabase.getDatabase(applicationContext).libraryDao()
            val repository = org.kaorun.nouto.repository.LibraryRepository(dao)
            val allFolders = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                repository.getAllFoldersSync()
            }
            
            org.kaorun.nouto.ui.components.LibraryDialogs.showMoveDialog(
                this@MainActivity,
                resources,
                allFolders,
                currentFolderId = null,
                excludeFolderId = null,
                titleResId = R.string.select_destination_folder
            ) { targetFolderId ->
                lifecycleScope.launch {
                    org.kaorun.nouto.ui.utils.ShareImportHelper.importToFolder(this@MainActivity, uris, text, repository, targetFolderId)
                    
                    val navHostFragment = supportFragmentManager
                        .findFragmentById(R.id.nav_host_fragment_container) as? NavHostFragment
                    val navController = navHostFragment?.navController
                    navController?.navigate(
                        NavGraphDirections.actionGlobalLibraryFragment(
                            folderId = targetFolderId ?: -1L,
                            folderName = null
                        )
                    )
                    
                    android.widget.Toast.makeText(this@MainActivity, "Saved to Nouto Library", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        warnDialog?.takeIf { it.isShowing }?.dismiss()
        warnDialog = null
    }
}