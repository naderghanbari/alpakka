/*
 * Copyright (C) 2016-2020 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.stream.alpakka.cassandra.scaladsl

import akka.Done
import akka.annotation.ApiMayChange
import akka.stream.scaladsl.{Flow, Keep, Sink}
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.{BoundStatement, PreparedStatement}

import scala.concurrent.Future
import scala.compat.java8.FutureConverters._

/**
 * Scala API to create Cassandra Sinks.
 */
@ApiMayChange // https://github.com/akka/alpakka/issues/1213
object CassandraSink {
  def apply[T](
      parallelism: Int,
      statement: PreparedStatement,
      statementBinder: (T, PreparedStatement) => BoundStatement
  )(implicit session: CqlSession): Sink[T, Future[Done]] =
    Flow[T]
      .mapAsyncUnordered(parallelism)(t ⇒ session.executeAsync(statementBinder(t, statement)).toScala)
      .toMat(Sink.ignore)(Keep.right)
}
