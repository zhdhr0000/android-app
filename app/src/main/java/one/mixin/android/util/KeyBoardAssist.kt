package one.mixin.android.util

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import one.mixin.android.extension.hasNavBar
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.statusBarHeight

class KeyBoardAssist constructor(content: ViewGroup, private val isFull: Boolean = false) {

    private val mChildOfContent: View = content.getChildAt(0)
    private var usableHeightPrevious: Int = 0
    private val frameLayoutParams: FrameLayout.LayoutParams

    var keyBoardShow = false

    init {
        mChildOfContent.viewTreeObserver.addOnGlobalLayoutListener { possiblyResizeChildOfContent() }
        frameLayoutParams = mChildOfContent.layoutParams as FrameLayout.LayoutParams
    }

    private fun possiblyResizeChildOfContent() {
        val usableHeightNow = computeUsableHeight()
        if (usableHeightNow != usableHeightPrevious) {
            val usableHeightSansKeyboard = mChildOfContent.rootView.height
            val heightDifference = usableHeightSansKeyboard - usableHeightNow
            if (heightDifference > usableHeightSansKeyboard / 4) {
                keyBoardShow = true
                frameLayoutParams.height = usableHeightSansKeyboard - heightDifference - if (isFull) {
                    0
                } else {
                    mChildOfContent.context.statusBarHeight() + if (mChildOfContent.context.hasNavBar()) {
                        mChildOfContent.context.navigationBarHeight()
                    } else {
                        0
                    }
                }
            } else {
                keyBoardShow = false
                frameLayoutParams.height = usableHeightSansKeyboard - if (isFull) {
                    0
                } else {
                    mChildOfContent.context.statusBarHeight() + if (mChildOfContent.context.hasNavBar()) {
                        mChildOfContent.context.navigationBarHeight()
                    } else {
                        0
                    }
                }
            }
            mChildOfContent.requestLayout()
            usableHeightPrevious = usableHeightNow
        }
    }

    private fun computeUsableHeight(): Int {
        val r = Rect()
        mChildOfContent.getWindowVisibleDisplayFrame(r)
        return r.bottom - r.top
    }

    companion object {
        fun assistContent(contentView: ViewGroup): KeyBoardAssist {
            return KeyBoardAssist(contentView)
        }
    }
}