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

class RangesTest extends TestSpec {

  /**
   *
   * # Ranges
   *
   * When dealing with numbers in this chapter, we have so far only searched for exact numbers.
   * In practice, filtering on ranges is often more useful. For example, find all products with a
   * price greater than $20 and less than $40.
   *
   * In SQL terms, a range can be expressed as:
   *
   * SELECT product
   * FROM   products
   * WHERE  price BETWEEN 20 AND 40
   *
   */

  "RangesTest" should "filter the full list of products and find three products with price >= 20 and < 40" in {
    val q = search in "my_store/products" query {
      filteredQuery filter {
        rangeFilter("price") gte "20" lt "40"
      }
    } sort (by field "price" order SortOrder.ASC)

    val products = client.execute(q).map(to[Product]).futureValue
    products shouldBe List(Product(20.0, "KDKE-B-9947-#kL5"), Product(30.0, "JODL-X-1937-#pV7"), Product(30.0, "QQPX-R-3956-#aD8"))
  }
}
