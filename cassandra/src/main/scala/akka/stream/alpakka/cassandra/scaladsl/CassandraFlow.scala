/*
 * Copyright (C) 2016-2020 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.stream.alpakka.cassandra.scaladsl

import akka.NotUsed
import akka.annotation.ApiMayChange
import akka.dispatch.ExecutionContexts
import akka.stream.FlowShape
import akka.stream.alpakka.cassandra.CassandraBatchSettings
import akka.stream.scaladsl.{Flow, GraphDSL}
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.{BatchStatement, BatchType, BoundStatement, PreparedStatement}

import scala.collection.JavaConverters._
import scala.compat.java8.FutureConverters._

/**
 * Scala API to create Cassandra flows.
 */
@ApiMayChange // https://github.com/akka/alpakka/issues/1213
object CassandraFlow {
  def createWithPassThrough[T](
      parallelism: Int,
      statement: PreparedStatement,
      statementBinder: (T, PreparedStatement) => BoundStatement
  )(implicit session: CqlSession): Flow[T, T, NotUsed] =
    Flow[T].mapAsync(parallelism)(
      t ⇒
        session
          .executeAsync(statementBinder(t, statement))
          .toScala
          .map(_ => t)(ExecutionContexts.sameThreadExecutionContext)
    )

  /**
   * Creates a flow that batches using an unlogged batch. Use this when most of the elements in the stream
   * share the same partition key. Cassandra unlogged batches that share the same partition key will only
   * resolve to one write internally in Cassandra, boosting write performance.
   *
   * Be aware that this stage does not preserve the upstream order.
   */
  def createUnloggedBatchWithPassThrough[T, K](
      parallelism: Int,
      statement: PreparedStatement,
      statementBinder: (T, PreparedStatement) => BoundStatement,
      partitionKey: T => K,
      settings: CassandraBatchSettings = CassandraBatchSettings()
  )(implicit session: CqlSession): Flow[T, T, NotUsed] = {
    val graph = GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val groupStage: FlowShape[T, Seq[T]] =
        builder.add(Flow[T].groupedWithin(settings.maxGroupSize, settings.maxGroupWait))

      val groupByKeyStage: FlowShape[Seq[T], Seq[T]] =
        builder.add(Flow[Seq[T]].map(_.groupBy(partitionKey).values.toList).mapConcat(identity))

      val batchStatementStage: FlowShape[Seq[T], Seq[T]] = builder.add(
        Flow[Seq[T]].mapAsyncUnordered(parallelism)(
          list => {
            val boundStatements = list.map(t => statementBinder(t, statement))
            val batchStatement = BatchStatement.newInstance(BatchType.UNLOGGED).addAll(boundStatements.asJava)
            session
              .executeAsync(batchStatement)
              .toScala
              .map(_ => list)(ExecutionContexts.sameThreadExecutionContext)
          }
        )
      )

      val flattenResults: FlowShape[Seq[T], T] = builder.add(Flow[Seq[T]].mapConcat(_.toList))

      groupStage ~> groupByKeyStage ~> batchStatementStage ~> flattenResults

      FlowShape(groupStage.in, flattenResults.out)
    }
    Flow.fromGraph(graph)
  }
}
