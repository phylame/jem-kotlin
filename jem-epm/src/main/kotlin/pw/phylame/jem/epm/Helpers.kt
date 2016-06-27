/*
 * Copyright 2014-2016 Peng Wan <phylame@163.com>
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

package pw.phylame.jem.epm

import pw.phylame.jem.core.*
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.*

class ImplManager<T>(val type: Class<T>, val reusable: Boolean = true) {
    private val impls = HashMap<String, ImpHolder>()

    operator fun set(name: String, path: String) {
        if (name.isEmpty()) {
            throw IllegalArgumentException("name cannot be empty")
        }
        if (path.isEmpty()) {
            throw IllegalArgumentException("path cannot be empty")
        }
        val imp = impls[name]
        if (imp != null) {
            imp.path = path
        } else {
            impls[name] = ImpHolder(path = path, reusable = reusable)
        }
    }

    operator fun set(name: String, clazz: Class<out T>) {
        if (name.isEmpty()) {
            throw IllegalArgumentException("name cannot be empty")
        }
        val imp = impls[name]
        if (imp != null) {
            imp.clazz = clazz
        } else {
            impls[name] = ImpHolder(clazz = clazz, reusable = reusable)
        }
    }

    operator fun contains(name: String): Boolean = name in impls

    fun remove(name: String) {
        impls.remove(name)
    }

    val names: Set<String> get() = impls.keys

    operator fun get(name: String): T? = impls[name]?.instantiate()

    private inner class ImpHolder(var path: String? = null,
                                  var clazz: Class<out T>? = null,
                                  val reusable: Boolean = true) {
        var instance: T? = null

        @Suppress("UNCHECKED_CAST")
        fun instantiate(): T? {
            if (reusable && instance != null) {
                return instance;
            }
            if (clazz == null) {
                if (path == null) {
                    throw IllegalStateException("No path or clazz specified")
                }
                val cls = Class.forName(path)
                if (!type.isAssignableFrom(cls)) {
                    Log.d("IMP", "${cls.name} not extend or implement ${type.name}")
                    return null
                }
                clazz = cls as Class<T>
            }
            if (reusable) {
                instance = clazz?.newInstance()
            }
            return instance
        }
    }
}

object EpmManager {
    const val PARSER_DEFINE_FILE = "META-INF/pw-jem/parsers.properties"
    const val MAKER_DEFINE_FILE = "META-INF/pw-jem/makers.properties"

    val parsers = ImplManager(Parser::class.java, true)

    val makers = ImplManager(Maker::class.java, true)

    val extensions: MutableMap<String, Set<String>> = HashMap()

    val names = HashMap<String, String>()

    init {
        loadCustomizedImplementations()
    }

    fun registerParser(name: String, path: String) {
        parsers[name] = path
    }

    fun registerParser(name: String, clazz: Class<out Parser>) {
        parsers[name] = clazz
    }

    fun removeParser(name: String) {
        parsers.remove(name)
    }

    fun hasParser(name: String): Boolean = name in parsers

    fun supportedParsers(): Set<String> = parsers.names

    fun parserFor(name: String): Parser? = parsers[name]

    fun registerMaker(name: String, path: String) {
        makers[name] = path
    }

    fun registerMaker(name: String, clazz: Class<out Maker>) {
        makers[name] = clazz
    }

    fun removeMaker(name: String) {
        makers.remove(name)
    }

    fun hasMaker(name: String): Boolean = name in makers

    fun supportedMakers(): Set<String> = makers.names

    fun makerFor(name: String): Maker? = makers[name]

    fun mapExtensions(name: String, extensions: List<String>?) {
        var old = this.extensions[name] as? MutableSet<String>
        if (old == null) {
            old = HashSet()
            this.extensions.put(name, old)
        }
        if (extensions == null) {
            old.add(name)
        } else {
            old.addAll(extensions)
        }
        old.forEach {
            names[it] = name
        }
    }

    fun extensionForName(name: String): Set<String>? = extensions[name]

    fun nameOfExtension(extension: String): String? = names[extension]

    fun loadCustomizedImplementations() {
        val loader = contextClassLoader()
        loadRegisters(loader, PARSER_DEFINE_FILE, parsers)
        loadRegisters(loader, MAKER_DEFINE_FILE, makers)
    }

    private const val NAME_EXTENSION_SEPARATOR = ";"
    private const val EXTENSION_SEPARATOR = " "

    private fun <T> loadRegisters(loader: ClassLoader?, path: String, mgr: ImplManager<T>) {
        val urls = resourcesForPath(path, loader) ?: return
        var input: InputStream
        var prop: Properties
        try {
            while (urls.hasMoreElements()) {
                input = urls.nextElement().openStream()
                prop = Properties()
                prop.load(input)
                for ((k, v) in prop) {
                    val name = k.toString()
                    val parts = v.toString().split(NAME_EXTENSION_SEPARATOR)
                    mgr[name] = parts[0]
                    if (parts.size > 1) {
                        mapExtensions(name, parts[1].toLowerCase().split(EXTENSION_SEPARATOR))
                    } else {
                        mapExtensions(name, null)
                    }
                }
            }
        } catch(e: IOException) {
        }
    }
}

fun formatOfExtension(path: String): String? = EpmManager.nameOfExtension(Paths.extensionName(path).toLowerCase())

fun parserForFormat(format: String): Parser =
        EpmManager.parserFor(format) ?: throw UnsupportedFormatException("Unsupported format: $format")

fun makerForFormat(format: String): Maker =
        EpmManager.makerFor(format) ?: throw UnsupportedFormatException("Unsupported format: $format")

fun readBook(file: File, format: String, args: Map<String, Any> = emptyMap()): Book =
        parserForFormat(format).parse(file, args)

fun writeBook(book: Book, file: File, format: String = PMAB, args: Map<String, Any> = emptyMap()) {
    makerForFormat(format).make(book, file, args)
}

fun Book.make(file: File, format: String = PMAB, args: Map<String, Any> = emptyMap()) {
    writeBook(this, file, format, args)
}
