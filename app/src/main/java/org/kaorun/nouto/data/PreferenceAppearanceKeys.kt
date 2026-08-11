// org.kaorun.nouto.utils.PrefKeys.kt
package org.kaorun.nouto.data

import org.kaorun.nouto.ui.utils.BlackThemeHelper
import org.kaorun.nouto.ui.utils.ColorThemeHelper
import org.kaorun.nouto.ui.utils.ThemeHelper

object PreferenceAppearanceKeys {
    const val WIDGET_VIEW_ONLY_MODE = "view_only_mode"
    const val WIDGET_SHOW_TITLE = "show_title"
    const val WIDGET_SHOW_KEYBOARD = "show_keyboard"
    const val WIDGET_FOCUS_ON_CONTENT = "focus_on_content"
    const val WIDGET_THEME = ThemeHelper.KEY
    const val WIDGET_BLACK_THEME = BlackThemeHelper.KEY
    const val WIDGET_DYNAMIC_COLORS = ColorThemeHelper.KEY_DYNAMIC

    const val IS_VIEW_ONLY_MODE = "is_view_only_mode"
    const val IS_SHOW_TITLE = "is_show_title"
    const val IS_SHOW_KEYBOARD = "is_show_keyboard"
    const val IS_FOCUS_ON_CONTENT = "is_focus_on_content"
}