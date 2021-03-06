package com.agilogy.srdb.tx

import javax.sql.DataSource

import scala.util.control.NonFatal

class TransactionController(ds: DataSource) {

  def inTransaction[T](f: Transaction => T)(implicit config: TransactionConfig): T = TransactionController.inTransaction(ds)(f)
}

object TransactionController {

  def inTransaction[T](ds: DataSource)(f: Transaction => T)(implicit config: TransactionConfig): T = {
    val (txCreated, tx) = config match {
      case NewTransaction => (true, Transaction(ds))
      case tx: Transaction =>
        if (tx.isClosed) throw new IllegalStateException("Transaction already closed")
        (false, tx)
    }
    try {
      val res = f(tx)
      // Transaction may be already closed if the user rolled it back by hand
      if (txCreated && !tx.isClosed) tx.commit()
      res
    } catch {
      case t: Throwable =>
        if (txCreated) {
          try {
            tx.rollback()
          } catch {
            case NonFatal(re) =>
              re.printStackTrace
          }
        }
        throw t
    }
  }
}
