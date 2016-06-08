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

class ImplementationManager<T>(val type: Class<T>, val reusable: Boolean = true) {
    private val implementations = HashMap<String, ImpHolder>()
    private val caches: MutableMap<String, T>?

    init {
        caches = if (reusable) HashMap() else null
    }

    fun register(name: String, path: String) {
        if (name.isEmpty()) {
            throw IllegalArgumentException("name cannot be empty")
        }
        if (path.isEmpty()) {
            throw IllegalArgumentException("path cannot be empty")
        }
        val imp = implementations[name]
        if (imp != null) {
            imp.path = path
            caches?.remove(name)
        } else {
            implementations[name] = ImpHolder(path = path)
        }
    }

    fun register(name: String, clazz: Class<out T>) {
        if (name.isEmpty()) {
            throw IllegalArgumentException("name cannot be empty")
        }
        val imp = implementations[name]
        if (imp != null) {
            imp.clazz = clazz
            caches?.remove(name)
        } else {
            implementations[name] = ImpHolder(clazz = clazz)
        }
    }

    operator fun contains(name: String): Boolean = name in implementations

    fun remove(name: String) {
        implementations.remove(name)
        caches?.remove(name)
    }

    val names: Set<String> get() = implementations.keys

    fun getInstance(name: String): T? {
        var obj = caches?.get(name)
        if (obj != null) {
            return obj
        }
        val imp = implementations[name]
        if (imp != null) {
            obj = imp.instantiate()
            if (obj != null) {
                caches?.put(name, obj)
            }
        }
        return obj
    }

    private inner class ImpHolder(internal var path: String? = null, internal var clazz: Class<out T>? = null) {
        fun instantiate(): T? {
            if (clazz != null) {
                return clazz!!.newInstance()
            }
            if (path == null) {
                throw IllegalStateException("No path or clazz specified")
            }
            val clazz = Class.forName(path)
            if (!type.isAssignableFrom(clazz)) {
                return null
            }
            this.clazz = clazz as Class<T>
            return clazz.newInstance()
        }
    }
}

open class VariantMap
constructor(private var map: MutableMap<String, Any> = HashMap(),
            var validator: ((String, Any) -> Unit)? = null) :
        Iterable<Map.Entry<String, Any>>, Cloneable {

    open fun put(key: String, value: Any, validation: Boolean = false): VariantMap {
        if (validation) {
            validator?.invoke(key, value)
        }
        map[key] = value
        return this
    }

    operator fun set(key: String, value: Any) {
        put(key, value)
    }

    fun update(map: Map<String, Any>, validation: Boolean = false): VariantMap {
        map.forEach {
            put(it.key, it.value, validation)
        }
        return this
    }

    fun update(rhs: VariantMap, validation: Boolean = false): VariantMap = update(rhs.map, validation)

    operator fun contains(key: String): Boolean = key in map

    operator fun get(key: String, defaultValue: Any? = null): Any? = map[key] ?: defaultValue

    fun get(key: String, defaultString: String = ""): String = map[key]?.toString() ?: defaultString

    fun <T> get(key: String, type: Class<T>, defaultValue: T? = null): T? {
        val o = map[key]
        return if (o != null && type.isInstance(o)) o as T else defaultValue
    }

    fun remove(key: String): Any? = map.remove(key)

    fun clear(): VariantMap {
        map.clear()
        return this
    }

    val size: Int get() = map.size

    val keys: Set<String> get() = map.keys

    val items: Set<Map.Entry<String, Any>> get() = map.entries

    override fun iterator(): Iterator<Map.Entry<String, Any>> = map.iterator()

    public override fun clone(): VariantMap {
        val result = super.clone() as VariantMap
        result.map = HashMap(map)
        return result
    }

    override fun toString(): String = map.toString()
}

val Throwable.stackMessage: String
    get() {
        val w = StringWriter()
        printStackTrace(PrintWriter(w))
        return w.toString()
    }

class Log {
    companion object {
        // trace
        fun t(tag: String, msg: String, vararg args: Any) {
            println(makeText(tag, "t", msg, args))
        }

        fun t(tag: String, t: Throwable) {
            t(tag, t.stackMessage)
        }

        // debug
        fun d(tag: String, msg: String, vararg args: Any) {
            println(makeText(tag, "d", msg, args))
        }

        fun d(tag: String, t: Throwable) {
            d(tag, t.stackMessage)
        }

        // error
        fun e(tag: String, msg: String, vararg args: Any) {
            System.err?.println(makeText(tag, "e", msg, args))
        }

        fun e(tag: String, t: Throwable) {
            e(tag, t.stackMessage)
        }

        private fun makeText(tag: String, level: String, msg: String, vararg args: Any): String =
                "[${Thread.currentThread().name}] $level/$tag: ${MessageFormat.format(msg, args)}"
    }
}

const val FILE = "file"
const val TEXT = "text"
const val STRING = "str"
const val INTEGER = "int"
const val REAL = "real"
const val LOCALE = "locale"
const val DATETIME = "datetime"
const val BOOLEAN = "bool"

fun supportedTypes(): Array<String> = arrayOf(FILE, TEXT, STRING, INTEGER, REAL, LOCALE, DATETIME, BOOLEAN)

private val variantTypes: MutableMap<Class<*>, String> = HashMap()

fun mapVariantType(clazz: Class<*>, type: String) {
    variantTypes[clazz] = type
}

fun typeOfVariant(o: Any): String {
    val clazz = o.javaClass
    val name = variantTypes[clazz]
    if (name != null) {
        return name
    }
    if (Text::class.java.isAssignableFrom(clazz)) {
        return TEXT
    } else if (Blob::class.java.isAssignableFrom(clazz)) {
        return FILE
    } else {
        return clazz.name
    }
}

fun defaultOfType(type: String): Any = when (type) {
    STRING -> ""
    TEXT -> Texts.emptyText(Texts.PLAIN)
    FILE -> Blobs.emptyFile(Paths.UNKNOWN_MIME)
    DATETIME -> Date()
    LOCALE -> Locale.getDefault()
    INTEGER -> 0
    REAL -> 0.0
    BOOLEAN -> false
    else -> ""
}

fun formatVariant(o: Any): CharSequence = when (o) {
    is Text -> o.text
    is Date -> SimpleDateFormat("yy-M-d").format(o)
    is Locale -> o.displayName
    else -> o.toString()
}
