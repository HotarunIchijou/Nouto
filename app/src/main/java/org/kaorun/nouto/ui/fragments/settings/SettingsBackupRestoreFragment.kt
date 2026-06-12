package org.kaorun.nouto.ui.fragments.settings

import org.kaorun.nouto.R
import org.kaorun.nouto.ui.fragments.base.SettingsBaseFragment
import org.kaorun.nouto.ui.fragments.settings.preferences.PreferenceBackupRestoreFragment

class SettingsBackupRestoreFragment: SettingsBaseFragment() {
    override val titleRes = R.string.backup_and_restore
    override fun preferenceFragment() = PreferenceBackupRestoreFragment()
}