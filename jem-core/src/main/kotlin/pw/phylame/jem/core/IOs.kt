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

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.net.URL
import java.util.*

class Paths {
    companion object {
        fun split(path: String): IntArray {
            var extPos = path.length
            var sepPos = extPos - 1
            while (sepPos >= 0) {
                val ch = path[sepPos]
                if (ch == '.') {
                    extPos = sepPos
                } else if (ch == '/' || ch == '\\') {
                    break
                }
                --sepPos
            }
            return intArrayOf(sepPos, extPos)
        }

        fun fullName(path: String): String {
            val index = split(path)[0]
            return path.substring(if (index != 0) index + 1 else index)
        }

        fun baseName(path: String): String {
            val indexes = split(path)
            return path.substring(indexes[0] + 1, indexes[1])
        }

        fun extensionName(path: String): String {
            val index = split(path)[1]
            return if (index != path.length) path.substring(index + 1) else ""
        }

        private const val MIME_MAPPING_FILE = "mime.properties"

        private val mimeMap: MutableMap<String, String> by lazy {
            val map = HashMap<String, String>()
            val url = Paths::class.java.getResource(MIME_MAPPING_FILE)
            if (url != null) {
                loadProperties(url, map)
            }
            return@lazy map
        }

        fun mapMime(extension: String, mime: String) {
            mimeMap.put(extension, mime)
        }

        const val UNKNOWN_MIME = "application/octet-stream"

        fun mimeFor(name: String): String {
            if (name.isEmpty()) {
                return ""
            }
            val ext = extensionName(name)
            if (ext.isEmpty()) {
                return UNKNOWN_MIME
            }
            return mimeMap[ext] ?: UNKNOWN_MIME
        }

        internal fun mimeOrDetect(path: String, mime: String?): String =
                if (mime == null || mime.isEmpty()) mimeFor(path) else mime
    }
}

fun loadProperties(url: URL, map: MutableMap<String, String>): Int {
    val prop = Properties()
    url.openStream().buffered().use {
        prop.load(it)
    }
    for ((k, v) in prop) {
        map[k.toString()] = v.toString()
    }
    return prop.size
}

fun RandomAccessFile.copyTo(out: OutputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
    var bytesCopied: Long = 0
    val buffer = ByteArray(bufferSize)
    var bytes = read(buffer)
    while (bytes >= 0) {
        out.write(buffer, 0, bytes)
        bytesCopied += bytes
        bytes = read(buffer)
    }
    return bytesCopied
}

fun RandomAccessFile.copyTo(out: RandomAccessFile, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
    var bytesCopied: Long = 0
    val buffer = ByteArray(bufferSize)
    var bytes = read(buffer)
    while (bytes >= 0) {
        out.write(buffer, 0, bytes)
        bytesCopied += bytes
        bytes = read(buffer)
    }
    return bytesCopied
}

fun RandomAccessFile.readBytes(estimatedSize: Int = DEFAULT_BUFFER_SIZE): ByteArray {
    val buffer = ByteArrayOutputStream(estimatedSize)
    copyTo(buffer)
    return buffer.toByteArray()
}

class RAFInputStream
constructor(private val source: RandomAccessFile, offset: Long, size: Long) : InputStream() {
    private val endpos: Long
    private var curpos: Long = 0

    init {
        val length = source.length()
        curpos = if (offset < 0) 0 else offset
        endpos = if (size < 0) length else curpos + size
        if (curpos >= length) {
            throw IllegalArgumentException("offset >= length of source")
        }
        if (endpos > length) {
            throw IllegalArgumentException("offset + size > length of source")
        }
    }

    constructor(source: RandomAccessFile, size: Long) : this(source, source.filePointer, size)

    override fun read(): Int {
        if (curpos < endpos) {
            ++curpos
            return source.read()
        } else {
            return -1
        }
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (off < 0 || len < 0 || len > b.size - off) {
            throw IndexOutOfBoundsException()
        } else if (len == 0) {
            return 0
        }
        var count = endpos - curpos
        if (count == 0L) {
            return -1
        }
        count = if (count < len) count else len.toLong()
        val n = source.read(b, off, count.toInt())
        curpos += count
        return n
    }

    override fun skip(n: Long): Long {
        if (n < 0) {
            return 0
        }
        val len = source.skipBytes(Math.min(n, endpos - curpos).toInt()).toLong()
        curpos = Math.min(curpos + len, endpos)
        return len
    }

    override fun available(): Int {
        return (endpos - curpos).toInt()
    }
}
