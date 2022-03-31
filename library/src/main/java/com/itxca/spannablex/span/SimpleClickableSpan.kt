package com.itxca.spannablex.span

import android.graphics.Color
import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import androidx.annotation.ColorInt
import com.itxca.spannablex.annotation.TextStyle


typealias OnSimpleClickListener = (widget: View) -> Unit

data class SimpleClickableConfig(

    /**
     * 下划线
     */
    val underline: Boolean? = null,
)

class SimpleClickableSpan(
    @ColorInt private val color: Int? = null,
    @ColorInt private val backgroundColor: Int? = null,
    @TextStyle private val typeStyle: Int? = null,
    private val config: SimpleClickableConfig? = null,
    private val onClick: OnSimpleClickListener? = null
) : ClickableSpan() {

    constructor(
        colorString: String?,
        backgroundColorString: String?,
        @TextStyle typeStyle: Int? = null,
        config: SimpleClickableConfig? = null,
        onClick: OnSimpleClickListener? = null
    ) : this(
        colorString?.let(Color::parseColor),
        backgroundColorString?.let(Color::parseColor),
        typeStyle,
        config,
        onClick
    )

    override fun updateDrawState(ds: TextPaint) {
        color?.let(ds::setColor)
        backgroundColor?.let { ds.bgColor = backgroundColor }
        typeStyle?.let(Typeface::defaultFromStyle)?.let(ds::setTypeface)

        config?.run {
            underline?.let(ds::setUnderlineText)
        }
    }

    override fun onClick(widget: View) {
        onClick?.invoke(widget)
    }
}