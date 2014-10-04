package com.github.dnvriend.structured

import akka.actor.ActorSystem
import com.github.dnvriend.elasticsearch.extension.ElasticSearch
import com.sksamuel.elastic4s.ElasticDsl._
import org.elasticsearch.search.sort.SortOrder
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

class RangesTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  import com.github.dnvriend.Dsl.JsonMarshaller._
  import com.github.dnvriend.Dsl._

  val system = ActorSystem("TestSystem")
  implicit val ec = system.dispatcher
  val es = ElasticSearch(system)

  "ElasticSearch" should "create an index and index products" in {
    Await.ready(es.client.execute(delete index "my_store"), 5 seconds)
    Await.ready(es.client.execute(productIdFieldNotAnalyzedMyStoreCreateIndexDefinition), 5 seconds)
    Await.ready(es.client.execute(bulkProductIndexDefinition), 5 seconds)
    Thread.sleep((2 seconds).toMillis)

    val products = Await.result(es.client.execute(search in "my_store/products").map(to[Product]), 5 seconds)
    products.size shouldBe 4
  }

  /**

    # Ranges

    When dealing with numbers in this chapter, we have so far only searched for exact numbers.
    In practice, filtering on ranges is often more useful. For example, find all products with a
    price greater than $20 and less than $40.

    In SQL terms, a range can be expressed as:

    SELECT product
    FROM   products
    WHERE  price BETWEEN 20 AND 40

   */

  it should "filter the full list of products and find three products with price >= 20 and < 40" in {
    val q = search in "my_store/products" query {
      filteredQuery filter {
        rangeFilter("price") gte "20" lt "40"
      }
    } sort (by field "price" order SortOrder.ASC)

    val products = Await.result(es.client.execute(q).map(to[Product]), 5 seconds)
    products shouldBe List(Product(20.0, "KDKE-B-9947-#kL5"), Product(30.0, "JODL-X-1937-#pV7"), Product(30.0, "QQPX-R-3956-#aD8"))
  }
}
