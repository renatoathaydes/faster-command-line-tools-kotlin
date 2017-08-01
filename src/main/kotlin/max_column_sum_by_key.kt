import com.athaydes.osgiaas.cli.CommandHelper
import com.athaydes.osgiaas.cli.args.ArgsSpec
import org.apache.felix.shell.Command
import org.osgi.service.component.annotations.Component
import java.io.File
import java.io.PrintStream

@Component
class MaxColumnSumByKeyCmd : Command {

    val spec = ArgsSpec.builder()
            .accepts("file")
            .end()
            .accepts("key_field_index")
            .end()
            .accepts("value_field_index")
            .end()
            .build()

    override fun getUsage() = "$name\n${spec.documentation}"

    override fun getName() = "max_column_sum_by_key"

    override fun getShortDescription() = "See " +
            "http://dlang.org/blog/2017/05/24/faster-command-line-tools-in-d/ for details."

    override fun execute(line: String, out: PrintStream, err: PrintStream) {
        val startTime = System.currentTimeMillis()
        val arguments = CommandHelper.breakupArguments(line) - name

        when (arguments.size) {
            3 -> {
                val fileName = arguments[0]
                val file = File(fileName)
                if (!file.isFile) {
                    return CommandHelper.printError(err, usage,
                            "Not a file: ${file.absolutePath}")
                }

                val keyIndex = arguments[1].toIntOrNull() ?:
                        return CommandHelper.printError(err, usage,
                                "keyIndex is not a valid int: ${arguments[1]}")

                val valueIndex = arguments[2].toIntOrNull() ?:
                        return CommandHelper.printError(err, usage,
                                "valueIndex is not a valid int: ${arguments[2]}")

                run(file, keyIndex, valueIndex)

                fun Double.format(digits: Int) = java.lang.String.format("%.${digits}f", this)
                println("Total time: ${((System.currentTimeMillis() - startTime) / 1000.0).format(2)} sec")
            }
            else -> CommandHelper.printError(err, usage,
                    "Wrong number of arguments")
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

    data class IntBox(var value: Int) {
        fun add(other: Int) {
            value += other
        }
    }

}

fun main(args: Array<String>) {
    MaxColumnSumByKeyCmd().execute(args.joinToString(" "), System.out, System.err)
}