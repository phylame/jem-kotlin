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

import pw.phylame.jem.core.Book
import pw.phylame.jem.core.JemException
import java.io.*

interface Parser {
    val name: String

    @Throws(IOException::class, JemException::class)
    fun parse(file: File, args: Map<String, Any>): Book
}

interface Maker {
    val name: String

    @Throws(IOException::class, JemException::class)
    fun make(book: Book, file: File, args: Map<String, Any>)
}

abstract class BookWorker<CF : CommonConfig>
constructor(val name: String,
            private val cfgkey: String? = null,
            private val cfgcls: Class<CF>? = null) {
    init {
        if (cfgkey != null && cfgcls == null) {
            throw IllegalArgumentException("'configKey' is valid but 'configClass' not")
        }
    }

    protected fun fetchConfig(kw: Map<String, Any>): CF? {
        return null
    }
}

abstract class CommonParser<IN : Closeable, CF : CommonConfig>(name: String, cfgkey: String?, cfgcls: Class<CF>?) :
        BookWorker<CF>(name, cfgkey, cfgcls), Parser {
    lateinit protected var source: File

    abstract fun openFile(file: File, config: CF?): IN

    open fun validateInput(input: IN, config: CF?) {
    }

    abstract fun parse(input: IN, config: CF?): Book

    override fun parse(file: File, args: Map<String, Any>): Book {
        if (!file.exists()) {
            throw FileNotFoundException(file.path)
        }
        val config = fetchConfig(args)
        val input = openFile(file, config)
        val book: Book
        try {
            validateInput(input, config)
            source = file
            book = parse(input, config)
        } catch(e: Exception) {
            input.close()
            throw e
        }
        // source clean
        return book
    }
}

abstract class CommonMaker<CF : CommonConfig>(name: String, cfgkey: String?, cfgcls: Class<CF>?) :
        BookWorker<CF>(name, cfgkey, cfgcls), Maker {

    abstract fun make(book: Book, output: OutputStream, config: CF?)

    override fun make(book: Book, file: File, args: Map<String, Any>) {
        FileOutputStream(file).buffered().use {
            make(book, it, fetchConfig(args))
        }
    }
}
