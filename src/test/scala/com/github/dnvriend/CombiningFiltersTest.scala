package com.github.dnvriend

import akka.actor.ActorSystem
import com.sksamuel.elastic4s.ElasticDsl._
import com.github.dnvriend.elasticsearch.extension.ElasticSearch
import org.elasticsearch.search.sort.SortOrder
import org.scalatest.{BeforeAndAfterAll, Matchers, FlatSpec}
import Matchers._

import scala.concurrent.Await
import scala.concurrent.duration._

class CombiningFiltersTest extends FlatSpec with Matchers with BeforeAndAfterAll {
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
    The example in FindingExactValuesTest showed a single filter in use. You could do the following:

    in SQL:

      SELECT product
      FROM   mystore.products
      WHERE  price = 20

    in ElasticSearch:

      search in "my_store/products" query {
        filteredQuery query {
          matchall
        } filter {
          termFilter("price", "20")
        }
      }

    or shorter:

      search in "my_store/products" query {
        filteredQuery filter {
          termFilter("price", "20")
        }
      }

    In practice you will probably need to filter on multiple values or fields. For example, how would you express
    this SQL in Elasticsearch?

      SELECT product
      FROM   products
      WHERE  (price = 20 OR productID = 'XHDK-A-1293-#fJ3')
      AND  (price != 30)

    In these situations, you will need the 'bool filter'. This is a compound filter that accepts other filters as
    arguments, combining them in various boolean combinations.

    # bool filter

    The bool filter is composed of three sections:

    search in "index/type" query {
      filteredQuery filter {
        must {}
        not {}
        should {}
      }
    }

    - 'must': All of these clauses must match. The equivalent of AND.

    - 'not': All of these clauses must_not match. The equivalent of NOT.

    - 'should': At least one of these clauses must match. The equivalent of OR.

    And that’s it! When you need multiple filters, simply place them into the different sections of the bool filter.

    __Note:__ Each section of the bool filter is optional (e.g. you can have a must clause and nothing else),
              and each section can contain a single filter or an array of filters.

    To replicate the SQL example above, we will take the two term filters that we used previously and place them inside
    the should clause, the (OR), and add another clause to deal with the NOT condition:
   */

  it should "find two products matching the query, one matching the productID and one matching the price" in {
    val q = search in "my_store/products" query {
      filteredQuery filter {
        should (
          termFilter("price", "20"),
          termFilter("productID", "XHDK-A-1293-#fJ3")
        ) not {
          termFilter("price", "30")
        }
      }
    } sort (by field "price" order SortOrder.DESC)

    val products = Await.result(es.client.execute(q).map(to[Product]), 5 seconds)
    products shouldBe List(Product(20.0, "KDKE-B-9947-#kL5"), Product(10.0,"XHDK-A-1293-#fJ3"))
  }

  /**

  # nesting boolean filters

  Even though bool is a compound filter and accepts "child" filters, it is important to understand that bool is just
  a filter itself. This means you can nest bool filters inside of other bool filters, giving you the ability to make
  arbitrarily complex boolean logic.

  Given this SQL statement:

    SELECT product
    FROM   products
    WHERE  productID      = "KDKE-B-9947-#kL5"
    OR (   productID      = "JODL-X-1937-#pV7" AND price     = 30 )

    To translate this to ElasticSearch, we can use the following model:

    operator
      operand
    operator
      operand

    this translates to:

    should (OR)
      operand
    must (AND)
      operand
      operand

    and in code:

   */

  it should "find two products using nested filters" in {
    val q = search in "my_store/products" query {
      filteredQuery filter {
        should (
          termFilter("productID", "KDKE-B-9947-#kL5"),
          must (
            termFilter("productID", "JODL-X-1937-#pV7"),
            termFilter("price", "30")
          )
        )
      }
    } sort (by field "price" order SortOrder.DESC)

    val products = Await.result(es.client.execute(q).map(to[Product]), 5 seconds)
    products shouldBe List(Product(30.0, "JODL-X-1937-#pV7"), Product(20.0, "KDKE-B-9947-#kL5"))
  }

  override protected def afterAll(): Unit = {
    Await.ready(es.client.execute(delete index "my_store"), 5 seconds)
    Thread.sleep((1 second).toMillis)
    system.shutdown()
  }
}
