/*
 * Copyright (C) 2016-2020 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.stream.alpakka.cassandra.scaladsl

import akka.NotUsed
import akka.annotation.ApiMayChange
import akka.stream.scaladsl.Source
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.{Row, Statement}

@ApiMayChange // https://github.com/akka/alpakka/issues/1213
object CassandraSource {

  /**
   * Scala API: creates a [[CassandraSourceStage]] from a given statement.
   */
  def apply(stmt: Statement[_])(implicit session: CqlSession): Source[Row, NotUsed] =
    Source.fromPublisher(session.executeReactive(stmt))

}
