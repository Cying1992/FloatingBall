package com.cying.floatingball

import android.content.Context
import android.support.v4.widget.ViewDragHelper
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout


class FloatingBallView(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {

        val FLING_MIN_DISTANCE = 100
        val FLING_MIN_VELOCITY = 200


        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            MockAction.BACK.trigger()
            return true
        }

        override fun onLongPress(e: MotionEvent?) {
            isLongPressing = true
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {


            if (e1.y - e2.y > FLING_MIN_DISTANCE && Math.abs(velocityY) > FLING_MIN_VELOCITY) {
                // Fling top
                MockAction.HOME.trigger()
                return true
            } else if (e2.y - e1.y > FLING_MIN_DISTANCE && Math.abs(velocityX) > FLING_MIN_VELOCITY) {
                // Fling bottom
                MockAction.LOCK.trigger()
                return true
            }

            return super.onFling(e1, e2, velocityX, velocityY)
        }

        override fun onDoubleTap(e: android.view.MotionEvent?): kotlin.Boolean {
            MockAction.RECENTS.trigger()
            return true
        }
    }

    private val gest = GestureDetector(context, gestureListener).apply {
        setOnDoubleTapListener(gestureListener)
        setIsLongpressEnabled(true)
    }

    private var isLongPressing = false

    var updatePositionCallback: UpdatePositionCallback? = null
        set

    private val dragHelper = ViewDragHelper.create(this, 0.5F, DragCallback())

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {

        val interceptForDrag = dragHelper.shouldInterceptTouchEvent(ev)
        var interceptForTap = false
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> interceptForTap = true
        }

        return interceptForDrag || interceptForTap
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        when (event.action) {
            MotionEvent.ACTION_UP -> isLongPressing = false
            MotionEvent.ACTION_MOVE -> {
                if (isLongPressing) {
                    updatePositionCallback?.update(this, event.rawX, event.rawY)
                }
            }
        }
        if (event.action == MotionEvent.ACTION_UP) {
            isLongPressing = false
        }
        return gest.onTouchEvent(event)
    }

    inner class DragCallback : ViewDragHelper.Callback() {
        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            return true
        }

        override fun onViewReleased(releasedChild: View?, xvel: Float, yvel: Float) {
            super.onViewReleased(releasedChild, xvel, yvel)
        }
    }
}
