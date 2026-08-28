package org.kaorun.nouto.ui.components

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnticipateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.view.children
import androidx.core.view.isVisible
import com.google.android.material.floatingactionbutton.FloatingActionButton

class FabMenuController(
    private val mainFab: FloatingActionButton,
    private val menuContainer: ViewGroup,
    private val scrimView: View? = null,
    private val onNoteClick: () -> Unit,
    private val onFolderClick: () -> Unit,
    private val onImportFilesClick: () -> Unit,
    private val onLinkClick: () -> Unit
) {
    var isExpanded: Boolean = false
        private set

    init {
        mainFab.setOnClickListener {
            toggle()
        }

        scrimView?.setOnClickListener {
            if (isExpanded) {
                collapse()
            }
        }

        menuContainer.findViewById<View>(org.kaorun.nouto.R.id.sub_fab_create_note)?.setOnClickListener {
            collapse()
            onNoteClick()
        }

        menuContainer.findViewById<View>(org.kaorun.nouto.R.id.sub_fab_create_folder)?.setOnClickListener {
            collapse()
            onFolderClick()
        }

        menuContainer.findViewById<View>(org.kaorun.nouto.R.id.sub_fab_import_files)?.setOnClickListener {
            collapse()
            onImportFilesClick()
        }

        menuContainer.findViewById<View>(org.kaorun.nouto.R.id.sub_fab_link)?.setOnClickListener {
            collapse()
            onLinkClick()
        }
    }

    fun toggle() {
        if (isExpanded) collapse() else expand()
    }

    fun expand() {
        if (isExpanded) return
        isExpanded = true

        scrimView?.let { scrim ->
            scrim.alpha = 0f
            scrim.isVisible = true
            scrim.animate()
                .alpha(1f)
                .setDuration(250)
                .start()
        }

        mainFab.animate()
            .rotation(135f)
            .setDuration(250)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()

        menuContainer.isVisible = true
        val children = menuContainer.children.toList()
        val totalChildren = children.size

        children.forEachIndexed { index, child ->
            child.alpha = 0f
            child.scaleX = 0.7f
            child.scaleY = 0.7f
            child.translationY = 40f

            val delay = ((totalChildren - 1 - index) * 35).toLong()

            child.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(280)
                .setStartDelay(delay)
                .setInterpolator(OvershootInterpolator(1.1f))
                .start()
        }
    }

    fun collapse() {
        if (!isExpanded) return
        isExpanded = false

        scrimView?.animate()
            ?.alpha(0f)
            ?.setDuration(200)
            ?.withEndAction {
                scrimView.isVisible = false
            }
            ?.start()

        mainFab.animate()
            .rotation(0f)
            .setDuration(250)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()

        val children = menuContainer.children.toList()

        children.forEachIndexed { index, child ->
            val delay = (index * 25).toLong()
            child.animate()
                .alpha(0f)
                .scaleX(0.7f)
                .scaleY(0.7f)
                .translationY(30f)
                .setDuration(200)
                .setStartDelay(delay)
                .setInterpolator(AnticipateInterpolator(1.1f))
                .withEndAction {
                    if (index == children.lastIndex && !isExpanded) {
                        menuContainer.isVisible = false
                    }
                }
                .start()
        }
    }
}
