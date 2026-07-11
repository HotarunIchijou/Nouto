package org.kaorun.nouto.ui.utils

import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.widget.NestedScrollView

object InsetsHandler {
    fun applyViewInsets(
        view: View,
        isTopPaddingEnabled: Boolean = true,
        isBottomPaddingEnabled: Boolean = true,
        isHorizontalPaddingEnabled: Boolean = true,
        isAdditionalTopPaddingEnabled: Boolean = false,
        isAdditionalBottomPaddingEnabled: Boolean = true,
        isAdditionalHorizontalPaddingEnabled: Boolean = false,
        isTopSystemPaddingEnabled: Boolean = true,
        additionalPadding: Int = 0) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.displayCutout()
            )
            
            val topPadding = (if (isTopSystemPaddingEnabled) systemBars.top else 0) +
                    (if (isAdditionalTopPaddingEnabled) additionalPadding else 0)
            val bottomPadding = if (isAdditionalBottomPaddingEnabled) additionalPadding else 0
            val horizontalPadding = if (isAdditionalHorizontalPaddingEnabled) additionalPadding else 0

            v.setPadding(
                if (isHorizontalPaddingEnabled) systemBars.left + horizontalPadding
                else 0,
                if (isTopPaddingEnabled) topPadding else 0,
                if (isHorizontalPaddingEnabled) systemBars.right + horizontalPadding
                else 0,
                if (isBottomPaddingEnabled) systemBars.bottom + bottomPadding else 0
            )
            insets
        }
    }

    fun applyViewInsets(view: View, additionalMargin: Int) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.displayCutout()
            )

            v.updateLayoutParams<MarginLayoutParams> {
                leftMargin = insets.left + additionalMargin
                topMargin = insets.top + additionalMargin
                rightMargin = insets.right + additionalMargin
                bottomMargin = insets.bottom + additionalMargin
            }
            windowInsets
        }
    }

    fun applyImeInsets(view: View) {
        ViewCompat.setWindowInsetsAnimationCallback(
            view,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                var startBottom = 0f
                var endBottom = 0f

                override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                    startBottom = view.bottom.toFloat()
                }

                override fun onStart(
                    animation: WindowInsetsAnimationCompat,
                    bounds: WindowInsetsAnimationCompat.BoundsCompat
                ): WindowInsetsAnimationCompat.BoundsCompat {
                    endBottom = view.bottom.toFloat()
                    return bounds
                }

                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>
                ): WindowInsetsCompat {
                    val imeAnimation = runningAnimations.find {
                        it.typeMask and WindowInsetsCompat.Type.ime() != 0
                    } ?: return insets

                    val translation = (startBottom - endBottom) * (1 - imeAnimation.interpolatedFraction)
                    view.translationY = translation

                    return insets
                }
            }
        )
    }

    fun applyImeInsets(scrollView: NestedScrollView) {
        ViewCompat.setWindowInsetsAnimationCallback(
            scrollView,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>
                ): WindowInsetsCompat {
                    val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                    scrollView.updateLayoutParams<MarginLayoutParams> {
                        bottomMargin = imeHeight
                    }
                    return insets
                }
            }
        )
    }
}