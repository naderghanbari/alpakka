/*
 * Copyright (C) 2016-2020 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.stream.alpakka.cassandra.javadsl

import akka.NotUsed
import akka.annotation.ApiMayChange
import akka.stream.javadsl.Source
import com.datastax.dse.driver.api.core.cql.reactive.ReactiveRow
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.{Row, Statement}

@ApiMayChange // https://github.com/akka/alpakka/issues/1213
object CassandraSource {

  /**
   * Java API: creates a [[CassandraSource]] from a given statement.
   */
  def create(stmt: Statement[_], session: CqlSession): Source[ReactiveRow, NotUsed] =
    akka.stream.javadsl.Source.fromPublisher(session.executeReactive(stmt))

}
