package com.github.dnvriend.structured

import akka.actor.ActorSystem
import com.github.dnvriend.elasticsearch.extension.ElasticSearch
import com.sksamuel.elastic4s.ElasticDsl._
import org.elasticsearch.search.sort.SortOrder
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

class FindingMultipleExactValuesTest extends FlatSpec with Matchers with BeforeAndAfterAll {
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

    val q = search in "my_store/products"
    val products = Await.result(es.client.execute(q).map(to[Product]), 5 seconds)
    products.size shouldBe 4
  }

  /**

    The 'term filter' is useful for finding a single value, but often you’ll want to search for multiple values.
    What if you want to find documents that have a price of $20 or $30?

    Rather than using multiple term filters, you can instead use a single terms filter (note the “s” at the end).
    The terms filter is simply the plural version of the singular term filter.

    It looks nearly identical to a vanilla term too. Instead of specifying a single price, we are now specifying an
    array of values:

   */

  it should "filter the full list of products and find three products" in {
    val q = search in "my_store/products" query {
      filteredQuery filter {
        termsFilter("price", "20", "30")
      }
    } sort (by field "price" order SortOrder.ASC)

    val products = Await.result(es.client.execute(q).map(to[Product]), 5 seconds)
    products shouldBe List(Product(20.0, "KDKE-B-9947-#kL5"), Product(30.0, "JODL-X-1937-#pV7"), Product(30.0, "QQPX-R-3956-#aD8"))
  }

  /**

    # contains, but does not equal

    It is important to understand that 'term' and 'terms' are "contains" operations, not "equals". What does that mean?

    If you have a term filter for:

     termFilter("tags", "search")

    it will match both of the following documents:

    { "tags" : ["search"] }
    { "tags" : ["search", "open_source"] }

    it returns the 2nd document, even though it has terms other than "search".

    Recall how the term filter works: it checks the inverted index for all documents which contain a term,
    then constructs a bitset. In our simple example, we have the following inverted index:

    |    Token     | DocIds |
    + ------------ + ------ +
    | open_source  | 2      |
    | search       | 1, 2   |

    When a term filter is executed for the token search, it goes straight to the corresponding entry in the inverted
    index and extracts the associated doc IDs. As you can see, both doc 1 and 2 contain the token in the inverted index,
    therefore they are both returned as a result.

    # equals exactly

    If you do want entire field equality behavior, the best way to accomplish it involves indexing a secondary field.
    In this field, you index the number of values that your field contains. Using our two previous documents, we now
    include a field that maintains the number of tags:

    { "tags" : ["search"], "tag_count" : 1 }
    { "tags" : ["search", "open_source"], "tag_count" : 2 }

    Once you have the count information indexed, you can construct a bool filter that enforces the appropriate number of terms:

      search in "index/type" query {
      filteredQuery filter {
        must (
          termFilter("tags", "search"),
          termFilter("tag_count", "1")
        )
      }
    }

    it does the following:

    1. Find all documents that have the term "search".
    2. But make sure the document only has one tag.

    This query will now match only the document that has a single tag which is search, rather than any document which
    contains search.

   */

  override protected def afterAll(): Unit = {
    Await.ready(es.client.execute(delete index "my_store"), 5 seconds)
    Thread.sleep((1 second).toMillis)
    system.shutdown()
  }

}
