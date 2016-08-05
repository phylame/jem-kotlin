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
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

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

abstract class BookWorker<out CF : CommonConfig>(val name: String,
                                                 private val configKey: String? = null,
                                                 private val configClass: Class<CF>? = null) {
    init {
        require(configKey != null && configClass == null) { "'configKey' is valid but 'configClass' not" }
    }

    protected fun fetchConfig(kw: Map<String, Any>): CF {
        TODO("add fetch method")
    }
}

abstract class CommonParser<IN : Closeable, CF : CommonConfig>(name: String, configKey: String?, configClass: Class<CF>?) :
        BookWorker<CF>(name, configKey, configClass), Parser {
    lateinit protected var source: File

    abstract fun openFile(file: File, config: CF): IN

    open fun validateInput(input: IN, config: CF) {
    }

    abstract fun parse(input: IN, config: CF): Book

    override final fun parse(file: File, args: Map<String, Any>): Book {
        if (!file.exists()) {
            throw FileNotFoundException("No such file ${file.path}")
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

abstract class CommonMaker<CF : CommonConfig>(name: String, configKey: String?, configClass: Class<CF>?) :
        BookWorker<CF>(name, configKey, configClass), Maker {

    abstract fun make(book: Book, output: OutputStream, config: CF)

    override final fun make(book: Book, file: File, args: Map<String, Any>) {
        FileOutputStream(file).buffered().use {
            make(book, it, fetchConfig(args))
        }
    }
}

abstract class ZipParser<CF : ZipParseConfig>(name: String, configKey: String?, configClass: Class<CF>?) :
        CommonParser<ZipFile, CF>(name, configKey, configClass) {
    override final fun openFile(file: File, config: CF): ZipFile = ZipFile(file)
}

abstract class ZipMaker<CF : ZipMakeConfig>(name: String, configKey: String?, configClass: Class<CF>?) : CommonMaker<CF>(name, configKey, configClass) {

    abstract fun make(book: Book, zipout: ZipOutputStream, config: CF)

    override final fun make(book: Book, output: OutputStream, config: CF) {
        ZipOutputStream(output).use {
            it.setMethod(config.zipMethod)
            it.setLevel(config.zipLevel)
            it.setComment(config.zipComment)
            make(book, it, config)
            it.flush()
        }
    }
}

abstract class BinaryParser<CF : CommonConfig>(name: String, configKey: String?, configClass: Class<CF>?) :
        CommonParser<RandomAccessFile, CF>(name, configKey, configClass) {
    override final fun openFile(file: File, config: CF): RandomAccessFile = BufferedRandomAccessFile(file)
}