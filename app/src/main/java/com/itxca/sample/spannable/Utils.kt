@file:JvmName("Utils")

package com.itxca.sample.spannable

import android.widget.Toast
import androidx.fragment.app.Fragment

/**
 * 简单toast
 */
fun Fragment.toast(msg: String) {
    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}

/**
 * 非空白字符正则
 */
val unSpaceRegex = "\\S+".toRegex()
