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

import androidx.annotation.Keep
import com.itxca.spannablex.interfaces.OnSpanReplacementMatch

/**
 * 替换规则
 */
@Keep
data class ReplaceRule(
    /**
     * 查找的字符串或正则文本
     */
    val replaceString: String,
    /**
     * [replaceString]是否为正则
     */
    val isRegex: Boolean,
    /**
     * 匹配范围
     */
    val matchRange: IntRange?,
    /**
     * 替换文本(null 为不替换)
     */
    val newString: CharSequence?,
    /**
     * 匹配时回调
     */
    val replacementMatch: OnSpanReplacementMatch?
) {
    internal val replaceRules: Regex
        get() = (if (isRegex) replaceString else Regex.escape(replaceString)).toRegex()
}


/**
 * 创建替换规则
 * @receiver 查找的字符串或正则文本
 * @param isRegex receiver是否为正则
 * @param matchIndex 单一匹配位置 ([matchRange]不为null时优先使用[matchRange])
 * @param matchRange 匹配范围
 * @param newString 替换文本(null 为不替换)
 * @param replacementMatch 匹配时回调
 */
fun String.toReplaceRule(
    isRegex: Boolean = false,
    matchIndex: Int? = null,
    matchRange: IntRange? = null,
    newString: CharSequence? = null,
    replacementMatch: OnSpanReplacementMatch? = null
): ReplaceRule = ReplaceRule(
    replaceString = this,
    isRegex = isRegex,
    matchRange = matchRange ?: matchIndex?.let { matchIndex..matchIndex },
    newString = newString,
    replacementMatch = replacementMatch
)
