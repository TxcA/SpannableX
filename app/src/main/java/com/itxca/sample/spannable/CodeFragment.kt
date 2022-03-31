package com.itxca.sample.spannable

import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.itxca.sample.spannable.databinding.CodeFragmentBinding
import com.itxca.spannablex.ReplaceRule
import com.itxca.spannablex.removeAllSpans
import com.itxca.spannablex.spannable
import com.itxca.spannablex.toReplaceRule
import com.itxca.spannablex.utils.color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class CodeFragment : Fragment() {


    //<editor-fold desc="Code sample replace rules">
    /**
     * 正则替换
     */
    private val functionArray =
        "\\b(class|private|val|override|fun|by|this|super|object)\\b".toRegex()
    private val stringArray = "\"([^\"]*)\"".toRegex()

    /**
     * [Array] [String] 替换
     */
    private val valArray = arrayOf(
        "fragments",
        "layoutInflater",
        "viewBinding",
        "root",
        "tab",
        "container",
        "adapter",
        "text",
        "size",
        "first",
        "second"
    )

    /**
     * [Array] [ReplaceRule] 替换
     */
    private val operatorArray = arrayOf(
        "onCreate".toReplaceRule(true, matchIndex = 0),
        "\\sto\\s".toReplaceRule(true),
        "getItemCount".toReplaceRule(),
        "createFragment".toReplaceRule(),
        "(".toReplaceRule(),
        ")".toReplaceRule(),
        "{".toReplaceRule(),
        "}".toReplaceRule()
    )

    /**
     * [List] [String] 替换
     */
    private val italicsArray = listOf("listOf", " to ", "lazy", "layoutInflater", "root")

    /**
     * 颜色
     */
    private val functionColor = "#cc7832".color
    private val stringColor = "#6a8759".color
    private val valColor = "#9876aa".color
    private val operatorColor = "#ffc66d".color
    //</editor-fold>

    private lateinit var viewBinding: CodeFragmentBinding

    private var lastRefreshTime = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = CodeFragmentBinding.inflate(inflater, container, false).apply {
        viewBinding = this
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.etCode.run {
            setTextColor("#a9b7c6".color)
            typeface = Typeface.createFromAsset(requireContext().assets, "Inconsolata-Regular.ttf")
            addTextChangedListener(afterTextChanged = ::refreshSpan)
            lifecycleScope.launch(Dispatchers.IO) {
                val codeText = requireContext().assets.open("code.txt").use {
                    String(it.readBytes())
                }
                withContext(Dispatchers.Main) {
                    for (c in codeText) {
                        append(c.toString())
                        delay(20)
                    }
                    refreshSpan(viewBinding.etCode.text)
                }
            }
        }
    }

    private fun refreshSpan(editable: Editable?) {
        editable ?: return
        if (System.currentTimeMillis() - lastRefreshTime > REFRESH_TIME_INTERVAL) {
            editable.removeAllSpans()
            editable.spannable {
                color(functionColor, functionArray)
                color(stringColor, stringArray)
                color(operatorColor, operatorArray)
                color(valColor, valArray)
                style(Typeface.ITALIC, italicsArray)
            }
            lastRefreshTime = System.currentTimeMillis()
        }
    }

    companion object {

        private const val REFRESH_TIME_INTERVAL = 300L

        fun newInstance() = CodeFragment()
    }
}