import java.io.File


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

val delim = '\t'

private fun run(file: File, keyIndex: Int, valueIndex: Int) {
    val maxFieldIndex = maxOf(keyIndex, valueIndex)
    val sumByKey = mutableMapOf<String, Int>()

    file.forEachLine { line ->
        val fields = line.split(delim)

        if (maxFieldIndex < fields.size) {
            sumByKey.merge(fields[keyIndex], fields[valueIndex].toInt(), Int::plus)
        }
    }

    if (sumByKey.isEmpty()) {
        println("No entries")
    } else {
        val (key, maxEntry) = sumByKey.maxBy { it.value }!!
        println("max_key: $key, sum: $maxEntry")
    }
}
