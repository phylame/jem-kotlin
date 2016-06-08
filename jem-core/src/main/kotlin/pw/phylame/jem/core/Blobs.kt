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
import java.util.zip.ZipFile

interface Blob {
    val name: String

    val mime: String

    fun openStream(): InputStream

    val bytes: ByteArray get() = openStream().readBytes()

    fun writeTo(out: OutputStream): Long {
        openStream().buffered().use {
            return it.copyTo(out)
        }
    }
}

abstract class AbstractBlob(private val _mime: String?) : Blob {
    override val mime: String by lazy {
        return@lazy Paths.mimeOrDetect(name, _mime)
    }

    override fun toString(): String = "$name;mime=$mime"
}

class FileBlob internal constructor(val file: File, mime: String?) : AbstractBlob(mime) {
    init {
        if (!file.exists()) {
            throw FileNotFoundException("No such file: $file")
        }
        if (file.isDirectory) {
            throw IllegalArgumentException("Require file but found directory: $file")
        }
    }

    override val name: String = file.path

    override fun openStream(): InputStream = file.inputStream()
}

class ZipBlob internal constructor(val zip: ZipFile, val entry: String, mime: String?) : AbstractBlob(mime) {
    init {
        if (zip.getEntry(entry) == null) {
            throw IOException("No such entry in ZIP: $entry")
        }
    }

    override val name: String = entry

    override fun openStream(): InputStream = zip.getInputStream(zip.getEntry(entry))

    override fun toString(): String = "zip://${zip.name}!${super.toString()}"
}

class BlockBlob
internal constructor(override val name: String, val file: RandomAccessFile, var offset: Long, var size: Long,
                     mime: String?) : AbstractBlob(mime) {
    init {
        if (size > (file.length() - offset)) {
            throw IndexOutOfBoundsException(
                    "available size of file(${file.length()}) < offset($offset) + size($size)")
        }
    }

    override fun openStream(): InputStream {
        file.seek(offset)
        return RAFInputStream(file, size)
    }

    override val bytes: ByteArray
        get() {
            file.seek(offset)
            val buf = ByteArray(size.toInt())
            val n = file.read(buf)
            if (n < size) {
                return buf.copyOf(n)
            } else {
                return buf
            }
        }

    override fun writeTo(out: OutputStream): Long {
        file.seek(offset)
        return file.copyTo(out, size.toInt())
    }

    override fun toString(): String = "block://${super.toString()};offset=$offset;size=$size"
}

class URLBlob internal constructor(val url: URL, mime: String?) : AbstractBlob(mime) {
    override val name: String = url.path

    override fun openStream(): InputStream = url.openStream()

    override fun toString(): String = "${url.toString()};mime=$mime"
}

class BytesBlob
internal constructor(override val name: String, val data: ByteArray, mime: String?) : AbstractBlob(mime) {
    override fun openStream(): InputStream = bytes.inputStream()

    override val bytes: ByteArray get() = data.copyOf(data.size)

    override fun writeTo(out: OutputStream): Long {
        out.write(data)
        return data.size.toLong()
    }

    override fun toString(): String = "bytes://${super.toString()}"
}

class Blobs {
    companion object {
        fun forFile(file: File, mime: String? = null): Blob = FileBlob(file, mime)

        fun forZip(zip: ZipFile, entry: String, mime: String? = null): Blob = ZipBlob(zip, entry, mime)

        fun forBlock(name: String, file: RandomAccessFile, offset: Long, size: Long, mime: String? = null):
                BlockBlob = BlockBlob(name, file, offset, size, mime)

        fun forURL(url: URL, mime: String? = null): Blob = URLBlob(url, mime)

        fun forBytes(name: String, data: ByteArray, mime: String? = null): Blob = BytesBlob(name, data, mime)

        fun emptyFile(mime: String? = null): Blob = forBytes("_empty_", byteArrayOf(0), mime)
    }
}
