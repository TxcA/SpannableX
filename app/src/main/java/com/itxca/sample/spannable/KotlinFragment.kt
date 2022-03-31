package com.itxca.sample.spannable

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.style.SuggestionSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.itxca.sample.spannable.databinding.SampleFragmentBinding
import com.itxca.spannablex.*
import com.itxca.spannablex.utils.color
import com.itxca.spannablex.utils.drawableSize
import com.itxca.spannablex.utils.sp


class KotlinFragment : Fragment() {

    private lateinit var viewBinding: SampleFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = SampleFragmentBinding.inflate(inflater, container, false).apply {
        viewBinding = this
    }.root

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.tvTitle.text = "x SpannableX".spannable {
            relativeSize(1.2f)
            style(Typeface.BOLD)

            image(requireContext(), R.mipmap.ic_launcher, viewBinding.tvTitle, replaceRule = "x")
            color("#ff0000", "S")
            color("#ffa500", "p")
            color("#ffff00", "a".toReplaceRule(matchIndex = 0))
            color("#00ff00", "n".toReplaceRule(matchIndex = 0))
            color("#00f7ff", "n".toReplaceRule(matchIndex = 1))
            color("#0000ff", "a".toReplaceRule(matchIndex = 1))
            color("#8b00ff", "b")
            color("#ff0000", "l")
            color("#ffa500", "e")
            color("#ffff00", "X")
        }

        viewBinding.tvMixed.activateClick().text = "all spannable mixed".spannable {
            style(Typeface.ITALIC, "spannable")
            typeface(family = "serif", replaceRule = arrayOf("all", "mix"))
            color("#da4f49".color, "all")
            color("#faa732".color, "spannable")
            color("#0088CC".color, "mixed")
            background("#eaeaea".color)
            scaleX(1.1f, "spannable")
            blurMask(1.1f, replaceRule = "spannable")
            superscript("all")
            subscript("mixed")
            absoluteSize(12, true)
            relativeSize(1.2f)
            strikethrough("all")
            underline("spannable")
            clickable { _, matchText ->
                toast("点击: $matchText")
            }
        }

        viewBinding.tvSample.activateClick(true).text = "TextView.text  = spannable {".spannable {
            style(Typeface.ITALIC)
            style(Typeface.BOLD, "spannable {")
            color(Color.BLACK, "spannable {")

            newline()
            "       style".newline().style(Typeface.BOLD or Typeface.ITALIC)
            "       typeface".newline().typeface(
                Typeface.createFromAsset(requireContext().assets, "Inconsolata-Regular.ttf"),
                replaceRule = unSpaceRegex
            )
            "       textAppearance".newline().textAppearance(
                style = Typeface.ITALIC,
                size = 14.sp,
                color = Color.RED,
                family = "serif",
                linkColor = ColorStateList.valueOf(Color.BLUE),
                replaceRule = unSpaceRegex
            )
            "       color".newline().color(
                ContextCompat.getColor(requireContext(), R.color.colorPrimary)
            )
            "       background".newline().background(
                ContextCompat.getColor(requireContext(), R.color.colorPrimaryVariant), unSpaceRegex
            )

            "       image x".image(
                requireContext(), R.mipmap.ic_launcher, viewBinding.tvSample, replaceRule = "x"
            )
            image(requireContext(), R.mipmap.ic_launcher, size = 18.sp.drawableSize)
            image(requireContext(), R.mipmap.ic_launcher, size = 24.sp.drawableSize)
            newline()
            "       scaleX".newline().scaleX(2.0f, "X".toReplaceRule(matchIndex = 0))
            "       blurMask".newline().blurMask(5.0f)
            "       superscript > Top".newline().superscript("Top")
            "       subscript > Bottom".newline().subscript("Bottom")
            "       absoluteSize".newline().absoluteSize(11, true, unSpaceRegex)
            "       relativeSize".newline().relativeSize(1.5f, unSpaceRegex)
            "       strikethrough".newline().strikethrough(unSpaceRegex)
            "       underline".newline().underline(unSpaceRegex)
            "       url".newline().url("https://github.com/TxcA/SpannableX", unSpaceRegex)
            "       suggestion // click".newline().span {
                clickable { _, _ ->
                    viewBinding.et.setText("T")
                    toast(requireContext().getString(R.string.input_tip))
                }
                color(Color.BLUE, "// click")
                underline("click")
            }
            "       clickable".clickable(
                color = Color.BLUE,
                typeStyle = Typeface.BOLD or Typeface.ITALIC
            ) { _, matchText ->
                toast("点击: $matchText")
            }
            newline()
        } + "   }".spannable {
            style(Typeface.ITALIC)
            style(Typeface.BOLD)
            color(Color.BLACK)
        }

        // 输入提示
        viewBinding.et.addTextChangedListener {
            it ?: return@addTextChangedListener
            if (it.contains("T", true)) {
                it.spannable {
                    suggestion(
                        requireContext(),
                        arrayOf(
                            "TxcA",
                            "https://github.com/TxcA/",
                            "https://github.com/TxcA/SpannableX",
                        ), 1
                    )
                }
            } else it.removeSpans<SuggestionSpan>()
        }
    }

    companion object {
        fun newInstance() = KotlinFragment()
    }
}