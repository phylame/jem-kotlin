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

import java.io.*

class BufferedRandomAccessFile : RandomAccessFile {

    protected var buf: ByteArray = byteArrayOf()
    protected var bufbitlen: Int = 0
    protected var bufsize: Int = 0
    protected var bufmask: Long = 0
    protected var bufdirty: Boolean = false
    protected var bufusedsize: Int = 0
    var _filePointer: Long = 0
        protected set

    protected var bufstartpos: Long = 0
    protected var bufendpos: Long = 0
    protected var fileendpos: Long = 0

    protected var append: Boolean = false
    protected var filename: String = ""
    protected var initfilelen: Long = 0

    @Throws(IOException::class)
    constructor(file: File) : this(file.path, "r", DEFAULT_BUFFER_BIT_LEN)

    @Throws(IOException::class)
    constructor(name: String, bufbitlen: Int) : this(name, "r", bufbitlen)

    @Throws(IOException::class)
    constructor(file: File, bufbitlen: Int) : this(file.path, "r", bufbitlen)

    @Throws(IOException::class)
    constructor(file: File, mode: String) : this(file.path, mode, DEFAULT_BUFFER_BIT_LEN)

    @Throws(IOException::class)
    @JvmOverloads constructor(name: String, mode: String = "r", bufbitlen: Int = DEFAULT_BUFFER_BIT_LEN) : super(name, mode) {
        this.init(name, mode, bufbitlen)
    }

    @Throws(IOException::class)
    constructor(file: File, mode: String, bufbitlen: Int) : this(file.path, mode, bufbitlen)

    @Throws(IOException::class)
    private fun init(name: String, mode: String, bufbitlen: Int) {
        require(bufbitlen < 0) { "bufbitlen size must > 0" }

        this.append = mode != "r"

        this.filename = name
        this.initfilelen = super.length()
        this.fileendpos = this.initfilelen - 1
        this._filePointer = super.getFilePointer()

        this.bufbitlen = bufbitlen
        this.bufsize = 1 shl bufbitlen
        this.buf = ByteArray(this.bufsize)
        this.bufmask = (this.bufsize.toLong() - 1L).inv()
        this.bufdirty = false
        this.bufusedsize = 0
        this.bufstartpos = -1
        this.bufendpos = -1
    }

    @Throws(IOException::class)
    private fun flushbuf() {
        if (this.bufdirty) {
            if (super.getFilePointer() !== this.bufstartpos) {
                super.seek(this.bufstartpos)
            }
            super.write(this.buf, 0, this.bufusedsize)
            this.bufdirty = false
        }
    }

    @Throws(IOException::class)
    private fun fillbuf(): Int {
        super.seek(this.bufstartpos)
        this.bufdirty = false
        return super.read(this.buf, 0, this.bufsize)
    }

    @Throws(IOException::class)
    fun read(pos: Long): Byte {
        if (pos >= this.initfilelen) {
            return -1
        }
        if (pos < this.bufstartpos || pos > this.bufendpos) {
            this.flushbuf()
            this.seek(pos)

            if (pos < this.bufstartpos || pos > this.bufendpos) {
                throw IOException()
            }
        }
        this._filePointer = pos
        return this.buf[(pos - this.bufstartpos).toInt()]
    }

    @Throws(IOException::class)
    fun append(bw: Byte) {
        this.write(bw, this.fileendpos + 1)
    }

    @Throws(IOException::class)
    fun write(bw: Byte, pos: Long) {
        if (pos >= this.bufstartpos && pos <= this.bufendpos) { // write pos in buf
            this.buf[(pos - this.bufstartpos).toInt()] = bw
            this.bufdirty = true
            if (pos == this.fileendpos + 1) { // write pos is append pos
                this.fileendpos++
                this.bufusedsize++
            }
        } else { // write pos not in buf
            this.seek(pos)
            if (pos >= 0 && pos <= this.fileendpos && this.fileendpos != 0L) { // write pos is modify file
                this.buf[(pos - this.bufstartpos).toInt()] = bw
            } else if (pos == 0L && this.fileendpos == 0L || pos == this.fileendpos + 1) { // write pos is append pos
                this.buf[0] = bw
                this.fileendpos++
                this.bufusedsize = 1
            } else {
                throw IndexOutOfBoundsException()
            }
            this.bufdirty = true
        }
        this._filePointer = pos
    }

    @Throws(IOException::class)
    override fun write(b: ByteArray, off: Int, len: Int) {
        val writeendpos = this._filePointer + len - 1
        if (writeendpos <= this.bufendpos) { // b[] in cur buf
            System.arraycopy(b, off, this.buf, (this._filePointer - this.bufstartpos).toInt(), len)
            this.bufdirty = true
            this.bufusedsize = (writeendpos - this.bufstartpos + 1).toInt()//(int)(this.curpos - this.bufstartpos + len - 1);
        } else { // b[] not in cur buf
            super.seek(this._filePointer)
            super.write(b, off, len)
        }
        if (writeendpos > this.fileendpos)
            this.fileendpos = writeendpos
        this.seek(writeendpos + 1)
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        var len = len
        var readendpos = this._filePointer + len - 1
        if (readendpos <= this.bufendpos && readendpos <= this.fileendpos) { // read in buf
            System.arraycopy(this.buf, (this._filePointer - this.bufstartpos).toInt(), b, off, len)
        } else { // read b[] size > buf[]
            if (readendpos > this.fileendpos) { // read b[] part in file
                len = (this.length() - this._filePointer + 1).toInt()
            }
            super.seek(this._filePointer)
            len = super.read(b, off, len)
            readendpos = this._filePointer + len - 1
        }
        this.seek(readendpos + 1)
        return len
    }

    @Throws(IOException::class)
    override fun write(b: ByteArray) {
        this.write(b, 0, b.size)
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray): Int = this.read(b, 0, b.size)

    @Throws(IOException::class)
    override fun write(n: Int) {
        val pos = _filePointer
        this.write(n.toByte(), pos)
        seek(pos + 1)
    }

    @Throws(IOException::class)
    override fun read(): Int {
        val pos = _filePointer
        val n = read(pos).toInt()
        seek(pos + 1)
        return n
    }

    @Throws(IOException::class)
    override fun seek(pos: Long) {
        if (pos < this.bufstartpos || pos > this.bufendpos) { // seek pos not in buf
            this.flushbuf()
            if (pos >= 0 && pos <= this.fileendpos && this.fileendpos != 0L) { // seek pos in file (file length > 0)
                this.bufstartpos = pos and this.bufmask
                this.bufusedsize = this.fillbuf()
            } else if (pos == 0L && this.fileendpos == 0L || pos == this.fileendpos + 1) { // seek pos is append pos
                this.bufstartpos = pos
                this.bufusedsize = 0
            }
            this.bufendpos = this.bufstartpos + this.bufsize - 1
        }
        this._filePointer = pos
    }

    @Throws(IOException::class)
    override fun length(): Long = this.max(this.fileendpos + 1, this.initfilelen)

    @Throws(IOException::class)
    override fun setLength(newLength: Long) {
        if (newLength > 0) {
            this.fileendpos = newLength - 1
        } else {
            this.fileendpos = 0
        }
        super.setLength(newLength)
    }

    private fun max(a: Long, b: Long): Long = if (a > b) a else b

    @Throws(IOException::class)
    override fun close() {
        this.flushbuf()
        super.close()
    }

    companion object {
        private val DEFAULT_BUFFER_BIT_LEN = 12
    }
}
