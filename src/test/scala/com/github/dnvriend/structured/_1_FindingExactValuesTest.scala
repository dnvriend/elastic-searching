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
import com.sksamuel.elastic4s.CreateIndexDefinition

class _1_FindingExactValuesTest extends TestSpec {

  // see: https://www.elastic.co/guide/en/elasticsearch/guide/current/_finding_exact_values.html

  "FindingExactValuesTest" should "find four products in the index" in {
    //
    // SELECT * FROM products
    //
    val res = client.execute {
      search in "my_store" / "products"
    }.await
    res.getHits.getTotalHits shouldBe 4
  }

  it should "filter the full list of products and find only one Product" in {
    val res = client.execute {
      search in "my_store" / "products" query {
        filteredQuery query {
          matchall
        } filter {
          termFilter("price", "20")
        }
      }
    }.await
    res.getHits.getTotalHits shouldBe 1
  }

  it should "filter the full list of products and find only one Product omitting the matchall query" in {
    val res = client.execute {
      search in "my_store" / "products" query {
        filteredQuery filter {
          termFilter("price", "20")
        }
      }
    }.await
    res.getHits.getTotalHits shouldBe 1
  }

  it should "filter the full list of products on the field 'productID', but it cannot find the text" in {
    createIndex(defaultMyStoreCreateIndexDefinition)
    val q = search in "my_store/products" query {
      filteredQuery filter {
        termFilter("productID", "XHDK-A-1293-#fJ3")
      }
    }

    val products = client.execute(q).map(to[Product]).futureValue
    products.size shouldBe 0
  }

  it should "filter the full list of products on the field 'productID'" in {
    val q = search in "my_store/products" query {
      filteredQuery filter {
        termFilter("productID", "XHDK-A-1293-#fJ3")
      }
    }

    val products = client.execute(q).map(to[Product]).futureValue
    products.size shouldBe 1
  }
}