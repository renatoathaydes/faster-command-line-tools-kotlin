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

const val delim = '\t'

private fun run(file: File, keyIndex: Int, valueIndex: Int) {
    val maxFieldIndex = maxOf(keyIndex, valueIndex)
    val sumByKey = HashMap<String, IntBox>()

    val inputStream = file.inputStream()

    val buffer = ByteArray(1024 * 1_000)
    var fieldIndex = 0
    val currentKey = StringBuilder(12)
    val currentVal = StringBuilder(12)

    fun startLine() {
        if (currentVal.isNotEmpty()) {
            sumByKey.getOrPut(currentKey.toString()) { IntBox() } + currentVal.toString().toInt()
        }

        fieldIndex = 0
        currentKey.setLength(0)
        currentVal.setLength(0)
    }

    inputStream.use {
        while (true) {
            val bytesCount = inputStream.read(buffer)

            if (bytesCount < 0) {
                break
            }

            (0 until bytesCount).forEach { i ->
                val char = buffer[i].toChar()

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
                                currentVal.append(char)
                            }
                        }
                    }
                } else if (char == '\n') {
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

