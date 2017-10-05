package com.cying.floatingball

import android.content.Context
import android.support.v4.view.ViewCompat
import android.support.v4.widget.ViewDragHelper
import android.util.AttributeSet
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
    private val vibrator = context.getVibrator()
    private val vibratorPattern = longArrayOf(0L, 10L, 20L, 30L)

    var doubleClickEnabled = false
        set(value) {
            field = value
            gestureDetector.setOnDoubleTapListener(if (value) gestureListener else null)
        }

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
        override fun onLongPress(e: MotionEvent) {
            isLongPressing = true
            repeat(childCount) {
                getChildAt(it).isActivated = true
            }
            if (ActionSettings.needVibrate) {
                vibrator.vibrate(vibratorPattern, -1)
            }
        }


        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            if (doubleClickEnabled) {
                return false
            }
            GESTURE.CLICK.trigger()
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            if (doubleClickEnabled) {
                GESTURE.CLICK.trigger()
                return true
            }
            return false
        }

        override fun onDoubleTap(e: MotionEvent?): Boolean {
            if (doubleClickEnabled) {
                GESTURE.DOUBLE_CLICK.trigger()
                return true
            }
            return false
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
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isLongPressing = false
                repeat(childCount) {
                    getChildAt(it).isActivated = false
                }
            }

        }


        dragHelper.processTouchEvent(ev)
        gestureDetector.onTouchEvent(ev)
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
                    0 -> {
                        GESTURE.SWIPE_TOP.trigger()
                        triggered = true
                    }
                    height - releasedChild.height -> {// context.toast("bottom")
                        GESTURE.SWIPE_BOTTOM.trigger()
                        triggered = true
                    }
                }
                if (!triggered) {

                    when (releasedChild.left) {
                        0 -> GESTURE.SWIPE_LEFT.trigger()
                        width - releasedChild.width -> GESTURE.SWIPE_RIGHT.trigger()
                    }
                }
            }
            isLongPressing = false
            if (dragHelper.settleCapturedViewAt((width - releasedChild.width) / 2, (height - releasedChild.height) / 2)) {
                invalidate()
            }
        }


        override fun getViewVerticalDragRange(child: View): Int {
            return child.height
        }

        override fun getViewHorizontalDragRange(child: View): Int {
            return child.width
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            if (!moveVertical || isLongPressing) {
                return (height - child.height) / 2
            }

            val topBound = 0
            val bottomBound = height - child.height
            return Math.min(Math.max(top, topBound), bottomBound)
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {


            if (moveVertical || isLongPressing) {
                return (width - child.width) / 2
            }

            val leftBound = 0
            val rightBound = width - child.width

            return Math.min(Math.max(left, leftBound), rightBound)
        }
    }
}