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
import java.util.*

class ImplementorFactory<T>(val type: Class<T>, val reusable: Boolean = true, val loader: ClassLoader? = null) {
    private val impHolders = HashMap<String, ImpHolder>()

    operator fun set(name: String, path: String) {
        require(name.isNotEmpty()) { "name cannot be empty" }
        require(path.isNotEmpty()) { "path cannot be empty" }
        val imp = impHolders[name]
        if (imp != null) {
            imp.path = path
        } else {
            impHolders[name] = ImpHolder(path = path)
        }
    }

    operator fun set(name: String, clazz: Class<out T>) {
        require(name.isNotEmpty()) { "name cannot be empty" }
        val imp = impHolders[name]
        if (imp != null) {
            imp.clazz = clazz
        } else {
            impHolders[name] = ImpHolder(clazz = clazz)
        }
    }

    operator fun contains(name: String): Boolean = name in impHolders

    fun remove(name: String) {
        impHolders.remove(name)
    }

    val names: Set<String> get() = impHolders.keys

    operator fun get(name: String): T? = impHolders[name]?.instantiate()

    private inner class ImpHolder(var path: String? = null, var clazz: Class<out T>? = null) {
        /**
         * Cached instance.
         */
        var instance: T? = null

        @Suppress("UNCHECKED_CAST")
        fun instantiate(): T? {
            if (reusable && instance != null) {
                return instance
            }
            if (clazz == null) {
                checkNotNull(path) { "No path or clazz specified" }
                val cls = if (loader != null) Class.forName(path, true, loader) else Class.forName(path)
                if (!type.isAssignableFrom(cls)) {
                    Log.d("IMP", "${cls.name} not extend or implement ${type.name}")
                    return null
                }
                clazz = cls as Class<T>
            }
            val obj = clazz?.newInstance()
            if (reusable) {
                instance = obj
            }
            return obj
        }
    }
}

object EpmManager {
    private const val TAG = "EPM"

    const val PARSER_DEFINE_FILE = "META-INF/pw-jem/parsers.properties"
    const val MAKER_DEFINE_FILE = "META-INF/pw-jem/makers.properties"

    const val AUTO_LOAD_CUSTOMIZED_KEY = "jem.emp.autoLoad"

    val parsers = ImplementorFactory(Parser::class.java, true)

    val makers = ImplementorFactory(Maker::class.java, true)

    /**
     * Maps epm name to extension names.
     */
    val extensions: MutableMap<String, MutableSet<String>> = HashMap()

    /**
     * Maps extension name to epm name.
     */
    val names = HashMap<String, String>()

    init {
        if (System.getProperty(AUTO_LOAD_CUSTOMIZED_KEY)?.equals("true") ?: true) {
            loadCustomizedImplementors()
        }
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

    fun mapExtensions(name: String, extensions: Collection<String>) {
        (this.extensions[name] as? MutableSet<String> ?:
                HashSet<String>().apply {
                    this@EpmManager.extensions.put(name, this)
                }).let {
            it.addAll(extensions)
        }
        extensions.forEach {
            names[it] = name
        }
    }

    fun extensionsForName(name: String): Set<String>? = extensions[name]

    fun nameOfExtension(extension: String): String? = names[extension]

    fun loadCustomizedImplementors() {
        val loader = contextClassLoader()
        loadRegisters(loader, PARSER_DEFINE_FILE, parsers)
        loadRegisters(loader, MAKER_DEFINE_FILE, makers)
    }

    private const val NAME_EXTENSION_SEPARATOR = ";"
    private const val EXTENSION_SEPARATOR = " "

    private fun <T> loadRegisters(loader: ClassLoader?, path: String, factory: ImplementorFactory<T>) {
        val urls = resourcesForPath(path, loader) ?: return
        try {
            urls.asSequence().forEach {
                it.openStream().use {
                    Properties().apply {
                        load(it)
                        for ((k, v) in this) {
                            val name = k.toString()
                            val parts = v.toString().split(NAME_EXTENSION_SEPARATOR)
                            factory[name] = parts[0]
                            if (parts.size > 1) {
                                mapExtensions(name, parts[1].toLowerCase().split(EXTENSION_SEPARATOR))
                            } else {
                                mapExtensions(name, listOf(name))
                            }
                        }
                    }
                }
            }
        } catch(e: IOException) {
            Log.e(TAG, "Cannot load customized implementors", e)
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
