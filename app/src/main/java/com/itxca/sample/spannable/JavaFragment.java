package com.itxca.sample.spannable;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.style.SuggestionSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.itxca.sample.spannable.databinding.SampleFragmentBinding;
import com.itxca.spannablex.Span;
import com.itxca.spannablex.annotation.ConversionUnit;

import java.util.Locale;


public class JavaFragment extends Fragment {

    public static JavaFragment newInstance() {
        return new JavaFragment();
    }

    private SampleFragmentBinding viewBinding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        viewBinding = SampleFragmentBinding.inflate(inflater, container, false);
        return viewBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewBinding.tvTitle.setText(Span.create()
                .text("x SpannableX").relativeSize(1.2f)
                .style(Typeface.BOLD)
                .image(requireContext(), R.mipmap.ic_launcher, viewBinding.tvTitle, null, "x")
                .color("#ff0000", "S")
                .color("#ffa500", "p")
                .color("#ffff00", Span.toReplaceRule("a", false, 0))
                .color("#00ff00", Span.toReplaceRule("n", false, 0))
                .color("#00f7ff", Span.toReplaceRule("n", false, 1))
                .color("#0000ff", Span.toReplaceRule("a", false, 1))
                .color("#8b00ff", "b")
                .color("#ff0000", "l")
                .color("#ffa500", "e")
                .color("#ffff00", "X")
                .spannable());

        Span.activateClick(viewBinding.tvMixed).setText(Span.create()
                .text("all spannable mixed")
                .style(Typeface.ITALIC, "spannable")
                .typeface(null, "serif", new String[]{"all", "mix"})
                .color(Color.parseColor("#da4f49"), "all")
                .color(Color.parseColor("#faa732"), "spannable")
                .color(Color.parseColor("#0088CC"), "mixed")
                .background(Color.parseColor("#eaeaea"))
                .scaleX(1.1f, "spannable")
                .blurMask(1.1f, null, "spannable")
                .superscript("all")
                .subscript("mixed")
                .absoluteSize(12, true)
                .relativeSize(1.2f)
                .strikethrough("all")
                .underline("spannable")
                .clickable(null, null, null, null, null, (v, matchText) ->
                        Utils.toast(this, "点击: " + matchText)
                )
                .spannable()
        );

        Span.activateClick(viewBinding.tvSample).setText(Span.create()
                .text("TextView.setText(Span.create()")
                .style(Typeface.ITALIC)
                .style(Typeface.BOLD, "Span.create()")
                .color(Color.BLACK, "Span.create()")
                .newline()
                .text("       .style()").newline().style(Typeface.BOLD | Typeface.ITALIC)
                .text("       .typeface()").newline().typeface(
                        Typeface.createFromAsset(requireContext().getAssets(), "Inconsolata-Regular.ttf"),
                        null, Utils.getUnSpaceRegex()
                )
                .text("       .textAppearance()").newline().textAppearance(
                        Typeface.ITALIC,
                        Span.sp(14),
                        Color.RED,
                        "serif",
                        ColorStateList.valueOf(Color.BLUE),
                        Utils.getUnSpaceRegex()
                )
                .text("       .color()").newline().color(
                        ContextCompat.getColor(requireContext(), R.color.colorPrimary)
                )
                .text("       .background()").newline().background(
                        ContextCompat.getColor(requireContext(), R.color.colorPrimaryVariant), Utils.getUnSpaceRegex()
                )

                .text("       .image() x").image(
                        requireContext(), R.mipmap.ic_launcher, viewBinding.tvSample, null, "x"
                )
                .image(requireContext(), R.mipmap.ic_launcher, null, Span.drawableSize(18, ConversionUnit.SP))
                .image(requireContext(), R.mipmap.ic_launcher, null, Span.drawableSize(24, ConversionUnit.SP))
                .newline()
                .text("       .scaleX()").newline().scaleX(2.0f, Span.toReplaceRule("X", false, 0))
                .text("       .blurMask()").newline().blurMask(5.0f)
                .text("       .superscript() > Top").newline().superscript("Top")
                .text("       .subscript() > Bottom").newline().subscript("Bottom")
                .text("       .absoluteSize()").newline().absoluteSize(11, true, Utils.getUnSpaceRegex())
                .text("       .relativeSize()").newline().relativeSize(1.5f, Utils.getUnSpaceRegex())
                .text("       .strikethrough()").newline().strikethrough(Utils.getUnSpaceRegex())
                .text("       .underline()").newline().underline(Utils.getUnSpaceRegex())
                .text("       .url()").newline().url("https://github.com/TxcA/SpannableX", Utils.getUnSpaceRegex())
                .text("       .suggestion() // click").newline().clickable(null, null, null, null, null, (v, matchText) ->
                {
                    viewBinding.et.setText("T");
                    Utils.toast(this, requireContext().getString(R.string.input_tip));
                })
                .color(Color.BLUE, "// click")
                .underline("click")
                .text("       .clickable()").clickable(
                        Color.BLUE,
                        null,
                        Typeface.BOLD | Typeface.ITALIC,
                        null,
                        null,
                        (v, matchText) ->
                                Utils.toast(this, "点击: " + matchText)
                )
                .newline()
                .text("       .spannable()").style(Typeface.ITALIC)
                .style(Typeface.BOLD)
                .color(Color.BLACK)
                .text(");")
                .spannable());

        viewBinding.et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s == null) {
                    return;
                }
                if (s.toString().toUpperCase(Locale.ROOT).contains("T")) {
                    Span.create().text(s).suggestion(
                            requireContext(),
                            new String[]{"TxcA", "https://github.com/TxcA/", "https://github.com/TxcA/SpannableX"},
                            1);
                } else {
                    Span.removeSpans(s, SuggestionSpan.class);
                }
            }
        });
    }
}
