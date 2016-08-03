/*
 * Copyright 2014-2016 Peng Wan <phylame@163.com>
 *
 * This file is part of Jem.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pw.phylame.jem.core

import java.io.BufferedReader
import java.io.Writer

interface Text : Iterable<String> {
    val type: CharSequence

    val string: CharSequence

    val lines: List<String> get() = string.lines()

    fun writeTo(out: Writer): Long {
        val str = string
        out.write(str.toString())
        out.flush()
        return str.length.toLong()
    }

    override fun iterator(): Iterator<String> = lines.iterator()
}

abstract class AbstractText(override val type: CharSequence) : Text {
    override fun toString(): String = string.toString()
}

class RawText
internal constructor(override val string: CharSequence, type: CharSequence) : AbstractText(type)

class BlobText
internal constructor(val blob: Flob, val encoding: String, type: CharSequence) : AbstractText(type) {

    private fun reader(): BufferedReader = blob.inputStream().bufferedReader(encoding)

    override val string: CharSequence get() = reader().readText()

    override val lines: List<String> get() = reader().readLines()

    override fun writeTo(out: Writer): Long = reader().copyTo(out)
}

object Texts {
    const val HTML = "html"

    const val PLAIN = "plain"

    fun forString(str: CharSequence, type: CharSequence = PLAIN): Text = RawText(str, type)

    fun forBlob(blob: Flob, encoding: String, type: CharSequence = PLAIN): Text =
            BlobText(blob, encoding, type)

    fun emptyText(type: CharSequence = PLAIN): Text = forString("", type)
}
