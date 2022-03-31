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
@file:JvmName("SpanInternal")
@file:Suppress("unused")

package com.itxca.spannablex

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.style.*
import android.util.Log
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import com.drake.spannable.replaceSpan
import com.drake.spannable.setSpan
import com.itxca.spannablex.annotation.TextStyle
import com.itxca.spannablex.interfaces.OnSpanClickListener
import com.itxca.spannablex.span.SimpleClickableConfig
import com.itxca.spannablex.span.SimpleClickableSpan
import com.itxca.spannablex.span.SizeImageSpan
import com.itxca.spannablex.utils.DrawableSize
import com.itxca.spannablex.utils.color
import com.itxca.spannablex.utils.textSizeInt
import java.util.*

//<editor-fold desc="日志">
internal fun Throwable.logW(message: String? = null) {
    Log.w("SpannableX", "${message?.plus(" - ") ?: ""}${this.message ?: "unknown error"}")
}
//</editor-fold>

//<editor-fold desc="内部方法">
/**
 * ImageSpan Text标识
 */
internal const val IMAGE_SPAN_TAG = " "

private const val UNKNOWN_REPLACE_RULES =
    "Unknown replace rules. please use `String(list/array)`, `Regex(list/array)`, `ReplaceRule(list/array)`."

/**
 * [SizeImageSpan] 适配 [Drawable] size
 */
private fun SizeImageSpan.setupSize(
    useTextViewSize: TextView?,
    size: DrawableSize?
): SizeImageSpan = apply {
    useTextViewSize?.textSizeInt?.let { textSize ->
        drawableSize(textSize, textSize)
    } ?: size?.let { drawableSize ->
        drawableSize(drawableSize.width, drawableSize.height)
    }
}

/**
 * 适配[setSpan] 的返回值为 [Spannable], 以便进行plus操作
 */
private fun CharSequence.span(what: Any?): Spannable = setSpan(what) as Spannable

/**
 * 适配[replaceSpan] 的返回值为 [Spannable], 以便进行plus操作
 */
private fun CharSequence.spanReplace(
    regex: Regex,
    replacement: (MatchResult) -> Any?
): Spannable = replaceSpan(regex, replacement = replacement) as Spannable

private fun CharSequence.replaceRegexList(
    ruleList: List<Regex>,
    createWhat: (matchText: String) -> CharacterStyle
): Spannable? {
    var span: CharSequence? = null
    ruleList.forEach { replace ->
        span = (span ?: this).spanReplace(replace) {
            createWhat.invoke(it.value)
        }
    }
    return if (span is Spannable) span as Spannable else SpannableString.valueOf(span)
}

private fun CharSequence.replaceReplaceRuleList(
    ruleList: List<ReplaceRule>,
    createWhat: (matchText: String) -> CharacterStyle
): Spannable? {
    var span: CharSequence? = null
    ruleList.forEach { replace ->
        var currentMatchCount = 0
        span = (span ?: this).spanReplace(replace.replaceRules) {
            if (replace.matchRange == null || currentMatchCount++ in replace.matchRange) {
                replace.replacementMatch?.onMatch(it)
                val characterStyle = createWhat.invoke(it.value)
                replace.newString?.span(characterStyle) ?: characterStyle
            } else null
        }
    }

    return if (span is Spannable) span as Spannable else SpannableString.valueOf(span)
}

/**
 * [setSpan] or [replaceRule]
 */
@Suppress("UNCHECKED_CAST")
private fun CharSequence.setOrReplaceSpan(
    replaceRule: Any?,
    createWhat: (matchText: String) -> CharacterStyle
): Spannable = replaceRule?.let { rule ->

    when (rule) {
        //<editor-fold desc="String | Regex | ReplaceRule">
        is String -> spanReplace(Regex.escape(rule).toRegex()) {
            createWhat.invoke(it.value)
        }
        is Regex -> spanReplace(rule) {
            createWhat.invoke(it.value)
        }
        is ReplaceRule -> replaceReplaceRuleList(listOf(rule), createWhat)
        //</editor-fold>

        //<editor-fold desc="数组">
        is Array<*> -> if (rule.isEmpty()) {
            span(createWhat.invoke(this.toString()))
        } else {
            when (rule[0]) {
                /* String */
                is String -> replaceRegexList(
                    (rule as Array<String>).map { Regex.escape(it).toRegex() },
                    createWhat
                )
                /* 正则 */
                is Regex -> replaceRegexList(
                    (rule as Array<Regex>).toList(),
                    createWhat
                )

                /* ReplaceRule */
                is ReplaceRule -> replaceReplaceRuleList(
                    (rule as Array<ReplaceRule>).toList(),
                    createWhat
                )
                else -> throw IllegalArgumentException(UNKNOWN_REPLACE_RULES)
            }
        }
        //</editor-fold>

        //<editor-fold desc="List">
        is List<*> -> if (rule.isEmpty()) {
            span(createWhat.invoke(this.toString()))
        } else {
            when (rule[0]) {
                /* String */
                is String -> replaceRegexList(
                    (rule as List<String>).map { Regex.escape(it).toRegex() },
                    createWhat
                )
                /* 正则 */
                is Regex -> replaceRegexList(
                    (rule as List<Regex>),
                    createWhat
                )
                /* ReplaceRule */
                is ReplaceRule -> replaceReplaceRuleList(
                    (rule as List<ReplaceRule>),
                    createWhat
                )
                else -> throw IllegalArgumentException(UNKNOWN_REPLACE_RULES)
            }
        }
        //</editor-fold>
        else -> throw IllegalArgumentException(UNKNOWN_REPLACE_RULES)
    }
} ?: span(createWhat.invoke(this.toString()))

//</editor-fold>

//<editor-fold desc="Spannable 扩展 ">
/**
 * [StyleSpan] 设置文本样式
 */
internal fun CharSequence.spanStyle(
    @TextStyle style: Int,
    replaceRule: Any? = null
): Spannable = setOrReplaceSpan(replaceRule) {
    StyleSpan(style)
}

/**
 * [TypefaceSpan] 设置字体样式
 */
internal fun CharSequence.spanTypeface(
    typeface: Typeface? = null,
    family: String? = null,
    replaceRule: Any? = null
): Spannable = (if (typeface != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    TypefaceSpan(typeface)
} else TypefaceSpan(family)).let { typefaceSpan ->
    setOrReplaceSpan(replaceRule) { typefaceSpan }
}

/**
 * [TextAppearanceSpan] 设置字体效果spanTypeface
 *
 * @param replaceRule [ReplaceRule] 替换规则
 */
internal fun CharSequence.spanTextAppearance(
    @TextStyle style: Int = Typeface.NORMAL,
    size: Int = -1,
    @ColorInt color: Int? = null,
    family: String? = null,
    linkColor: ColorStateList? = null,
    replaceRule: Any? = null
): Spannable = setOrReplaceSpan(replaceRule) {
    TextAppearanceSpan(family, style, size, color?.let(ColorStateList::valueOf), linkColor)
}

/**
 * [ForegroundColorSpan] 文本颜色
 *
 * @param replaceRule [ReplaceRule] 替换规则
 */
internal fun CharSequence.spanColor(
    colorString: String,
    replaceRule: Any? = null
): Spannable = setOrReplaceSpan(replaceRule) {
    ForegroundColorSpan(colorString.color)
}

/**
 * [ForegroundColorSpan] 文本颜色
 *
 * @param replaceRule [ReplaceRule] 替换规则
 */
internal fun CharSequence.spanColor(
    @ColorInt color: Int,
    replaceRule: Any? = null
): Spannable = setOrReplaceSpan(replaceRule) {
    ForegroundColorSpan(color)
}

/**
 * [BackgroundColorSpan] 背景颜色
 *
 * @param replaceRule [ReplaceRule] 替换规则
 */
internal fun CharSequence.spanBackground(
    colorString: String,
    replaceRule: Any? = null
): Spannable = setOrReplaceSpan(replaceRule) {
    BackgroundColorSpan(Color.parseColor(colorString))
}

/**
 * [BackgroundColorSpan] 背景颜色
 *
 * @param replaceRule [ReplaceRule] 替换规则
 */
internal fun CharSequence.spanBackground(
    @ColorInt color: Int,
    replaceRule: Any? = null
): Spannable = setOrReplaceSpan(replaceRule) {
    BackgroundColorSpan(color)
}

/**
 * [SizeImageSpan] 图片
 */
internal fun spanImage(
    drawable: Drawable,
    source: String? = null,
    useTextViewSize: TextView? = null,
    size: DrawableSize? = null
): Spannable = IMAGE_SPAN_TAG.spanImage(drawable, source, useTextViewSize, size)

/**
 * [SizeImageSpan] 图片
 *
 * @param replaceRule [ReplaceRule] 替换规则
 */
internal fun CharSequence.spanImage(
    drawable: Drawable,
    source: String? = null,
    useTextViewSize: TextView? = null,
    size: DrawableSize? = null,
    replaceRule: Any? = null
): Spannable = setOrReplaceSpan(replaceRule) {
    (source?.let {
        SizeImageSpan(drawable, it)
    } ?: SizeImageSpan(drawable)).setupSize(useTextViewSize, size)
}

/**
 * [SizeImageSpan] 图片
 */
internal fun spanImage(
    context: Context,
    uri: Uri,
    useTextViewSize: TextView? = null,
    size: DrawableSize? = null,
): Spannable = IMAGE_SPAN_TAG.spanImage(context, uri, useTextViewSize, size)

/**
 * [SizeImageSpan] 图片
 *
 * @param replaceRule [ReplaceRule] 替换规则
 */
internal fun CharSequence.spanImage(
    context: Context,
    uri: Uri,
    useTextViewSize: TextView? = null,
    size: DrawableSize? = null,
    replaceRule: Any? = null
): Spannable = setOrReplaceSpan(replaceRule) {
    SizeImageSpan(context, uri).setupSize(useTextViewSize, size)
}

/**
 * [SizeImageSpan] 图片
 */
internal fun spanImage(
    context: Context,
    @DrawableRes resourceId: Int,
    useTextViewSize: TextView? = null,
    size: DrawableSize? = null,
): Spannable = IMAGE_SPAN_TAG.spanImage(context, resourceId, useTextViewSize, size)

/**
 * [SizeImageSpan] 图片
 *
 * @param replaceRule [ReplaceRule] 替换规则
 */
internal fun CharSequence.spanImage(
    context: Context,
    @DrawableRes resourceId: Int,
    useTextViewSize: TextView? = null,
    size: DrawableSize? = null,
    replaceRule: Any? = null,
): Spannable = setOrReplaceSpan(replaceRule) {
    SizeImageSpan(context, resourceId).setupSize(useTextViewSize, size)
}

/**
 * [SizeImageSpan] 图片
 */
internal fun spanImage(
    context: Context,
    bitmap: Bitmap,
    useTextViewSize: TextView? = null,
    size: DrawableSize? = null,
): Spannable = IMAGE_SPAN_TAG.spanImage(context, bitmap, useTextViewSize, size)

/**
 * [SizeImageSpan] 图片
 *
 * @param replaceRule [ReplaceRule] 替换规则
 */
internal fun CharSequence.spanImage(
    context: Context,
    bitmap: Bitmap,
    useTextViewSize: TextView? = null,
    size: DrawableSize? = null,
    replaceRule: Any? = null,
): Spannable = setOrReplaceSpan(replaceRule) {
    SizeImageSpan(context, bitmap).setupSize(useTextViewSize, size)
}

/**
 * [ScaleXSpan] X轴文本缩放
 *
 * @param replaceRule [ReplaceRule] 替换规则
 */
internal fun CharSequence.spanScaleX(
    @FloatRange(from = 0.0) proportion: Float,
    replaceRule: Any? = null
): Spannable = setOrReplaceSpan(replaceRule) {
    ScaleXSpan(proportion)
}

/**
 * [MaskFilterSpan] 设置文本蒙版效果
 *
 * @param replaceRule [ReplaceRule] 替换规则
 */
internal fun CharSequence.spanMaskFilter(
    filter: MaskFilter,
    replaceRule: Any? = null
): Spannable = setOrReplaceSpan(replaceRule) {
    MaskFilterSpan(filter)
}

/**
 * [BlurMaskFilter] 设置文本模糊滤镜蒙版效果
 *
 * @param replaceRule [ReplaceRule] 替换规则
 */
internal fun CharSequence.spanBlurMask(
    @FloatRange(from = 0.0) radius: Float,
    style: BlurMaskFilter.Blur? = null,
    replaceRule: Any? = null
): Spannable =
    spanMaskFilter(BlurMaskFilter(radius, style ?: BlurMaskFilter.Blur.NORMAL), replaceRule)

/**
 * [SuperscriptSpan] 设置文本为上标
 *
 * @param replaceRule [ReplaceRule] 替换规则
 */
internal fun CharSequence.spanSuperscript(
    replaceRule: Any? = null
): Spannable = setOrReplaceSpan(replaceRule) {
    SuperscriptSpan()
}

/**
 * [SubscriptSpan] 设置文本为下标
 *
 * @param replaceRule [ReplaceRule] 替换规则
 */
internal fun CharSequence.spanSubscript(
    replaceRule: Any? = null
): Spannable = setOrReplaceSpan(replaceRule) {
    SubscriptSpan()
}

/**
 * [AbsoluteSizeSpan] 设置文本绝对大小
 *
 * @param replaceRule [ReplaceRule] 替换规则
 */
internal fun CharSequence.spanAbsoluteSize(
    size: Int,
    dip: Boolean = true,
    replaceRule: Any? = null
): Spannable = setOrReplaceSpan(replaceRule) {
    AbsoluteSizeSpan(size, dip)
}

/**
 * [RelativeSizeSpan] 设置文本相对大小
 *
 * @param replaceRule [ReplaceRule] 替换规则
 */
internal fun CharSequence.spanRelativeSize(
    @FloatRange(from = 0.0) proportion: Float,
    replaceRule: Any? = null
): Spannable = setOrReplaceSpan(replaceRule) {
    RelativeSizeSpan(proportion)
}

/**
 * [StrikethroughSpan] 设置文本删除线
 *
 * @param replaceRule [ReplaceRule] 替换规则
 */
internal fun CharSequence.spanStrikethrough(
    replaceRule: Any? = null
): Spannable = setOrReplaceSpan(replaceRule) {
    StrikethroughSpan()
}

/**
 * [UnderlineSpan] 设置文本下划线
 *
 * @param replaceRule [ReplaceRule] 替换规则
 */
internal fun CharSequence.spanUnderline(
    replaceRule: Any? = null
): Spannable = setOrReplaceSpan(replaceRule) {
    UnderlineSpan()
}

/**
 * [URLSpan] 设置文本超链接
 * 配合[TextView.activateClick]使用
 *
 * @param replaceRule [ReplaceRule] 替换规则
 */
internal fun CharSequence.spanURL(
    url: String,
    replaceRule: Any? = null
): Spannable = setOrReplaceSpan(replaceRule) {
    URLSpan(url)
}

/**
 * [SuggestionSpan] 设置文本输入提示
 *
 * @param replaceRule [ReplaceRule] 替换规则
 */
internal fun CharSequence.spanSuggestion(
    context: Context,
    suggestions: Array<String>,
    flags: Int = SuggestionSpan.SUGGESTIONS_MAX_SIZE,
    locale: Locale? = null,
    notificationTargetClass: Class<*>? = null,
    replaceRule: Any? = null
): Spannable = setOrReplaceSpan(replaceRule) {
    SuggestionSpan(context, locale, suggestions, flags, notificationTargetClass)
}

/**
 * [SimpleClickableSpan] 设置文本点击效果
 *
 * @param replaceRule [ReplaceRule] 替换规则
 * @param onClick [OnSpanClickListener] 点击回调
 */
internal fun CharSequence.spanClickable(
    @ColorInt color: Int? = null,
    @ColorInt backgroundColor: Int? = null,
    @TextStyle typeStyle: Int? = null,
    config: SimpleClickableConfig? = null,
    replaceRule: Any? = null,
    onClick: OnSpanClickListener? = null
): Spannable = setOrReplaceSpan(replaceRule) { matchText ->
    SimpleClickableSpan(color, backgroundColor, typeStyle, config) {
        onClick?.onClick(it, matchText)
    }
}

//</editor-fold>