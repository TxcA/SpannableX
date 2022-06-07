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
import android.text.Layout
import android.text.Spannable
import android.text.SpannableString
import android.text.style.*
import android.util.Log
import android.widget.TextView
import androidx.annotation.*
import androidx.annotation.IntRange
import com.bumptech.glide.request.RequestOptions
import com.drake.spannable.replaceSpan
import com.drake.spannable.setSpan
import com.drake.spannable.span.CenterImageSpan
import com.drake.spannable.span.GlideImageSpan
import com.itxca.spannablex.annotation.TextStyle
import com.itxca.spannablex.interfaces.OnSpanClickListener
import com.itxca.spannablex.span.*
import com.itxca.spannablex.span.LeadingMarginSpan
import com.itxca.spannablex.span.legacy.LegacyBulletSpan
import com.itxca.spannablex.span.legacy.LegacyLineBackgroundSpan
import com.itxca.spannablex.span.legacy.LegacyLineHeightSpan
import com.itxca.spannablex.span.legacy.LegacyQuoteSpan
import com.itxca.spannablex.utils.DrawableSize
import com.itxca.spannablex.utils.color
import com.itxca.spannablex.utils.drawableSize
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
 * [CenterImageSpan] 适配 [Drawable] size
 */
private fun CenterImageSpan.setupSize(
    useTextViewSize: TextView?,
    size: DrawableSize?
): CenterImageSpan = apply {
    useTextViewSize?.textSizeInt?.let { textSize ->
        setDrawableSize(textSize, textSize)
    } ?: size?.let { drawableSize ->
        setDrawableSize(drawableSize.width, drawableSize.height)
    }
}

/**
 * [CenterImageSpan] 适配 [Drawable] margin
 * 这里多做判断，是防止[CenterImageSpan.setMarginHorizontal] 做多余的`drawableRef?.clear()`
 */
private fun CenterImageSpan.setupMarginHorizontal(
    left: Int?,
    right: Int?
): CenterImageSpan = apply {
    if (left != null || right != null) {
        setMarginHorizontal(left ?: 0, right ?: 0)
    }
}

/**
 * [GlideImageSpan] 适配 [Drawable] size
 */
private fun GlideImageSpan.setupSize(
    useTextViewSize: TextView?,
    size: DrawableSize?
): GlideImageSpan = apply {
    useTextViewSize?.textSizeInt?.let { textSize ->
        setDrawableSize(textSize, textSize)
    } ?: size?.let { drawableSize ->
        setDrawableSize(drawableSize.width, drawableSize.height)
    }
}

/**
 * [GlideImageSpan] 适配 [Drawable] margin
 * 这里多做判断，是防止[CenterImageSpan.setMarginHorizontal] 做多余的`drawableRef?.set(null)`
 */
private fun GlideImageSpan.setupMarginHorizontal(
    left: Int?,
    right: Int?
): GlideImageSpan = apply {
    if (left != null || right != null) {
        setMarginHorizontal(left ?: 0, right ?: 0)
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
    createWhat: (matchText: String) -> Any
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
    createWhat: (matchText: String) -> Any
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
    createWhat: (matchText: String) -> Any
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
    replaceRule: Any?
): Spannable = setOrReplaceSpan(replaceRule) {
    StyleSpan(style)
}

/**
 * [TypefaceSpan] 设置字体样式
 */
internal fun CharSequence.spanTypeface(
    typeface: Typeface?,
    family: String?,
    replaceRule: Any?
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
    @ColorInt color: Int?,
    family: String?,
    linkColor: ColorStateList?,
    replaceRule: Any?
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
    replaceRule: Any?
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
    replaceRule: Any?
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
    replaceRule: Any?
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
    replaceRule: Any?
): Spannable = setOrReplaceSpan(replaceRule) {
    BackgroundColorSpan(color)
}

/**
 * [CenterImageSpan] 图片
 *
 * @param replaceRule [ReplaceRule] 替换规则
 */
internal fun CharSequence.spanImage(
    drawable: Drawable,
    source: String?,
    useTextViewSize: TextView?,
    size: DrawableSize?,
    marginLeft: Int?,
    marginRight: Int?,
    align: CenterImageSpan.Align,
    replaceRule: Any?,
): Spannable = setOrReplaceSpan(replaceRule) {
    (source?.let {
        CenterImageSpan(drawable, it)
    } ?: CenterImageSpan(drawable)).setupSize(useTextViewSize, size)
        .setupMarginHorizontal(marginLeft, marginRight)
        .setAlign(align)
}

/**
 * [CenterImageSpan] 图片
 *
 * @param replaceRule [ReplaceRule] 替换规则
 */
internal fun CharSequence.spanImage(
    context: Context,
    uri: Uri,
    useTextViewSize: TextView?,
    size: DrawableSize?,
    marginLeft: Int?,
    marginRight: Int?,
    align: CenterImageSpan.Align,
    replaceRule: Any?
): Spannable = setOrReplaceSpan(replaceRule) {
    CenterImageSpan(context, uri).setupSize(useTextViewSize, size)
        .setupMarginHorizontal(marginLeft, marginRight)
        .setAlign(align)
}

/**
 * [CenterImageSpan] 图片
 *
 * @param replaceRule [ReplaceRule] 替换规则
 */
internal fun CharSequence.spanImage(
    context: Context,
    @DrawableRes resourceId: Int,
    useTextViewSize: TextView?,
    size: DrawableSize?,
    marginLeft: Int?,
    marginRight: Int?,
    align: CenterImageSpan.Align,
    replaceRule: Any?,
): Spannable = setOrReplaceSpan(replaceRule) {
    CenterImageSpan(context, resourceId).setupSize(useTextViewSize, size)
        .setupMarginHorizontal(marginLeft, marginRight)
        .setAlign(align)
}

/**
 * [CenterImageSpan] 图片
 *
 * @param replaceRule [ReplaceRule] 替换规则
 */
internal fun CharSequence.spanImage(
    context: Context,
    bitmap: Bitmap,
    useTextViewSize: TextView?,
    size: DrawableSize?,
    marginLeft: Int?,
    marginRight: Int?,
    align: CenterImageSpan.Align,
    replaceRule: Any?,
): Spannable = setOrReplaceSpan(replaceRule) {
    CenterImageSpan(context, bitmap).setupSize(useTextViewSize, size)
        .setupMarginHorizontal(marginLeft, marginRight)
        .setAlign(align)
}

/**
 * [GlideImageSpan] 图片
 *
 * @param replaceRule [ReplaceRule] 替换规则
 */
internal fun CharSequence.spanGlide(
    view: TextView,
    url: Any,
    useTextViewSize: TextView?,
    size: DrawableSize?,
    marginLeft: Int?,
    marginRight: Int?,
    align: GlideImageSpan.Align,
    loopCount: Int?,
    requestOption: RequestOptions?,
    replaceRule: Any?,
): Spannable = setOrReplaceSpan(replaceRule) {
    GlideImageSpan(view, url).setupSize(useTextViewSize, size)
        .setupMarginHorizontal(marginLeft, marginRight)
        .setAlign(align)
        .apply {
            loopCount?.let(::setLoopCount)
            requestOption?.let(::setRequestOption)
        }
}

/**
 * [ScaleXSpan] X轴文本缩放
 *
 * @param replaceRule [ReplaceRule] 替换规则
 */
internal fun CharSequence.spanScaleX(
    @FloatRange(from = 0.0) proportion: Float,
    replaceRule: Any?
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
    replaceRule: Any?
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
    style: BlurMaskFilter.Blur?,
    replaceRule: Any?
): Spannable =
    spanMaskFilter(BlurMaskFilter(radius, style ?: BlurMaskFilter.Blur.NORMAL), replaceRule)

/**
 * [SuperscriptSpan] 设置文本为上标
 *
 * @param replaceRule [ReplaceRule] 替换规则
 */
internal fun CharSequence.spanSuperscript(
    replaceRule: Any?
): Spannable = setOrReplaceSpan(replaceRule) {
    SuperscriptSpan()
}

/**
 * [SubscriptSpan] 设置文本为下标
 *
 * @param replaceRule [ReplaceRule] 替换规则
 */
internal fun CharSequence.spanSubscript(
    replaceRule: Any?
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
    replaceRule: Any?
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
    replaceRule: Any?
): Spannable = setOrReplaceSpan(replaceRule) {
    RelativeSizeSpan(proportion)
}

/**
 * [StrikethroughSpan] 设置文本删除线
 *
 * @param replaceRule [ReplaceRule] 替换规则
 */
internal fun CharSequence.spanStrikethrough(
    replaceRule: Any?
): Spannable = setOrReplaceSpan(replaceRule) {
    StrikethroughSpan()
}

/**
 * [UnderlineSpan] 设置文本下划线
 *
 * @param replaceRule [ReplaceRule] 替换规则
 */
internal fun CharSequence.spanUnderline(
    replaceRule: Any?
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
    replaceRule: Any?
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
    locale: Locale?,
    notificationTargetClass: Class<*>?,
    replaceRule: Any?
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
    @ColorInt color: Int?,
    @ColorInt backgroundColor: Int?,
    @TextStyle typeStyle: Int?,
    config: SimpleClickableConfig?,
    replaceRule: Any?,
    onClick: OnSpanClickListener?
): Spannable = setOrReplaceSpan(replaceRule) { matchText ->
    SimpleClickableSpan(color, backgroundColor, typeStyle, config) {
        onClick?.onClick(it, matchText)
    }
}

/**
 * [QuoteSpan] 设置段落引用样式
 */
internal fun CharSequence.spanQuote(
    @ColorInt color: Int,
    @Px @IntRange(from = 0) stripeWidth: Int,
    @Px @IntRange(from = 0) gapWidth: Int,
): Spannable = setOrReplaceSpan(null) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        QuoteSpan(color, stripeWidth, gapWidth)
    } else {
        LegacyQuoteSpan(color, stripeWidth, gapWidth)
    }
}

/**
 * [BulletSpan] 设置段落圆形标识
 */
internal fun CharSequence.spanBullet(
    @ColorInt color: Int,
    @Px @IntRange(from = 0) bulletRadius: Int,
    @Px gapWidth: Int,
): Spannable = setOrReplaceSpan(null) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        BulletSpan(gapWidth, color, bulletRadius)
    } else {
        LegacyBulletSpan(color, bulletRadius, gapWidth)
    }
}

/**
 * [AlignmentSpan] 设置段落对齐方式
 */
internal fun CharSequence.spanAlignment(
    align: Layout.Alignment
): Spannable = setOrReplaceSpan(null) {
    AlignmentSpan.Standard(align)
}

/**
 * [LineBackgroundSpan] 设置段落背景颜色
 */
internal fun CharSequence.spanLineBackground(
    @ColorInt color: Int
): Spannable = setOrReplaceSpan(null) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        LineBackgroundSpan.Standard(color)
    } else {
        LegacyLineBackgroundSpan(color)
    }
}

/**
 * [LeadingMarginSpan] 设置段落文本缩进
 */
internal fun CharSequence.spanLeadingMargin(
    @IntRange(from = 1L) firstLines: Int,
    @Px firstMargin: Int,
    @Px restMargin: Int
): Spannable = setOrReplaceSpan(null) {
    LeadingMarginSpan(firstLines, firstMargin, restMargin)
}

/**
 * [LineHeightSpan] 设置段落行高
 */
internal fun CharSequence.spanLineHeight(
    @Px @IntRange(from = 1L) height: Int
): Spannable = setOrReplaceSpan(null) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        LineHeightSpan.Standard(height)
    } else {
        LegacyLineHeightSpan(height)
    }
}

/**
 * [ParagraphBitmapSpan] 设置段落图片
 */
internal fun CharSequence.spanImageParagraph(
    bitmap: Bitmap,
    @Px padding: Int,
    useTextViewSize: TextView?,
    size: DrawableSize?
): Spannable = setOrReplaceSpan(null) {
    ParagraphBitmapSpan(bitmap, useTextViewSize?.textSizeInt?.drawableSize ?: size, padding)
}

/**
 * [ParagraphDrawableSpan] 设置段落图片
 */
internal fun CharSequence.spanImageParagraph(
    drawable: Drawable,
    @Px padding: Int,
    useTextViewSize: TextView?,
    size: DrawableSize?
): Spannable = setOrReplaceSpan(null) {
    ParagraphDrawableSpan(drawable, useTextViewSize?.textSizeInt?.drawableSize ?: size, padding)
}
//</editor-fold>