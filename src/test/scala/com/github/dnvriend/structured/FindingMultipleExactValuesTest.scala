/*
 * Copyright 2015 Dennis Vriend
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dnvriend
package structured

import com.sksamuel.elastic4s.ElasticDsl._
import org.elasticsearch.search.sort.SortOrder

class FindingMultipleExactValuesTest extends TestSpec {

  /**
   *
   * The 'term filter' is useful for finding a single value, but often you’ll want to search for multiple valu
   * What if you want to find documents that have a price of $20 or $30?
   *
   * Rather than using multiple term filters, you can instead use a single terms filter (note the “s” at the end).
   * The terms filter is simply the plural version of the singular term filter.
   *
   * It looks nearly identical to a vanilla term too. Instead of specifying a single price, we are now specifying an
   * array of values:
   *
   */

  "FindingMultipleExactValuesTest" should "filter the full list of products and find three products" in {
    val q = search in "my_store/products" query {
      filteredQuery filter {
        termsFilter("price", "20", "30")
      }
    } sort (by field "price" order SortOrder.ASC)

    val products = client.execute(q).map(to[Product]).futureValue
    products shouldBe List(Product(20.0, "KDKE-B-9947-#kL5"), Product(30.0, "JODL-X-1937-#pV7"), Product(30.0, "QQPX-R-3956-#aD8"))
  }

  /**
   *
   * # contains, but does not equal
   *
   * It is important to understand that 'term' and 'terms' are "contains" operations, not "equals". What does that mean?
   *
   * If you have a term filter for:
   *
   * termFilter("tags", "search")
   *
   * it will match both of the following documents:
   *
   * { "tags" : ["search"] }
   * { "tags" : ["search", "open_source"] }
   *
   * it returns the 2nd document, even though it has terms other than "search".
   *
   * Recall how the term filter works: it checks the inverted index for all documents which contain a term,
   * then constructs a bitset. In our simple example, we have the following inverted index:
   *
   * |    Token     | DocIds |
   * + ------------ + ------ +
   * | open_source  | 2      |
   * | search       | 1, 2   |
   *
   * When a term filter is executed for the token search, it goes straight to the corresponding entry in the inverted
   * index and extracts the associated doc IDs. As you can see, both doc 1 and 2 contain the token in the inverted index,
   * therefore they are both returned as a result.
   *
   * # equals exactly
   *
   * If you do want entire field equality behavior, the best way to accomplish it involves indexing a secondary field.
   * In this field, you index the number of values that your field contains. Using our two previous documents, we now
   * include a field that maintains the number of tags:
   *
   * { "tags" : ["search"], "tag_count" : 1 }
   * { "tags" : ["search", "open_source"], "tag_count" : 2 }
   *
   * Once you have the count information indexed, you can construct a bool filter that enforces the appropriate number of terms:
   *
   * search in "index/type" query {
   * filteredQuery filter {
   * must (
   * termFilter("tags", "search"),
   * termFilter("tag_count", "1")
   * )
   * }
   * }
   *
   * it does the following:
   *
   * 1. Find all documents that have the term "search".
   * 2. But make sure the document only has one tag.
   *
   * This query will now match only the document that has a single tag which is search, rather than any document which
   * contains search.
   *
   */
}
