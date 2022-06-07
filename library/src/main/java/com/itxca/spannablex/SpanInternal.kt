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
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.MaskFilter
import android.graphics.Typeface
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
 * 这里多做判断，是防止[GlideImageSpan.setMarginHorizontal] 做多余的`drawableRef?.set(null)`
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

/**
 * 正则 [Regex] 列表替换
 */
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

/**
 * 组合替换规则 [ReplaceRule] 列表替换
 */
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
 *
 * @param style 文本样式 [Typeface.NORMAL] [Typeface.BOLD] [Typeface.ITALIC] [Typeface.BOLD_ITALIC]
 * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
 */
internal fun CharSequence.spanStyle(
    @TextStyle style: Int,
    replaceRule: Any?
): Spannable = setOrReplaceSpan(replaceRule) {
    StyleSpan(style)
}

/**
 * [TypefaceSpan] 设置字体样式
 *
 * @param typeface 字体(API>=28)
 * @param family 字体集
 * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
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
 * @param style 文本样式 [Typeface.NORMAL] [Typeface.BOLD] [Typeface.ITALIC] [Typeface.BOLD_ITALIC]
 * @param size 文本大小
 * @param color 文本颜色
 * @param family 字体集
 * @param linkColor 链接颜色
 * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
 */
internal fun CharSequence.spanTextAppearance(
    @TextStyle style: Int = Typeface.NORMAL,
    @Px size: Int = -1,
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
 * @param color 文本颜色
 * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
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
 * @param color 背景颜色
 * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
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
 * @param drawable [Drawable]
 * @param source [Drawable] Uri
 * @param useTextViewSize 图片使用指定的[TextView]文本大小，与参数[size]冲突，优先使用[useTextViewSize]
 * @param size 图片大小 [DrawableSize]
 * @param marginLeft 图片左边距
 * @param marginRight 图片右边距
 * @param align 图片对齐方式 [CenterImageSpan.Align.CENTER] [CenterImageSpan.Align.BOTTOM] [CenterImageSpan.Align.BASELINE]
 * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
 */
internal fun CharSequence.spanImage(
    drawable: Drawable,
    source: String?,
    useTextViewSize: TextView?,
    size: DrawableSize?,
    @Px marginLeft: Int?,
    @Px marginRight: Int?,
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
 * @param context [Context]
 * @param uri 图片 Uri
 * @param useTextViewSize 图片使用指定的[TextView]文本大小，与参数[size]冲突，优先使用[useTextViewSize]
 * @param size 图片大小 [DrawableSize]
 * @param marginLeft 图片左边距
 * @param marginRight 图片右边距
 * @param align 图片对齐方式 [CenterImageSpan.Align.CENTER] [CenterImageSpan.Align.BOTTOM] [CenterImageSpan.Align.BASELINE]
 * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
 */
internal fun CharSequence.spanImage(
    context: Context,
    uri: Uri,
    useTextViewSize: TextView?,
    size: DrawableSize?,
    @Px marginLeft: Int?,
    @Px marginRight: Int?,
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
 * @param context [Context]
 * @param resourceId 图片Id
 * @param useTextViewSize 图片使用指定的[TextView]文本大小，与参数[size]冲突，优先使用[useTextViewSize]
 * @param size 图片大小 [DrawableSize]
 * @param marginLeft 图片左边距
 * @param marginRight 图片右边距
 * @param align 图片对齐方式 [CenterImageSpan.Align.CENTER] [CenterImageSpan.Align.BOTTOM] [CenterImageSpan.Align.BASELINE]
 * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
 */
internal fun CharSequence.spanImage(
    context: Context,
    @DrawableRes resourceId: Int,
    useTextViewSize: TextView?,
    size: DrawableSize?,
    @Px marginLeft: Int?,
    @Px marginRight: Int?,
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
 * @param context [Context]
 * @param bitmap [Bitmap]
 * @param useTextViewSize 图片使用指定的[TextView]文本大小，与参数[size]冲突，优先使用[useTextViewSize]
 * @param size 图片大小 [DrawableSize]
 * @param marginLeft 图片左边距
 * @param marginRight 图片右边距
 * @param align 图片对齐方式 [CenterImageSpan.Align.CENTER] [CenterImageSpan.Align.BOTTOM] [CenterImageSpan.Align.BASELINE]
 * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
 */
internal fun CharSequence.spanImage(
    context: Context,
    bitmap: Bitmap,
    useTextViewSize: TextView?,
    size: DrawableSize?,
    @Px marginLeft: Int?,
    @Px marginRight: Int?,
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
 * @param view 当前Span所在的[TextView], 用于异步加载完图片后通知[TextView]刷新
 * @param url 图片地址参见 [Glide.with(view).load(url)]
 * @param useTextViewSize 图片使用指定的[TextView]文本大小，与参数[size]冲突，优先使用[useTextViewSize]
 * @param size 图片大小 [DrawableSize]
 * @param marginLeft 图片左边距
 * @param marginRight 图片右边距
 * @param align 图片对齐方式 [CenterImageSpan.Align.CENTER] [CenterImageSpan.Align.BOTTOM] [CenterImageSpan.Align.BASELINE]
 * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
 */
internal fun CharSequence.spanGlide(
    view: TextView,
    url: Any,
    useTextViewSize: TextView?,
    size: DrawableSize?,
    @Px marginLeft: Int?,
    @Px marginRight: Int?,
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
 * @param proportion 水平(X轴)缩放比例
 * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
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
 * @param filter 蒙版效果 [MaskFilter]
 * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
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
 * @param radius 模糊半径
 * @param style 模糊效果 [BlurMaskFilter.Blur]
 * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
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
 * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
 */
internal fun CharSequence.spanSuperscript(
    replaceRule: Any?
): Spannable = setOrReplaceSpan(replaceRule) {
    SuperscriptSpan()
}

/**
 * [SubscriptSpan] 设置文本为下标
 *
 * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
 */
internal fun CharSequence.spanSubscript(
    replaceRule: Any?
): Spannable = setOrReplaceSpan(replaceRule) {
    SubscriptSpan()
}

/**
 * [AbsoluteSizeSpan] 设置文本绝对大小
 *
 * @param size 文本大小
 * @param dp true = [size] dp, false = [size] px
 * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
 */
internal fun CharSequence.spanAbsoluteSize(
    size: Int,
    dp: Boolean,
    replaceRule: Any?
): Spannable = setOrReplaceSpan(replaceRule) {
    AbsoluteSizeSpan(size, dp)
}

/**
 * [RelativeSizeSpan] 设置文本相对大小
 *
 * @param proportion 文本缩放比例
 * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
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
 * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
 */
internal fun CharSequence.spanStrikethrough(
    replaceRule: Any?
): Spannable = setOrReplaceSpan(replaceRule) {
    StrikethroughSpan()
}

/**
 * [UnderlineSpan] 设置文本下划线
 *
 * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
 */
internal fun CharSequence.spanUnderline(
    replaceRule: Any?
): Spannable = setOrReplaceSpan(replaceRule) {
    UnderlineSpan()
}

/**
 * [URLSpan] 设置文本超链接
 *
 * 需配合[TextView.activateClick]使用
 * @param url 超链接地址
 * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
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
 * @param context [Context]
 * @param suggestions 提示规则文本数组
 * @param flags 提示规则 [SuggestionSpan.FLAG_EASY_CORRECT] [SuggestionSpan.FLAG_MISSPELLED] [SuggestionSpan.FLAG_AUTO_CORRECTION]
 * @param locale 语言区域设置
 * @param notificationTargetClass 通知目标. 基本已废弃, 只在API<29时生效
 * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
 */
internal fun CharSequence.spanSuggestion(
    context: Context,
    suggestions: Array<String>,
    flags: Int,
    locale: Locale?,
    notificationTargetClass: Class<*>?,
    replaceRule: Any?
): Spannable = setOrReplaceSpan(replaceRule) {
    SuggestionSpan(context, locale, suggestions, flags, notificationTargetClass)
}

/**
 * [SimpleClickableSpan] 设置文本点击效果
 *
 * @param color 文本颜色
 * @param backgroundColor 背景颜色
 * @param style 文本样式 [Typeface.NORMAL] [Typeface.BOLD] [Typeface.ITALIC] [Typeface.BOLD_ITALIC]
 * @param config 附加配置 [SimpleClickableConfig]
 * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
 * @param onClick [OnSpanClickListener] 点击回调
 */
internal fun CharSequence.spanClickable(
    @ColorInt color: Int?,
    @ColorInt backgroundColor: Int?,
    @TextStyle style: Int?,
    config: SimpleClickableConfig?,
    replaceRule: Any?,
    onClick: OnSpanClickListener?
): Spannable = setOrReplaceSpan(replaceRule) { matchText ->
    SimpleClickableSpan(color, backgroundColor, style, config) {
        onClick?.onClick(it, matchText)
    }
}

/**
 * [QuoteSpan] 设置段落引用样式(段落前竖线标识)
 *
 * [ParagraphStyle] 段落Style不支持文本替换
 * @param color 竖线颜色
 * @param stripeWidth 竖线宽度
 * @param gapWidth 竖线与文本之间间隔宽度
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
 * [BulletSpan] 设置段落项目符号(段落前圆形标识)
 *
 * [ParagraphStyle] 段落Style不支持文本替换
 * @param color 圆形颜色
 * @param bulletRadius 圆形半径
 * @param gapWidth 竖线与文本之间间隔宽度
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
 *
 * [ParagraphStyle] 段落Style不支持文本替换
 * @param align [Layout.Alignment.ALIGN_NORMAL] [Layout.Alignment.ALIGN_CENTER] [Layout.Alignment.ALIGN_OPPOSITE]
 */
internal fun CharSequence.spanAlignment(
    align: Layout.Alignment
): Spannable = setOrReplaceSpan(null) {
    AlignmentSpan.Standard(align)
}

/**
 * [LineBackgroundSpan] 设置段落背景颜色
 *
 * [ParagraphStyle] 段落Style不支持文本替换
 * @param color 背景颜色
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
 *
 * [ParagraphStyle] 段落Style不支持文本替换
 * @param firstLines 首行行数. 与[firstMargin]关联
 * @param firstMargin 首行左边距(缩进)
 * @param restMargin 剩余行(非首行)左边距(缩进)
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
 *
 * [ParagraphStyle] 段落Style不支持文本替换
 * @param height 行高
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
 *
 * [ParagraphStyle] 段落Style不支持文本替换
 * @param bitmap [Bitmap]
 * @param padding 图片与文本的间距
 * @param useTextViewSize 图片使用指定的[TextView]文本大小，与参数[size]冲突，优先使用[useTextViewSize]
 * @param size 图片大小 [DrawableSize]
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
 *
 * [ParagraphStyle] 段落Style不支持文本替换
 * @param drawable [Drawable]
 * @param padding 图片与文本的间距
 * @param useTextViewSize 图片使用指定的[TextView]文本大小，与参数[size]冲突，优先使用[useTextViewSize]
 * @param size 图片大小 [DrawableSize]
 */
internal fun CharSequence.spanImageParagraph(
    drawable: Drawable,
    @Px padding: Int,
    useTextViewSize: TextView?,
    size: DrawableSize?
): Spannable = setOrReplaceSpan(null) {
    ParagraphDrawableSpan(drawable, useTextViewSize?.textSizeInt?.drawableSize ?: size, padding)
}

/**
 * 自定义字符样式
 *
 * @param style 自定义样式. eg. spanCustom(ForegroundColorSpan(Color.RED))
 * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
 */
internal fun <T : CharacterStyle> CharSequence.spanCustom(
    style: T,
    replaceRule: Any?
): Spannable = setOrReplaceSpan(replaceRule) {
    style
}

/**
 * 自定义段落样式
 *
 * @param style 自定义样式. eg. spanCustom(LineBackgroundSpan.Standard(Color.Red))
 * @param replaceRule 组合替换规则 [String] [Regex] [ReplaceRule]
 * 由于段落样式的特殊性, [ParagraphStyle] 段落样式下 [replaceRule] 大部分情况并不会生效
 */
internal fun <T : ParagraphStyle> CharSequence.spanCustom(
    style: T,
    replaceRule: Any?
): Spannable = setOrReplaceSpan(replaceRule) {
    style
}

//</editor-fold>