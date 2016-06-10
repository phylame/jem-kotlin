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
import java.io.File
import java.io.IOException

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

