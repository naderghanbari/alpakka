/*
 * Copyright (C) 2016-2020 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.stream.alpakka.cassandra.javadsl

import java.util.function.BiFunction
import java.util.function.Function

import akka.NotUsed
import akka.annotation.ApiMayChange
import akka.stream.alpakka.cassandra.CassandraBatchSettings
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.{BoundStatement, PreparedStatement}
import akka.stream.alpakka.cassandra.scaladsl.{CassandraFlow => ScalaCFlow}
import akka.stream.javadsl.Flow

@ApiMayChange // https://github.com/akka/alpakka/issues/1213
object CassandraFlow {
  def createWithPassThrough[T](parallelism: Int,
                               statement: PreparedStatement,
                               statementBinder: BiFunction[T, PreparedStatement, BoundStatement],
                               session: CqlSession): Flow[T, T, NotUsed] =
    ScalaCFlow
      .createWithPassThrough[T](parallelism, statement, (t, p) => statementBinder.apply(t, p))(session)
      .asJava

  /**
   * Creates a flow that batches using an unlogged batch. Use this when most of the elements in the stream
   * share the same partition key. Cassandra unlogged batches that share the same partition key will only
   * resolve to one write internally in Cassandra, boosting write performance.
   *
   * Be aware that this stage does not preserve the upstream order.
   */
  def createUnloggedBatchWithPassThrough[T, K](parallelism: Int,
                                               statement: PreparedStatement,
                                               statementBinder: BiFunction[T, PreparedStatement, BoundStatement],
                                               partitionKey: Function[T, K],
                                               settings: CassandraBatchSettings,
                                               session: CqlSession): Flow[T, T, NotUsed] =
    ScalaCFlow
      .createUnloggedBatchWithPassThrough[T, K](parallelism,
                                                statement,
                                                (t, p) => statementBinder.apply(t, p),
                                                t => partitionKey.apply(t),
                                                settings)(session)
      .asJava

}
