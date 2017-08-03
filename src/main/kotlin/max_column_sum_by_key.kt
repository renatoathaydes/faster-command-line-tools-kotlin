import java.io.File
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

const val delim = '\t'.toByte()

fun ByteArray.toIntUpTo(maxIndex: Int): Int {
    var result = 0
    var multiplier = 1
    ((maxIndex - 1) downTo 0).forEach { index ->
        val digit = this[index].toInt() - 48
        result += digit * multiplier
        multiplier *= 10
    }
    return result
}

private fun run(file: File, keyIndex: Int, valueIndex: Int) {
    val maxFieldIndex = maxOf(keyIndex, valueIndex)
    val sumByKey = HashMap<String, IntBox>()

    val inputStream = file.inputStream()

    val buffer = ByteArray(1024 * 1_000)
    var fieldIndex = 0
    val currentKey = ByteArray(12)
    val currentVal = ByteArray(12)
    var currentKeyMaxIndex = 0
    var currentValMaxIndex = 0

    fun startLine() {
        if (currentValMaxIndex > 0) {
            val key = String(currentKey, 0, currentKeyMaxIndex)
            var box = sumByKey[key]
            if (box == null) {
                box = IntBox()
                sumByKey[key] = box
            }
            box + currentVal.toIntUpTo(currentValMaxIndex)
        }

        fieldIndex = 0
        currentKeyMaxIndex = 0
        currentValMaxIndex = 0
    }

    inputStream.use {
        while (true) {
            val bytesCount = inputStream.read(buffer)

            if (bytesCount < 0) {
                break
            }

            (0 until bytesCount).forEach { i ->
                val byte = buffer[i]

                if (fieldIndex <= maxFieldIndex) {
                    when (byte) {
                        delim -> {
                            fieldIndex++
                        }
                        '\n'.toByte() -> {
                            startLine()
                        }
                        else -> {
                            if (fieldIndex == keyIndex) {
                                currentKey[currentKeyMaxIndex++] = byte
                            } else if (fieldIndex == valueIndex) {
                                currentVal[currentValMaxIndex++] = byte
                            }
                        }
                    }
                } else if (byte == '\n'.toByte()) {
                    startLine()
                }
            }
        }
    }

    if (sumByKey.isEmpty()) {
        println("No entries")
    } else {
        val (key, maxEntry) = sumByKey.maxBy { it.value.int }!!
        println("max_key: $key, sum: ${maxEntry.int}")
    }
}

