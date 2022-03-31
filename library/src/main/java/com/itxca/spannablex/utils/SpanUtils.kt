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

import android.content.res.Resources
import android.graphics.Color
import android.widget.TextView
import androidx.annotation.ColorInt
import kotlin.math.roundToInt

/**
 * ColorString 2 [ColorInt]
 * error default is [Color.RED]
 */
val String.color: Int
    get() = try {
        Color.parseColor(this)
    } catch (e: Exception) {
        Color.RED
    }

/**
 * @receiver to dp
 */
val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density + 0.5f).roundToInt()

/**
 * @receiver to sp
 */
val Int.sp: Int
    get() = (this * Resources.getSystem().displayMetrics.scaledDensity + 0.5f).roundToInt()

/**
 * 获取Int型[TextView.getTextSize]
 */
val TextView.textSizeInt: Int
    get() = textSize.roundToInt()