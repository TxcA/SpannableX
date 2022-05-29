/*
 * Copyright (C) 2022 TxcA, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.itxca.spannablex.utils

import android.graphics.drawable.Drawable
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.IntRange
import androidx.annotation.Keep
import com.drake.spannable.span.CenterImageSpan

/**
 * [CenterImageSpan] 的大小配置辅助类
 */
@Keep
data class DrawableSize(
    @IntRange(from = 0L) val width: Int,
    @IntRange(from = 0L) val height: Int = width
)

/**
 * 快速构建 [DrawableSize]
 */
val Int.drawableSize: DrawableSize
    get() = DrawableSize(this, this)


/**
 * 设置[Drawable] 大小
 */
fun Drawable.drawableSize(width: Int, height: Int = width): Drawable = apply {
    setBounds(0, 0, width, height)
}

/**
 * 设置[Drawable] 大小为[TextView] [EditText] 的字体大小
 * @param view 参考文字大小的textSize view
 */
fun <T : TextView> Drawable.configTextViewSize(view: T?): Drawable = apply {
    view?.textSizeInt?.let { size ->
        drawableSize(size, size)
    } ?: drawableSize(intrinsicWidth, intrinsicHeight)
}