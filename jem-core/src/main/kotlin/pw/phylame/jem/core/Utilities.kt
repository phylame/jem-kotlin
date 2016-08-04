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

import java.io.PrintWriter
import java.io.StringWriter
import java.text.MessageFormat
import java.text.SimpleDateFormat
import java.util.*

open class JemException : Exception {
    constructor(message: String) : super(message)

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(cause: Throwable) : super(cause)
}

// for unsupervised ebook format
class UnsupportedFormatException : JemException {
    constructor(message: String) : super(message)

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(cause: Throwable) : super(cause)
}

open class VariantMap(m: MutableMap<CharSequence, Any> = HashMap(),
                      var validator: ((CharSequence, Any) -> Unit)? = null) : Cloneable, MutableMap<CharSequence, Any> by m {

    inline fun <reified T> get(key: CharSequence, defaultValue: T): T {
        val value = this[key]
        return if (value != null && value is T) value else defaultValue
    }

    fun put(key: CharSequence, value: Any, validation: Boolean = false): Any? {
        if (validation)
            validator?.invoke(key, value)
        return put(key, value)
    }

    fun putAll(from: Map<out CharSequence, Any>, validation: Boolean = false) {
        from.forEach {
            put(it.key, it.value, validation)
        }
    }

    fun putAll(from: VariantMap, validation: Boolean = false) {
        from.forEach {
            put(it.key, it.value, validation)
        }
    }

    public override fun clone(): VariantMap {
        val dump = VariantMap()
        dump.putAll(this)
        return dump
    }
}

val Throwable.stackMessage: String
    get() {
        val w = StringWriter()
        printStackTrace(PrintWriter(w))
        return w.toString()
    }

object Log {
    var normalOutput: (String) -> Unit = ::println
    var errorOutput: (String) -> Unit = { System.err?.println(it) }

    // trace
    fun t(tag: String, msg: String, vararg args: Any) {
        normalOutput(makeText(tag, "t", msg, args))
    }

    fun t(tag: String, t: Throwable) {
        t(tag, t.stackMessage)
    }

    // debug
    fun d(tag: String, msg: String, vararg args: Any) {
        normalOutput(makeText(tag, "d", msg, args))
    }

    fun d(tag: String, t: Throwable) {
        d(tag, t.stackMessage)
    }

    // error
    fun e(tag: String, msg: String, vararg args: Any) {
        errorOutput(makeText(tag, "e", msg, args))
    }

    fun e(tag: String, t: Throwable) {
        e(tag, t.stackMessage)
    }

    private fun makeText(tag: String, level: String, msg: String, vararg args: Any): String =
            "[${Thread.currentThread().name}] $level/$tag: ${MessageFormat.format(msg, args)}"
}

object Variants {
    const val BLOB = "blob"
    const val TEXT = "text"
    const val STRING = "str"
    const val INTEGER = "int"
    const val REAL = "real"
    const val LOCALE = "locale"
    const val DATETIME = "datetime"
    const val BOOLEAN = "bool"

    init {
        mapType(Byte::class.java, INTEGER)
        mapType(Short::class.java, INTEGER)
        mapType(Int::class.java, INTEGER)
        mapType(Long::class.java, INTEGER)
        mapType(Float::class.java, REAL)
        mapType(Double::class.java, REAL)
        mapType(Boolean::class.java, BOOLEAN)
    }

    fun supportedTypes(): Array<String> =
            arrayOf(BLOB, TEXT, STRING, INTEGER, REAL, LOCALE, DATETIME, BOOLEAN)

    private val variantTypes: MutableMap<Class<*>, String> = HashMap()

    fun mapType(clazz: Class<*>, type: String) {
        variantTypes[clazz] = type
    }

    fun typeOf(o: Any): String =
            variantTypes.getOrElse(o.javaClass) {
                when (o) {
                    is CharSequence -> STRING
                    is Text -> TEXT
                    is Flob -> BLOB
                    is Date -> DATETIME
                    is Locale -> LOCALE
                    else -> STRING
                }
            }

    fun defaultFor(type: String): Any =
            when (type) {
                STRING -> ""
                TEXT -> Texts.emptyText()
                BLOB -> Blobs.emptyFile()
                DATETIME -> Date()
                LOCALE -> Locale.getDefault()
                INTEGER -> 0
                REAL -> 0.0
                BOOLEAN -> false
                else -> ""
            }

    fun format(o: Any): CharSequence =
            when (o) {
                is Text -> o.string
                is Date -> SimpleDateFormat("yy-M-d").format(o)
                is Locale -> o.displayName
                else -> o.toString()
            }
}
