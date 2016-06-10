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

object EpmManager {
    const val PARSER_DEFINE_FILE = "META-INF/pw-jem/parsers.properties"
    const val MAKER_DEFINE_FILE = "META-INF/pw-jem/makers.properties"

    val parsers = ImplementationManager(Parser::class.java, true)

    val makers = ImplementationManager(Maker::class.java, true)

    val extensions: MutableMap<String, Set<String>> = HashMap()

    val names = HashMap<String, String>()

    init {
        loadCustomizedImplementations()
    }

    fun registerParser(name: String, path: String) {
        parsers.register(name, path)
    }

    fun registerParser(name: String, clazz: Class<out Parser>) {
        parsers.register(name, clazz)
    }

    fun removeParser(name: String) {
        parsers.remove(name)
    }

    fun hasParser(name: String): Boolean = name in parsers

    fun supportedParsers(): Set<String> = parsers.names

    fun parserFor(name: String): Parser? = parsers.getInstance(name)

    fun registerMaker(name: String, path: String) {
        makers.register(name, path)
    }

    fun registerMaker(name: String, clazz: Class<out Maker>) {
        makers.register(name, clazz)
    }

    fun removeMaker(name: String) {
        makers.remove(name)
    }

    fun hasMaker(name: String): Boolean = name in makers

    fun supportedMakers(): Set<String> = makers.names

    fun makerFor(name: String): Maker? = makers.getInstance(name)

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

    private fun <T> loadRegisters(loader: ClassLoader?, path: String, mgr: ImplementationManager<T>) {
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
                    mgr.register(name, parts[0])
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

fun parserForFormat(format: String): Parser {
    val parser = EpmManager.parserFor(format) ?: throw UnsupportedFormatException("Unsupported format: $format")
    return parser
}

fun makerForFormat(format: String): Maker {
    val maker = EpmManager.makerFor(format) ?: throw UnsupportedFormatException("Unsupported format: $format")
    return maker
}

fun readBook(file: File, format: String, args: Map<String, Any> = emptyMap()): Book =
        parserForFormat(format).parse(file, args)

fun writeBook(book: Book, file: File, format: String = PMAB, args: Map<String, Any> = emptyMap()) {
    makerForFormat(format).make(book, file, args)
}

fun Book.make(file: File, format: String = PMAB, args: Map<String, Any> = emptyMap()) {
    writeBook(this, file, format, args)
}