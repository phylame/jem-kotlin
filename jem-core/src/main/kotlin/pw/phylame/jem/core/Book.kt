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

import java.util.*

open class Chapter : Iterable<Chapter>, Cloneable {
    init {
        attributes = VariantMap()
    }

    constructor(title: String = "", text: Text = Texts.emptyText()) {
        attributes.put(Attributes.TITLE, title, false)
        this.text = text
    }

    constructor(title: String, cover: Flob, intro: Text, text: Text = Texts.emptyText()) {
        attributes.put(Attributes.TITLE, title, false)
        attributes.put(Attributes.COVER, cover, false)
        attributes.put(Attributes.INTRO, intro, false)
        this.text = text
    }

    constructor(chapter: Chapter) {
        chapter.dumpTo(this)
    }

    lateinit var attributes: VariantMap

    operator fun set(name: String, value: String) {
        attributes.put(name, value, false)
    }

    operator fun set(name: String, value: Any) {
        attributes.put(name, value, true)
    }

    operator fun get(name: String): Any? = attributes[name]

    var text: Text = Texts.emptyText()

    var parent: Chapter? = null
        protected set

    private var children: ArrayList<Chapter> = ArrayList()

    protected fun checkChapter(chapter: Chapter): Chapter {
        if (chapter.parent != null) {
            throw IllegalArgumentException("Chapter already in a certain section: $chapter")
        }
        if (chapter === this) {
            throw IllegalArgumentException("Cannot add self to sub chapter list: $chapter")
        }
        if (chapter === parent) {
            throw IllegalArgumentException("Cannot add parent chapter to its sub chapter list: $chapter")
        }
        return chapter
    }

    fun append(chapter: Chapter) {
        children.add(checkChapter(chapter))
        chapter.parent = this
    }

    fun insert(index: Int, chapter: Chapter) {
        children.add(index, checkChapter(chapter))
        chapter.parent = this
    }

    fun indexOf(chapter: Chapter): Int = if (chapter.parent !== this) -1 else children.indexOf(chapter)

    fun removeAt(index: Int): Chapter {
        val chapter = children.removeAt(index)
        chapter.parent = null
        return chapter
    }

    fun remove(chapter: Chapter): Boolean =
            if (chapter.parent !== this) {
                false
            } else if (children.remove(chapter)) {
                chapter.parent = null
                true
            } else {
                false
            }

    fun replace(index: Int, chapter: Chapter): Chapter {
        val previous = children.set(index, checkChapter(chapter))
        chapter.parent = this
        previous.parent = null
        return previous
    }

    operator fun set(index: Int, chapter: Chapter) {
        replace(index, chapter)
    }

    fun chapterAt(index: Int): Chapter = children[index]

    operator fun get(index: Int): Chapter = chapterAt(index)

    fun clear() {
        children.clear()
    }

    val size: Int get() = children.size

    fun isSection(): Boolean = !children.isEmpty()

    override fun iterator(): Iterator<Chapter> = children.iterator()

    private val cleaners: MutableSet<(Chapter) -> Unit> = HashSet()

    fun registerClean(clean: (Chapter) -> Unit) {
        cleaners.add(clean)
    }

    fun removeClean(clean: (Chapter) -> Unit) {
        cleaners.remove(clean)
    }

    open fun cleanup() {
        cleaners.forEach { it(this) }
        cleaners.clear()
        attributes.clear()
        children.forEach { it.cleanup() }
        children.clear()
        cleaned = true
    }

    private var cleaned = false

    protected fun finalize() {
        if (!cleaned) {
            System.err.printf("*** BUG: Chapter \"$title@${hashCode()}\" not cleaned ***\n")
        }
    }

    override fun clone(): Chapter {
        val result = super.clone() as Chapter
        dumpTo(result)
        return result
    }

    @Suppress("UNCHECKED_CAST")
    open fun dumpTo(chapter: Chapter) {
        chapter.text = text
        chapter.attributes = attributes.clone()
        chapter.children = children.clone() as ArrayList<Chapter>
    }

    open val description: String
        get() = "${javaClass.simpleName}@${hashCode()}: attributes@${attributes.hashCode()}:$attributes" +
                ", text@${text.hashCode()}: $text"

    override fun toString(): String = title
}

open class Book : Chapter {
    constructor(title: String = "", author: String = "") : super(title) {
        attributes.put(Attributes.AUTHOR, author, false)
    }

    constructor(chapter: Chapter) {
        chapter.dumpTo(this)
    }

    constructor(book: Book) {
        book.dumpTo(this)
    }

    var extensions: VariantMap = VariantMap()

    override fun cleanup() {
        extensions.clear()
        super.cleanup()
    }

    override fun dumpTo(chapter: Chapter) {
        super.dumpTo(chapter)
        if (chapter is Book) {
            chapter.extensions = extensions.clone()
        }
    }

    override val description: String
        get() = "${super.description}, extensions@${extensions.hashCode()}: $extensions"
}
