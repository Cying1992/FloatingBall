package com.cying.floatingball

import android.content.Context
import android.support.v4.view.ViewCompat
import android.support.v4.widget.ViewDragHelper
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import org.jetbrains.anko.dip

/**
 * Created by Cying on 17/9/29.
 */
private const val TAG = "TrackingBallLayout"

class TrackingBallLayout(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0, 0)
    constructor(context: Context) : this(context, null, 0, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)

    private val dragHelper = ViewDragHelper.create(this, 0.5F, DragCallback())

    private var isLongPressing = false
    private var moveVertical = false

    private val MOVE_SLOP = dip(2)

    private var initX = 0F
    private var initY = 0F
    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent?) {
            isLongPressing = true
        }

        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            MockAction.BACK.trigger()
            return true
        }
    }

    var updatePositionCallback: UpdatePositionCallback? = null
        set

    private val gestureDetector = GestureDetector(context, gestureListener).apply {
        setIsLongpressEnabled(true)
    }

    override fun computeScroll() {
        if (dragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return dragHelper.shouldInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {

        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                initX = ev.x
                initY = ev.y
            }

            MotionEvent.ACTION_MOVE -> {
                if (isLongPressing) {
                    updatePositionCallback?.update(this, ev.rawX, ev.rawY)
                } else {

                    moveVertical = Math.abs(ev.y - initY) > Math.abs(ev.x - initX)

                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> isLongPressing = false

        }

        if (gestureDetector.onTouchEvent(ev) || isLongPressing) {
            dragHelper.cancel()
        } else {
            dragHelper.processTouchEvent(ev)
        }


        return true
    }

    inner class DragCallback : ViewDragHelper.Callback() {

        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            return !isLongPressing && child.id == R.id.ball
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {

            if (!isLongPressing) {
                var triggered = false
                when (releasedChild.top) {
                    0 -> {//context.toast("top")
                        MockAction.HOME.trigger()
                        triggered = true
                    }
                    height - releasedChild.height -> {// context.toast("bottom")
                        MockAction.LOCK.trigger()
                        triggered = true
                    }
                }
                if (!triggered) {

                    when (releasedChild.left) {
                        0 -> MockAction.RECENTS.trigger()
                        width - releasedChild.width -> MockAction.NOTIFICATIONS.trigger()
                    }
                }
            }
            isLongPressing = false
            dragHelper.settleCapturedViewAt((width - releasedChild.width) / 2, (height - releasedChild.height) / 2)
            invalidate()
        }


        override fun getViewVerticalDragRange(child: View): Int {
            return child.height
        }

        override fun getViewHorizontalDragRange(child: View): Int {
            return child.width
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            if (!moveVertical) {
                return (height - child.height) / 2
            }

            val topBound = 0
            val bottomBound = height - child.height
            return Math.min(Math.max(top, topBound), bottomBound)
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {


            if (moveVertical) {
                Log.i(TAG, dx.toString() + "----1 ")
                return (width - child.width) / 2
            }

            val leftBound = 0
            val rightBound = width - child.width

            return Math.min(Math.max(left, leftBound), rightBound)
        }
    }
}