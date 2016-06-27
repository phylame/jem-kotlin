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

import java.io.*
import java.net.URL
import java.nio.charset.Charset
import java.security.AccessController
import java.security.PrivilegedAction
import java.util.*

object Paths {
    data class SplitResult(val separatorIndex: Int, val extensionIndex: Int)

    fun split(path: String): SplitResult {
        var extpos = path.length
        var seppos = extpos - 1
        while (seppos >= 0) {
            val ch = path[seppos]
            if (ch == '.') {
                extpos = seppos
            } else if (ch == '/' || ch == '\\') {
                break
            }
            seppos--
        }
        return SplitResult(seppos, extpos)
    }

    fun fullName(path: String): String {
        val seppos = split(path).separatorIndex
        return path.substring(if (seppos != 0) seppos + 1 else seppos)
    }

    fun baseName(path: String): String {
        val (seppos, extpos) = split(path)
        return path.substring(seppos + 1, extpos)
    }

    fun extensionName(path: String): String {
        val seppos = split(path).separatorIndex
        return if (seppos != path.length) path.substring(seppos + 1) else ""
    }

    private const val MIME_MAPPING_FILE = "mime.properties"

    private val mimeMap: MutableMap<String, String> by lazy {
        val map = HashMap<String, String>()
        val url = Paths::class.java.getResource(MIME_MAPPING_FILE)
        if (url != null) {
            loadProperties(url, map)
        }
        map
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

    fun mimeOrDetect(path: String, mime: String?): String =
            if (mime == null || mime.isEmpty()) mimeFor(path) else mime
}

fun contextClassLoader(): ClassLoader? {
    return AccessController.doPrivileged(PrivilegedAction {
        var classLoader: ClassLoader? = null
        try {
            classLoader = Thread.currentThread().contextClassLoader
        } catch (ex: SecurityException) {
            Log.d("Cannot get context class loader", ex)
        }
        classLoader
    })
}

fun resourcesForPath(name: String, loader: ClassLoader? = null): Enumeration<URL>? {
    return AccessController.doPrivileged(PrivilegedAction {
        try {
            loader?.getResources(name) ?: ClassLoader.getSystemResources(name)
        } catch (e: IOException) {
            null
        }
    })
}

fun loadProperties(url: URL): Properties =
        Properties().apply {
            url.openStream().buffered().use {
                load(it)
            }
        }

fun loadProperties(url: URL, map: MutableMap<String, String>): Int =
        loadProperties(url).run {
            for ((k, v) in this) {
                map[k.toString()] = v.toString()
            }
            size
        }

fun InputStream.bufferedReader(encoding: String): BufferedReader =
        reader(Charset.forName(encoding)).buffered()

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

fun RandomAccessFile.clipBlock(offset: Long = filePointer, size: Long = length()): InputStream =
        RAFInputStream(this, offset, size)

class RAFInputStream
constructor(val source: RandomAccessFile,
            val offset: Long = source.filePointer,
            val size: Long = source.length()) : InputStream() {
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
        source.seek(curpos)
    }

    override fun read(): Int =
            if (curpos < endpos) {
                curpos++
                source.read()
            } else {
                -1
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

    override fun available(): Int = (endpos - curpos).toInt()
}
