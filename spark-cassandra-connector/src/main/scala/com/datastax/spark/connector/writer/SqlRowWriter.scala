package com.datastax.spark.connector.writer

import com.datastax.spark.connector.ColumnRef
import com.datastax.spark.connector.cql.TableDef
import com.datastax.spark.connector.types.{ColumnType, VarIntType}
import org.apache.spark.sql.catalyst.expressions.Row

/** A [[RowWriter]] that can write SparkSQL `Row` objects. */
class SqlRowWriter(val table: TableDef, val selectedColumns: IndexedSeq[ColumnRef])
  extends RowWriter[Row] {

  override val columnNames = selectedColumns.map(_.columnName)

  private val columns = columnNames.map(table.columnByName)
  private val columnTypes = columns.map(_.columnType)
  private val converters = columns.map(_.columnType.converterToCassandra)

  /** Extracts column values from `data` object and writes them into the given buffer
    * in the same order as they are listed in the columnNames sequence. */
  override def readColumnValues(row: Row, buffer: Array[Any]) = {
    require(row.size == columnNames.size, s"Invalid row size: ${row.size} instead of ${columnNames.size}.")
    for (i <- 0 until row.size) {
      buffer(i) = columnTypes(i) match {
        case VarIntType => row(i).asInstanceOf[java.math.BigDecimal].toBigInteger
        case other: ColumnType[_] => other.converterToCassandra.convert(row(i))
      }
    }
  }

}


object SqlRowWriter {

  object Factory extends RowWriterFactory[Row] {
    override def rowWriter(table: TableDef, selectedColumns: IndexedSeq[ColumnRef]) =
      new SqlRowWriter(table, selectedColumns)
  }

}