package me.goga59.vengersky

import java.io.IOException
import java.io.Reader
import kotlin.io.path.Path
import kotlin.io.path.bufferedReader
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.writeText
import kotlin.system.exitProcess

fun readWeights(reader: Reader): List<List<Int?>> {
    val lines = reader.readLines().filter { it.isNotBlank() }
    val n = lines.firstOrNull()?.trim()?.toIntOrNull()
    require(n != null && n > 0) { "First line must contain a positive integer n" }
    require(lines.size - 1 == n) { "Expected $n matrix rows, got ${lines.size - 1}" }

    return lines.drop(1).mapIndexed { row, line ->
        val tokens = line.trim().split(Regex("\\s+"))
        require(tokens.size == n) { "Row ${row + 1}: expected $n weights, got ${tokens.size}" }

        tokens.mapIndexed { column, token ->
            if (token == "x") null else {
                requireNotNull(token.toIntOrNull()) {
                    "Row ${row + 1}, column ${column + 1}: expected an Int weight or 'x', got '$token'"
                }
            }
        }
    }
}

fun main(args: Array<String>) {
    try {
        require(args.size <= 2) { "Usage: vengersky [path-to-file.txt] [min|max]" }

        val input = Path(args.getOrElse(0) { "examples/small.txt" })
        val mode = args.getOrElse(1) { "min" }
        require(mode == "min" || mode == "max") { "Mode must be 'min' or 'max'" }

        val weights = input.bufferedReader().use(::readWeights)
        val matching = hungarian(weights, maximize = mode == "max")
        val report = createReport(input.toString().replace('\\', '/'), mode, weights, matching)

        val output = Path("results") / "${input.nameWithoutExtension}-$mode.txt"
        output.parent.createDirectories()
        output.writeText(report)

        print(report)
        println("Result: $output")
    } catch (ex: IllegalArgumentException) {
        System.err.println("Error: ${ex.message}")
        exitProcess(1)
    } catch (ex: IOException) {
        System.err.println("File error: ${ex.message}")
        exitProcess(1)
    }
}

private fun createReport(input: String, mode: String, weights: List<List<Int?>>, matching: Matching?): String =
    buildString {
        appendLine("Input: $input")
        appendLine("Mode: ${if (mode == "min") "minimum" else "maximum"}")

        appendLine("Weight matrix:")
        val cellWidth = maxOf("R${weights.size}".length, weights.flatten().maxOf { (it?.toString() ?: "x").length })

        val rowLabelWidth = "L${weights.size}".length
        appendLine(" ".repeat(rowLabelWidth + 1) + weights.indices.joinToString(" ") { "R${it + 1}".padStart(cellWidth) })

        weights.forEachIndexed { rowIndex, row ->
            appendLine("L${rowIndex + 1}".padStart(rowLabelWidth) + " " + row.joinToString(" ") {
                (it?.toString() ?: "x").padStart(cellWidth)
            })
        }

        appendLine()

        if (matching == null) {
            appendLine("No perfect matching exists.")
        } else {
            appendLine("Matching:")

            matching.matchedColumns.forEachIndexed { row, column ->
                appendLine("L${row + 1} -> R${column + 1} (weight: ${weights[row][column]})")
            }

            appendLine("Optimal weight: ${matching.totalWeight}")
        }
    }
