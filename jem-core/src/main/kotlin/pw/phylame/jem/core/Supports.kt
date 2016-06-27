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

enum class Attributes(val value: String) : CharSequence by value {
    AUTHOR("author"),

    BINDING("binding"),

    COVER("cover"),

    DATE("date"),

    GENRE("genre"),

    INTRO("intro"),

    ISBN("isbn"),

    KEYWORDS("keywords"),

    LANGUAGE("language"),

    PAGES("pages"),

    PRICE("price"),

    PROTAGONIST("protagonist"),

    PUBDATE("pubdate"),

    PUBLISHER("publisher"),

    RIGHTS("rights"),

    SERIES("series"),

    STATE("state"),

    TITLE("title"),

    TRANSLATOR("translator"),

    VENDOR("vendor"),

    WORDS("words")
}

private fun Chapter.put(name: CharSequence, value: Any?) {
    attributes.put(name, requireNotNull(value) {
        "Attribute $name require not null"
    }, false)
}

var Chapter.author: String
    get() = attributes.get(Attributes.AUTHOR, "")
    set(value) {
        put(Attributes.AUTHOR, value)
    }

var Chapter.binding: String
    get() = attributes.get(Attributes.BINDING, "")
    set(value) {
        put(Attributes.BINDING, value)
    }

var Chapter.cover: Blob?
    get() = attributes.get<Blob?>(Attributes.COVER, null)
    set(value) {
        put(Attributes.COVER, value)
    }

var Chapter.date: Date?
    get() = attributes.get<Date?>(Attributes.DATE, null)
    set(value) {
        put(Attributes.DATE, value)
    }

var Chapter.genre: String
    get() = attributes.get(Attributes.GENRE, "")
    set(value) {
        put(Attributes.GENRE, value)
    }

var Chapter.intro: Text?
    get() = attributes.get<Text?>(Attributes.INTRO, null)
    set(value) {
        put(Attributes.INTRO, value)
    }

var Chapter.isbn: String
    get() = attributes.get(Attributes.ISBN, "")
    set(value) {
        put(Attributes.ISBN, value)
    }

var Chapter.keywords: String
    get() = attributes.get(Attributes.KEYWORDS, "")
    set(value) {
        put(Attributes.KEYWORDS, value)
    }

var Chapter.language: Locale?
    get() = attributes.get<Locale?>(Attributes.LANGUAGE, null)
    set(value) {
        put(Attributes.LANGUAGE, value)
    }

var Chapter.pages: Int?
    get() = attributes.get<Int?>(Attributes.PAGES, null)
    set(value) {
        put(Attributes.PAGES, value)
    }

var Chapter.price: Double?
    get() = attributes.get<Double?>(Attributes.PRICE, null)
    set(value) {
        put(Attributes.PRICE, value)
    }

var Chapter.protagonist: String
    get() = attributes.get(Attributes.PROTAGONIST, "")
    set(value) {
        put(Attributes.PROTAGONIST, value)
    }

var Chapter.pubdate: Date?
    get() = attributes.get<Date?>(Attributes.PUBDATE, null)
    set(value) {
        put(Attributes.PUBDATE, value)
    }

var Chapter.publisher: String
    get() = attributes.get(Attributes.PUBLISHER, "")
    set(value) {
        put(Attributes.PUBLISHER, value)
    }

var Chapter.rights: String
    get() = attributes.get(Attributes.RIGHTS, "")
    set(value) {
        put(Attributes.RIGHTS, value)
    }

var Chapter.series: String
    get() = attributes.get(Attributes.SERIES, "")
    set(value) {
        put(Attributes.SERIES, value)
    }

var Chapter.state: String
    get() = attributes.get(Attributes.STATE, "")
    set(value) {
        put(Attributes.STATE, value)
    }

var Chapter.title: String
    get() = attributes.get(Attributes.TITLE, "")
    set(value) {
        put(Attributes.TITLE, value)
    }

var Chapter.translator: String
    get() = attributes.get(Attributes.TRANSLATOR, "")
    set(value) {
        put(Attributes.TRANSLATOR, value)
    }

var Chapter.vendor: String
    get() = attributes.get(Attributes.VENDOR, "")
    set(value) {
        put(Attributes.VENDOR, value)
    }

var Chapter.words: Int?
    get() = attributes.get<Int?>(Attributes.WORDS, null)
    set(value) {
        put(Attributes.WORDS, value)
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

fun Chapter.find(from: Int, recursion: Boolean = false, predicate: (Chapter) -> Boolean): Chapter? {
    var sub: Chapter?
    for (ix in from..size - 1) {
        sub = chapterAt(ix)
        if (predicate(sub)) {
            return sub
        }
        if (recursion && sub.isSection()) {
            sub = sub.find(0, true, predicate)
            if (sub != null) {
                return sub
            }
        }
    }
    return null
}

fun Chapter.select(result: MutableList<Chapter>, limit: Int, recursion: Boolean = false,
                   predicate: (Chapter) -> Boolean): Int {
    var count = 0
    for (sub in this) {
        if (predicate(sub)) {
            result.add(sub)
            if (count++ == limit) {
                break
            }
        }
        if (recursion && sub.isSection()) {
            count += sub.select(result, limit, true, predicate)
        }
    }
    return count
}
