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
package partialmatching

import com.sksamuel.elastic4s.ElasticDsl._
import org.elasticsearch.search.sort.SortOrder

class WildcardAndRegexpQueriesTest extends TestSpec {

  /**
   * The wildcard query is a low-level term-based query similar in nature to the prefix query, but it allows you to specify
   * a pattern instead of just a prefix. It uses the standard shell wildcards where ? matches any character, and * matches
   * zero or more characters.
   */

  "WildcardAndRegexpQueriesTest" should "match all documents based on the wildcard expression" in {
    val q = search in "my_store/products" query {
      wildcard("productID", "*")
    } sort (by field "price" order SortOrder.ASC)

    val products = client.execute(q).map(to[Product]).futureValue
    products shouldBe List(Product(10.0, "XHDK-A-1293-#fJ3"), Product(20.0, "KDKE-B-9947-#kL5"), Product(30.0, "JODL-X-1937-#pV7"), Product(30.0, "QQPX-R-3956-#aD8"))
  }

  it should "match one document containing '*1293*' documents based on the wildcard expression" in {
    val q = search in "my_store/products" query {
      wildcard("productID", "*1293*")
    } sort (by field "price" order SortOrder.ASC)

    val products = client.execute(q).map(to[Product]).futureValue
    products shouldBe List(Product(10.0, "XHDK-A-1293-#fJ3"))
  }

  it should "match three document containing '*1293*' for the productID and two for the price '*3*' documents based on the wildcard expression" in {
    val q = search in "my_store/products" query {
      bool {
        should(
          wildcard("productID", "*1293*"),
          term("price", "30")
        )
      }
    } sort (by field "price" order SortOrder.ASC)

    val products = client.execute(q).map(to[Product]).futureValue
    products shouldBe List(Product(10.0, "XHDK-A-1293-#fJ3"), Product(30.0, "JODL-X-1937-#pV7"), Product(30.0, "QQPX-R-3956-#aD8"))
  }
}
