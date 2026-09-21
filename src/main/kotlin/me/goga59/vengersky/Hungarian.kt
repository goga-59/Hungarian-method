package me.goga59.vengersky

// matchedColumns[row] - столбец, выбранный для строки row
data class Matching(val matchedColumns: List<Int>, val totalWeight: Long)

// Возвращает полное паросочетание или null, если его не существует
fun hungarian(weights: List<List<Int?>>, maximize: Boolean = false): Matching? {
    val size = weights.size
    require(size > 0 && weights.all { it.size == size }) { "Weight matrix must be nonempty and square" }

    // Алгоритм непосредственно решает задачу минимизации,
    // поэтому для максимизации меняем знак весов: max(weight) = min(-weight)
    fun cost(weight: Int): Long = if (maximize) -weight.toLong() else weight.toLong()

    // В каждом массиве индекс 0 - служебный, поэтому каждый массив длинной size+1

    // Благодаря им не приходится каждый раз менять всю матрицу
    val rowPotential = LongArray(size + 1) // Потенциалы строк
    val columnPotential = LongArray(size + 1) // Потенциалы столбцов

    // matchedRowByColumn[column] = row - показывает, какая строка сейчас назначена столбцу
    val matchedRowByColumn = IntArray(size + 1)

    // Запоминает предыдущий столбец в цепи
    val previousColumn = IntArray(size + 1)

    // Начальные потенциалы строк
    for (row in 1..size) {
        // Для каждой строки берём минимальный существующий вес
        rowPotential[row] = weights[row - 1].filterNotNull().minOfOrNull(::cost) ?: return null
    }

    // Последовательно добавляем в паросочетание каждую строку
    for (newRow in 1..size) {
        // Служебный столбец 0. Через него начинается поиск места для очередной строки
        matchedRowByColumn[0] = newRow

        // minCostToColumn[column] - минимальная найденная стоимость для столбца column
        val minCostToColumn = LongArray(size + 1) { Long.MAX_VALUE }

        var currentColumn = 0
        val visitedColumns = BooleanArray(size + 1)

        // Ищем свободный столбец
        do {
            visitedColumns[currentColumn] = true

            val currentRow = matchedRowByColumn[currentColumn]
            var nextColumn = 0

            // Минимальное изменение потенциалов
            var minDelta = Long.MAX_VALUE

            // Рассматриваем все ещё не посещённые столбцы
            for (column in 1..size) {
                if (visitedColumns[column]) continue

                val weight = weights[currentRow - 1][column - 1]

                // Есть соответствие между этой строкой и столбцом
                if (weight != null) {
                    // Приведённая стоимость
                    val reducedCost = cost(weight) - rowPotential[currentRow] - columnPotential[column]

                    if (reducedCost < minCostToColumn[column]) {
                        minCostToColumn[column] = reducedCost
                        previousColumn[column] = currentColumn
                    }
                }
                // Ищем самый доступный следующий столбец
                if (minCostToColumn[column] < minDelta) {
                    minDelta = minCostToColumn[column]
                    nextColumn = column
                }
            }

            // Если ни до одного нового столбца добраться невозможно, то паросочетания нету
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

            // Переходим к следующему столбцу
            currentColumn = nextColumn

            // Если столбец свободен (matchedRowByColumn[currentColumn] == 0) поиск заканчивается
            // Если занят, то пытаемся переставить уже названченную строку
        } while (matchedRowByColumn[currentColumn] != 0)

        // Свободный столбец найден
        // Теперь идём назад по previousColumn и перестраиваем назначения
        do {
            val previous = previousColumn[currentColumn]
            matchedRowByColumn[currentColumn] = matchedRowByColumn[previous]
            currentColumn = previous
        } while (currentColumn != 0)
    }

    // Сейчас соответствия хранятся: column -> row, перводим их в row -> column
    val matchedColumns = IntArray(size)
    for (column in 1..size) matchedColumns[matchedRowByColumn[column] - 1] = column - 1

    // Считаем суммарный вес уже по исходной матрице
    val totalWeight = matchedColumns.indices.sumOf { row -> weights[row][matchedColumns[row]]!!.toLong() }

    return Matching(matchedColumns.toList(), totalWeight)
}
