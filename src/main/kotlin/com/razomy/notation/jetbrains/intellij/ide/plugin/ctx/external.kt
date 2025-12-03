package com.razomy.notation.jetbrains.intellij.ide.plugin.ctx

interface WithString {
    var string: String
}


typealias Pipe<T, R> = (T, R) -> R
typealias SinglePipe<T> = (T) -> T

open class Exception(message: String) : kotlin.Exception(message)
open class ArgumentException(message: String, val context: Any? = null) : Exception(message)
class UnknownTypeArgumentException(val type: Any?) : Exception("Unknown type: $type")
class MultipleException(val exceptions: List<Exception>) : Exception("Multiple exceptions occurred")


class ValueRecursiveDict : HashMap<String, String?>()
typealias ValueRecursiveDictOrValue<T> = Any


fun <T> List<T>.at(index: Int): T? {
    return if (index < 0) {
        if (this.size + index >= 0) this[this.size + index] else null
    } else {
        if (index < this.size) this[index] else null
    }
}

fun <T> MutableList<T>.last(): T = this[this.lastIndex]
fun <T> MutableList<T>.push(item: T) = this.add(item)
fun <T> MutableList<T>.pop(): T = this.removeAt(this.lastIndex)
