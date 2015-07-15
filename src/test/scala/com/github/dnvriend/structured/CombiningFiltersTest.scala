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

class CombiningFiltersTest extends TestSpec {

  /**
   * see: https://www.elastic.co/guide/en/elasticsearch/guide/current/combining-filters.html
   */

  "CombiningFiltersTest" should "find two products matching the query, one matching the productID and one matching the price" in {
    val q = search in "my_store/products" query {
      filteredQuery filter {
        should(
          termFilter("price", "20"),
          termFilter("productID", "XHDK-A-1293-#fJ3")
        ) not {
            termFilter("price", "30")
          }
      }
    } sort (by field "price" order SortOrder.DESC)

    val products = client.execute(q).map(to[Product]).futureValue
    products shouldBe List(Product(20.0, "KDKE-B-9947-#kL5"), Product(10.0, "XHDK-A-1293-#fJ3"))
  }

  it should "find two products using nested filters" in {
    val q = search in "my_store/products" query {
      filteredQuery filter {
        should(
          termFilter("productID", "KDKE-B-9947-#kL5"),
          must(
            termFilter("productID", "JODL-X-1937-#pV7"),
            termFilter("price", "30")
          )
        )
      }
    } sort (by field "price" order SortOrder.DESC)

    val products = client.execute(q).map(to[Product]).futureValue
    products shouldBe List(Product(30.0, "JODL-X-1937-#pV7"), Product(20.0, "KDKE-B-9947-#kL5"))
  }
}
