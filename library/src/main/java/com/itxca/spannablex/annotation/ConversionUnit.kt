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

package com.itxca.spannablex.annotation

import androidx.annotation.IntDef

@IntDef(value = [ConversionUnit.NOT_CONVERT, ConversionUnit.SP, ConversionUnit.DP])
@Retention(AnnotationRetention.SOURCE)
annotation class ConversionUnit {

    companion object {
        /**
         * 不转换单位
         */
        const val NOT_CONVERT = 0

        /**
         * 转换为sp
         */
        const val SP = 1

        /**
         * 转换为dp
         */
        const val DP = 2
    }
}




