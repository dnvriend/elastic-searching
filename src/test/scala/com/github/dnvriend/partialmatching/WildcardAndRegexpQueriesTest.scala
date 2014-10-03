package com.github.dnvriend.partialmatching

import akka.actor.ActorSystem
import com.github.dnvriend.Dsl
import com.sksamuel.elastic4s.ElasticDsl._
import com.github.dnvriend.elasticsearch.extension.ElasticSearch
import org.elasticsearch.search.sort.SortOrder
import org.scalatest.{BeforeAndAfterAll, Matchers, FlatSpec}
import Matchers._

import scala.concurrent.Await
import scala.concurrent.duration._

class WildcardAndRegexpQueriesTest extends FlatSpec with Matchers with BeforeAndAfterAll {
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

  /**

    The wildcard query is a low-level term-based query similar in nature to the prefix query, but it allows you to specify
    a pattern instead of just a prefix. It uses the standard shell wildcards where ? matches any character, and * matches
    zero or more characters.

   */

  it should "match all documents based on the wildcard expression" in {
    val q = search in "my_store/products" query {
       wildcard("productID", "*")
    } sort (by field "price" order SortOrder.ASC)

    val products = Await.result(es.client.execute(q).map(to[Product]), 5 seconds)
    products shouldBe List(Product(10.0, "XHDK-A-1293-#fJ3"), Product(20.0, "KDKE-B-9947-#kL5"), Product(30.0, "JODL-X-1937-#pV7"), Product(30.0, "QQPX-R-3956-#aD8"))
  }

  it should "match one document containing '*1293*' documents based on the wildcard expression" in {
    val q = search in "my_store/products" query {
        wildcard("productID", "*1293*")
    } sort (by field "price" order SortOrder.ASC)

    val products = Await.result(es.client.execute(q).map(to[Product]), 5 seconds)
    products shouldBe List(Product(10.0, "XHDK-A-1293-#fJ3"))
  }

  it should "match three document containing '*1293*' for the productID and two for the price '*3*' documents based on the wildcard expression" in {
    val q = search in "my_store/products" query {
       bool {
        should (
          wildcard("productID", "*1293*"),
          term("price", "30")
        )
      }
    } sort (by field "price" order SortOrder.ASC)

    val products = Await.result(es.client.execute(q).map(to[Product]), 5 seconds)
    products shouldBe List(Product(10.0, "XHDK-A-1293-#fJ3"), Product(30.0, "JODL-X-1937-#pV7"), Product(30.0, "QQPX-R-3956-#aD8"))
  }


  override protected def afterAll(): Unit = {
    Await.ready(es.client.execute(delete index "my_store"), 5 seconds)
    Thread.sleep((1 second).toMillis)
    system.shutdown()
  }

}
