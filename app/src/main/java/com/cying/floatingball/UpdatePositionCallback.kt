package com.cying.floatingball

import android.view.View

/**
 * Created by Cying on 17/9/28.
 */

interface UpdatePositionCallback {

    fun update(view: View, x: Float, y: Float)
}