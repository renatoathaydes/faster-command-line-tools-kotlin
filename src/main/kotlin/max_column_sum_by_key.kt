@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import java.io.File
import java.io.RandomAccessFile
import java.util.HashMap

fun main(arguments: Array<String>) {
    when (arguments.size) {
        3 -> {
            val fileName = arguments[0]
            val file = File(fileName)
            if (!file.isFile) {
                return System.err.println("Not a file: ${file.absolutePath}")
            }

            val keyIndex = arguments[1].toIntOrNull() ?:
                    return System.err.println("keyIndex is not a valid int: ${arguments[1]}")

            val valueIndex = arguments[2].toIntOrNull() ?:
                    return System.err.println("valueIndex is not a valid int: ${arguments[2]}")

            val startTime = System.currentTimeMillis()

            run(file, keyIndex, valueIndex)

            fun Double.format(digits: Int) = java.lang.String.format("%.${digits}f", this)
            println("Total time: ${((System.currentTimeMillis() - startTime) / 1000.0).format(2)} sec")
        }
        else -> System.err.println("Wrong number of arguments")
    }
}

data class IntBox(var int: Int = 0) {
    operator fun plus(other: Int) {
        int += other
    }
}

private fun ByteArray.toIntUpTo(maxIndex: Int): Int {
    var result = 0
    var multiplier = 1
    ((maxIndex - 1) downTo 0).forEach { index ->
        val digit = this[index].toInt() - 48
        result += digit * multiplier
        multiplier *= 10
    }
    return result
}


const val delim = '\t'

private fun run(file: File, keyIndex: Int, valueIndex: Int) = runBlocking {
    val maxFieldIndex = maxOf(keyIndex, valueIndex)

    val partition1 = RandomAccessFile(file, "r")
    val partition2 = RandomAccessFile(file, "r")

    val fileLength = file.length()
    var secondPartitionStartIndex = fileLength / 2L

    partition2.seek(secondPartitionStartIndex)

    while (true) {
        val byte = partition2.read()
        secondPartitionStartIndex++
        if (byte < 0 || byte.toChar() == '\n') {
            break
        }
    }

    val firstPartitionMaxEntry = async(CommonPool) {
        maxOfPartition(partition1, keyIndex, valueIndex, maxFieldIndex, secondPartitionStartIndex.toInt())
    }

    val secondPartitionMaxEntry = async(CommonPool) {
        maxOfPartition(partition2, keyIndex, valueIndex, maxFieldIndex, fileLength.toInt())
    }

    val maxEntry = listOf(firstPartitionMaxEntry, secondPartitionMaxEntry)
            .map { it.await() }
            .maxBy { it.component2().int }

    if (maxEntry == null || maxEntry.first.isEmpty()) {
        println("No entries")
    } else {
        val (key, value) = maxEntry
        println("max_key: $key, sum: $value")
    }
}

private suspend fun maxOfPartition(partition: RandomAccessFile,
                                   keyIndex: Int,
                                   valueIndex: Int,
                                   maxFieldIndex: Int,
                                   lastByteIndex: Int): Pair<String, IntBox> {
    val sumByKey = HashMap<String, IntBox>()

    val buffer = ByteArray(1024 * 1_000)
    var fieldIndex = 0
    val currentKey = StringBuilder(12)
    val currentVal = ByteArray(12)
    var currentValMaxIndex = 0

    fun startLine() {
        if (currentVal.isNotEmpty()) {
            sumByKey.getOrPut(currentKey.toString()) { IntBox() } + currentVal.toIntUpTo(currentValMaxIndex)
        }

        fieldIndex = 0
        currentKey.setLength(0)
        currentValMaxIndex = 0
    }

    while (true) {
        val bytesCount = partition.read(buffer)

        if (bytesCount < 0) {
            break
        }

        (0 until bytesCount).forEach { i ->
            val byte = buffer[i]
            val char = byte.toChar()

            if (fieldIndex <= maxFieldIndex) {
                when (char) {
                    delim -> {
                        fieldIndex++
                    }
                    '\n' -> {
                        startLine()
                    }
                    else -> {
                        if (fieldIndex == keyIndex) {
                            currentKey.append(char)
                        } else if (fieldIndex == valueIndex) {
                            currentVal[currentValMaxIndex++] = byte
                        }
                    }
                }
            } else if (char == '\n') {
                startLine()
            }
        }

        if (partition.filePointer.toInt() + buffer.size >= lastByteIndex) {
            println("Exiting early at position ${partition.filePointer}")
            break // we might have read a few too many bytes, but by now we can definitely exit
        }
    }

    return sumByKey.maxBy { it.value.int }?.toPair() ?: ("" to IntBox())
}

