// Connections.kt — соединительные стенки между клавишами

interface Connector {
    fun build(): Model
}

class HorizontalConnector(
    private val fromCol: Int,
    private val toCol: Int,
    private val row: Int,
    private val cfg: MatrixConfig,
) : Connector {
    override fun build(): Model {
        val startX = fromCol * cfg.spacingX + cfg.keyConfig.width
        val endX = toCol * cfg.spacingX
        val length = endX - startX
        if (length <= 0) return bindings.emptyModel()

        val x = startX + length / 2.0
        val y = row * cfg.spacingY
        val z = cfg.rowCurvature * row

        return bindings.cube(length, cfg.keyConfig.wallThickness, cfg.keyConfig.wallHeight)
            .at(x, y, z)
    }
}

class VerticalConnector(
    private val fromRow: Int,
    private val toRow: Int,
    private val col: Int,
    private val cfg: MatrixConfig,
) : Connector {
    override fun build(): Model {
        val startY = fromRow * cfg.spacingY + cfg.keyConfig.height
        val endY = toRow * cfg.spacingY
        val length = endY - startY
        if (length <= 0) return bindings.emptyModel()

        val x = col * cfg.spacingX
        val y = startY + length / 2.0
        val z1 = cfg.rowCurvature * fromRow
        val z2 = cfg.rowCurvature * toRow

        return bindings.cube(cfg.keyConfig.wallThickness, length, cfg.keyConfig.wallHeight)
            .at(x, y, (z1 + z2) / 2.0)
    }
}

class ConnectionBuilder(private val cfg: MatrixConfig) {

    fun buildAllConnections(): Model {
        val connectors = mutableListOf<Connector>()

        // Horizontal connections
        for (row in 0 until cfg.rows) {
            for (col in 0 until cfg.columns - 1) {
                connectors.add(HorizontalConnector(col, col + 1, row, cfg))
            }
        }

        // Vertical connections
        for (col in 0 until cfg.columns) {
            for (row in 0 until cfg.rows - 1) {
                connectors.add(VerticalConnector(row, row + 1, col, cfg))
            }
        }

        return connectors.map { it.build() }.merge()
    }
}
