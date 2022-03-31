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

package com.itxca.spannablex

import android.app.Activity
import android.app.Fragment
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.CharacterStyle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.core.view.children
import androidx.viewbinding.ViewBinding
import com.drake.spannable.movement.ClickableMovementMethod

/**
 * 构建Spannable
 * @see [SpanDsl]
 */
fun Any?.spannable(builderAction: SpanDsl.() -> Unit): SpannableStringBuilder =
    SpanDsl.create(
        text = if (this is CharSequence) this else null, replaceRule = null
    ).apply(builderAction).spannable()

//<editor-fold desc="删除Span">
/**
 * 删除指定Span
 */
inline fun <reified T> CharSequence.removeSpans(): CharSequence =
    (if (this is Spannable) this else SpannableString(this)).apply {
        val allSpans = getSpans(0, length, T::class.java)
        for (span in allSpans) {
            removeSpan(span)
        }
    }

/**
 * 删除所有[CharacterStyle] Span
 */
fun CharSequence.removeAllSpans(): CharSequence =
    (if (this is Spannable) this else SpannableString(this)).apply {
        val allSpans = getSpans(0, length, CharacterStyle::class.java)
        for (span in allSpans) {
            removeSpan(span)
        }
    }

//</editor-fold>

//<editor-fold desc="配置 Movement Method">
/**
 * 配置 [LinkMovementMethod] 或 [ClickableMovementMethod]
 * @param background 是否显示点击背景
 */
fun TextView.activateClick(background: Boolean = true): TextView = apply {
    movementMethod = if (background) LinkMovementMethod.getInstance() else ClickableMovementMethod.getInstance()
}

/**
 * 循环获取控件并配置 [LinkMovementMethod] 或 [ClickableMovementMethod]
 * @param background 是否显示点击背景
 * @param ignoreId 忽略配置movementMethod的ViewId
 */
fun View?.autoActivateClick(background: Boolean, @IdRes vararg ignoreId: Int) {
    when (this) {
        is TextView -> {
            if (!ignoreId.contains(id)) {
                activateClick(background)
            }
        }
        is ViewGroup -> {
            children.forEach {
                it.autoActivateClick(background, *ignoreId)
            }
        }
    }
}

/**
 * 循环 [ViewBinding] 控件并配置 [LinkMovementMethod] 或 [ClickableMovementMethod]
 * @param background 是否显示点击背景
 * @param ignoreId 忽略配置movementMethod的ViewId
 */
fun ViewBinding.activateAllTextViewClick(
    background: Boolean = true,
    @IdRes vararg ignoreId: Int
) {
    root.autoActivateClick(background, *ignoreId)
}

/**
 * 循环 [Activity] 控件并配置 [LinkMovementMethod] 或 [ClickableMovementMethod]
 * @param background 是否显示点击背景
 * @param ignoreId 忽略配置movementMethod的ViewId
 */
fun Activity.activateAllTextViewClick(background: Boolean = true, @IdRes vararg ignoreId: Int) {
    findViewById<ViewGroup>(android.R.id.content).children.first()
        .autoActivateClick(background, *ignoreId)
}

/**
 * 循环 [Fragment] 控件并配置 [LinkMovementMethod] 或 [ClickableMovementMethod]
 * @param background 是否显示点击背景
 * @param ignoreId 忽略配置movementMethod的ViewId
 */
fun Fragment.activateAllTextViewClick(background: Boolean = true, @IdRes vararg ignoreId: Int) {
    view.autoActivateClick(background, *ignoreId)
}
//</editor-fold>

//<editor-fold desc="Spannable plus">
/**
 * [String] 转为 [Spannable], 以便进行plus操作
 */
val String.span: SpannedString
    get() = SpannedString(this)

/**
 * 扩展Spanned +, 保留样式
 * operator [Spannable] + [CharSequence]
 * @return [Spannable]
 */
operator fun Spanned.plus(other: CharSequence): SpannableStringBuilder =
    when (this) {
        is SpannableStringBuilder -> append(other)
        else -> SpannableStringBuilder(this).append(other)
    }

//</editor-fold>