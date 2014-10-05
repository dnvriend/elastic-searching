package com.github.dnvriend.aggregation

import akka.actor.ActorSystem
import com.github.dnvriend.Dsl
import com.sksamuel.elastic4s.ElasticDsl._
import com.github.dnvriend.elasticsearch.extension.ElasticSearch
import org.scalatest.{BeforeAndAfterAll, Matchers, FlatSpec}
import Matchers._

import scala.concurrent.Await
import scala.concurrent.duration._

class AggregationTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  import Dsl._
  import Dsl.JsonMarshaller._

  val system = ActorSystem("TestSystem")
  implicit val ec = system.dispatcher
  val es = ElasticSearch(system)

  "ElasticSearch" should "create an index and index products" in {
    Await.ready(es.client.execute(delete index "my_store"), 5 seconds)

    Await.ready(es.client.execute(productIdFieldNotAnalyzedMyStoreCreateIndexDefinition), 5 seconds)

    Await.ready(es.client.execute(bulkProductIndexDefinition), 5 seconds)

    Thread.sleep((2 seconds).toMillis)

    val q = search in "my_store/products"
    val products = Await.result(es.client.execute(q).map(to[Product]), 5 seconds)
    products.size shouldBe 4
  }

  it should "find the minimum price of 10" in {
    val q = search in "my_store" -> "products" aggs (aggregation min "min_price" field "price")
    val result = Await.result(es.client.execute(q).map(aggr), 5 seconds)
    result shouldBe Map("min_price" -> 10.0)
  }

  it should "find the maximum price of 30" in {
    val q = search in "my_store" -> "products" aggs (aggregation max "max_price" field "price")
    val result = Await.result(es.client.execute(q).map(aggr), 5 seconds)
    result shouldBe Map("max_price" -> 30.0)
  }

  it should "find the average price of 22.5" in {
    val q = search in "my_store" -> "products" aggs (aggregation avg "avg_price" field "price")
    val result = Await.result(es.client.execute(q).map(aggr), 5 seconds)
    result shouldBe Map("avg_price" -> 22.5)
  }

  it should "find three aggregations" in {
    val q = search in "my_store" -> "products" aggs (
      aggregation min "min_price" field "price",
      aggregation max "max_price" field "price",
      aggregation avg "avg_price" field "price"
    )
    val result = Await.result(es.client.execute(q).map(aggr), 5 seconds)
    result shouldBe Map("max_price" -> 30.0, "min_price" -> 10.0, "avg_price" -> 22.5)
  }
}
