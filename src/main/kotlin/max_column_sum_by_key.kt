import java.io.File
import java.io.RandomAccessFile
import java.util.HashMap
import java.util.concurrent.CompletableFuture

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

private fun MutableMap<String, IntBox>.mergeWith(
        other: Map<String, IntBox>): Map<String, IntBox> {
    for ((key, value) in other) {
        merge(key, value) { a, (int) -> a + int; a }
    }
    return this
}

const val delim = '\t'

private fun run(file: File, keyIndex: Int, valueIndex: Int) {
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

    val firstPartitionResult = CompletableFuture.supplyAsync {
        maxOfPartition(partition1, keyIndex, valueIndex, maxFieldIndex, secondPartitionStartIndex.toInt())
    }

    val secondPartitionResult = CompletableFuture.supplyAsync {
        maxOfPartition(partition2, keyIndex, valueIndex, maxFieldIndex, fileLength.toInt())
    }

    val sumByKey = firstPartitionResult.join().toMutableMap()
            .mergeWith(secondPartitionResult.join())

    val maxEntry = sumByKey.maxBy { it.value.int }

    if (maxEntry == null) {
        println("No entries")
    } else {
        val (key, value) = maxEntry
        println("max_key: $key, sum: ${value.int}")
    }
}

private fun maxOfPartition(partition: RandomAccessFile,
                           keyIndex: Int,
                           valueIndex: Int,
                           maxFieldIndex: Int,
                           lastByteIndex: Int): Map<String, IntBox> {
    val sumByKey = HashMap<String, IntBox>()

    var buffer = ByteArray(1024 * 1_000)
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

    var doneEarly = false

    while (true) {
        val bytesRemaining = 1 + lastByteIndex - partition.filePointer.toInt()

        if (bytesRemaining <= 0) {
            break
        }

        if (bytesRemaining < buffer.size) {
            buffer = ByteArray(bytesRemaining)
            doneEarly = true
        }

        val bytesCount = partition.read(buffer)

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

        if (doneEarly) {
            break // partition ended
        }
    }

    return sumByKey
}
