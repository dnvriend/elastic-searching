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
package aggregation

import com.sksamuel.elastic4s.ElasticDsl._

class AggregationTest extends TestSpec {

  "Aggregation" should "find the minimum price of 10" in {
    val q = search in "my_store" -> "products" aggs (aggregation min "min_price" field "price")
    val result = client.execute(q).map(aggr).futureValue
    result shouldBe Map("min_price" -> 10.0)
  }

  it should "find the maximum price of 30" in {
    val q = search in "my_store" -> "products" aggs (aggregation max "max_price" field "price")
    val result = client.execute(q).map(aggr).futureValue
    result shouldBe Map("max_price" -> 30.0)
  }

  it should "find the average price of 22.5" in {
    val q = search in "my_store" -> "products" aggs (aggregation avg "avg_price" field "price")
    val result = client.execute(q).map(aggr).futureValue
    result shouldBe Map("avg_price" -> 22.5)
  }

  it should "find three aggregations" in {
    val q = search in "my_store" -> "products" aggs (
      aggregation min "min_price" field "price",
      aggregation max "max_price" field "price",
      aggregation avg "avg_price" field "price"
    )
    val result = client.execute(q).map(aggr).futureValue
    result shouldBe Map("max_price" -> 30.0, "min_price" -> 10.0, "avg_price" -> 22.5)
  }
}
