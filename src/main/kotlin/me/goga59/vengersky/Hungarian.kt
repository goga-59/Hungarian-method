package me.goga59.vengersky

// matchedColumns[row] - столбец, выбранный для строки row
data class Matching(val matchedColumns: List<Int>, val totalWeight: Long)

// Возвращает полное паросочетание или null, если его не существует
fun hungarian(weights: List<List<Int?>>, maximize: Boolean = false): Matching? {
    val size = weights.size
    require(size > 0 && weights.all { it.size == size }) { "Weight matrix must be nonempty and square" }

    // Алгоритм решает задачу минимизации, поэтому для максимизации меняем знак весов: max(weight) = min(-weight)
    fun cost(weight: Int): Long = if (maximize) -weight.toLong() else weight.toLong()

    val rowPotential = LongArray(size + 1) // Потенциалы строк
    val columnPotential = LongArray(size + 1) // Потенциалы столбцов

    // matchedRowByColumn[column] = строка, назначенная этому столбцу
    val matchedRowByColumn = IntArray(size + 1)

    // Запоминает предыдущий столбец в цепи
    val previousColumn = IntArray(size + 1)

    // Начальные потенциалы строк
    for (row in 1..size) {
        rowPotential[row] = weights[row - 1].filterNotNull().minOfOrNull(::cost) ?: return null
    }

    for (newRow in 1..size) {
        // Индекс 0 - служебный
        matchedRowByColumn[0] = newRow

        // minCostToColumn[column] - минимальная приведённая стоимость для столбца column
        val minCostToColumn = LongArray(size + 1) { Long.MAX_VALUE }

        var currentColumn = 0
        val visitedColumns = BooleanArray(size + 1)

        do {
            visitedColumns[currentColumn] = true

            val currentRow = matchedRowByColumn[currentColumn]
            var minDelta = Long.MAX_VALUE
            var nextColumn = 0

            for (column in 1..size) {
                if (visitedColumns[column]) continue

                val weight = weights[currentRow - 1][column - 1]

                if (weight != null) {
                    val reducedCost = cost(weight) - rowPotential[currentRow] - columnPotential[column]

                    if (reducedCost < minCostToColumn[column]) {
                        minCostToColumn[column] = reducedCost
                        previousColumn[column] = currentColumn
                    }
                }

                if (minCostToColumn[column] < minDelta) {
                    minDelta = minCostToColumn[column]
                    nextColumn = column
                }
            }

            // Если ни до одного нового столбца добраться невозможно, то полного паросочетания нет
            if (minDelta == Long.MAX_VALUE) return null

            // Корректируем потенциалы
            for (column in 0..size) {
                if (visitedColumns[column]) {
                    rowPotential[matchedRowByColumn[column]] += minDelta
                    columnPotential[column] -= minDelta
                } else if (minCostToColumn[column] != Long.MAX_VALUE) {
                    minCostToColumn[column] -= minDelta
                }
            }

            currentColumn = nextColumn

            // Если столбец свободен поиск заканчивается, иначе пытаемся переставить уже назначенную строку
        } while (matchedRowByColumn[currentColumn] != 0)

        // Свободный столбец найден - идём назад и перестраиваем назначения
        do {
            val previous = previousColumn[currentColumn]
            matchedRowByColumn[currentColumn] = matchedRowByColumn[previous]
            currentColumn = previous
        } while (currentColumn != 0)
    }

    val matchedColumns = IntArray(size)
    for (column in 1..size) matchedColumns[matchedRowByColumn[column] - 1] = column - 1
    val totalWeight = matchedColumns.indices.sumOf { row -> weights[row][matchedColumns[row]]!!.toLong() }
    return Matching(matchedColumns.toList(), totalWeight)
}
