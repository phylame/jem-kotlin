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

class Attributes {
    companion object {
        const val AUTHOR = "author"

        const val BINDING = "binding"

        const val COVER = "cover"

        const val DATE = "date"

        const val GENRE = "genre"

        const val INTRO = "intro"

        const val ISBN = "isbn"

        const val KEYWORDS = "keywords"

        const val LANGUAGE = "language"

        const val PAGES = "pages"

        const val PRICE = "price"

        const val PROTAGONIST = "protagonist"

        const val PUBDATE = "pubdate"

        const val PUBLISHER = "publisher"

        const val RIGHTS = "rights"

        const val SERIES = "series"

        const val STATE = "state"

        const val TITLE = "title"

        const val TRANSLATOR = "translator"

        const val VENDOR = "vendor"

        const val WORDS = "words"
    }
}

var Chapter.author: String
    get() = attributes.get(Attributes.AUTHOR, "")
    set(value) {
        this[Attributes.AUTHOR] = value
    }

var Chapter.binding: String
    get() = attributes.get(Attributes.BINDING, "")
    set(value) {
        this[Attributes.BINDING] = value
    }

var Chapter.cover: Blob?
    get() = attributes.get(Attributes.COVER, Blob::class.java)
    set(value) {
        this[Attributes.COVER] = value
    }

var Chapter.date: Date?
    get() = attributes.get(Attributes.DATE, Date::class.java)
    set(value) {
        this[Attributes.DATE] = value
    }

var Chapter.genre: String
    get() = attributes.get(Attributes.GENRE, "")
    set(value) {
        this[Attributes.GENRE] = value
    }

var Chapter.intro: Text?
    get() = attributes.get(Attributes.INTRO, Text::class.java)
    set(value) {
        this[Attributes.INTRO] = value
    }

var Chapter.isbn: String
    get() = attributes.get(Attributes.ISBN, "")
    set(value) {
        this[Attributes.ISBN] = value
    }

var Chapter.keywords: String
    get() = attributes.get(Attributes.KEYWORDS, "")
    set(value) {
        this[Attributes.KEYWORDS] = value
    }

var Chapter.language: Locale?
    get() = attributes.get(Attributes.LANGUAGE, Locale::class.java)
    set(value) {
        this[Attributes.LANGUAGE] = value
    }

var Chapter.pages: Int?
    get() = attributes.get(Attributes.PAGES, Int::class.java)
    set(value) {
        this[Attributes.PAGES] = value
    }

var Chapter.price: Double?
    get() = attributes.get(Attributes.PRICE, Double::class.java)
    set(value) {
        this[Attributes.PRICE] = value
    }

var Chapter.protagonist: String
    get() = attributes.get(Attributes.PROTAGONIST, "")
    set(value) {
        this[Attributes.PROTAGONIST] = value
    }

var Chapter.pubdate: Date?
    get() = attributes.get(Attributes.PUBDATE, Date::class.java)
    set(value) {
        this[Attributes.PUBDATE] = value
    }

var Chapter.publisher: String
    get() = attributes.get(Attributes.PUBLISHER, "")
    set(value) {
        this[Attributes.PUBLISHER] = value
    }

var Chapter.rights: String
    get() = attributes.get(Attributes.RIGHTS, "")
    set(value) {
        this[Attributes.RIGHTS] = value
    }

var Chapter.series: String
    get() = attributes.get(Attributes.SERIES, "")
    set(value) {
        this[Attributes.SERIES] = value
    }

var Chapter.state: String
    get() = attributes.get(Attributes.STATE, "")
    set(value) {
        this[Attributes.STATE] = value
    }

var Chapter.title: String
    get() = attributes.get(Attributes.TITLE, "")
    set(value) {
        this[Attributes.TITLE] = value
    }

var Chapter.translator: String
    get() = attributes.get(Attributes.TRANSLATOR, "")
    set(value) {
        this[Attributes.TRANSLATOR] = value
    }

var Chapter.vendor: String
    get() = attributes.get(Attributes.VENDOR, "")
    set(value) {
        this[Attributes.VENDOR] = value
    }

var Chapter.words: Int?
    get() = attributes.get(Attributes.WORDS, Int::class.java)
    set(value) {
        this[Attributes.WORDS] = value
    }

const val PMAB = "pmab"

fun Chapter.locate(indices: IntArray): Chapter {
    var index: Int
    var chapter = this
    for (i in indices) {
        index = if (i < 0) chapter.size + i else i
        chapter = chapter[index]
    }
    return chapter
}

val Chapter.depth: Int
    get() {
        if (!isSection()) {
            return 0
        }
        var depth = 0
        var d: Int
        for (sub in this) {
            d = sub.depth
            if (d > depth) {
                depth = d
            }
        }
        return depth + 1
    }

interface ChapterFilter {
    fun accept(chapter: Chapter): Boolean
}

fun Chapter.find(filter: ChapterFilter, from: Int, recursion: Boolean = false): Chapter? {
    var sub: Chapter?
    for (ix in from..size - 1) {
        sub = chapterAt(ix)
        if (filter.accept(sub)) {
            return sub
        }
        if (recursion && sub.isSection()) {
            sub = sub.find(filter, 0, true)
            if (sub != null) {
                return sub
            }
        }
    }
    return null
}

fun Chapter.select(filter: ChapterFilter, result: MutableList<Chapter>, limit: Int, recursion: Boolean = false): Int {
    var count = 0
    for (sub in this) {
        if (filter.accept(sub)) {
            result.add(sub)
            if (++count == limit) {
                break
            }
        }
        if (recursion && sub.isSection()) {
            count += sub.select(filter, result, limit, true)
        }
    }
    return count
}
