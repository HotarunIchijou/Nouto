package org.kaorun.nouto.ui.fragments.settings

import org.kaorun.nouto.R
import org.kaorun.nouto.ui.fragments.base.SettingsBaseFragment
import org.kaorun.nouto.ui.fragments.settings.preferences.PreferenceLanguageFragment

class SettingsLanguageFragment : SettingsBaseFragment() {
    override val titleRes = R.string.language
    override fun preferenceFragment() = PreferenceLanguageFragment()
}