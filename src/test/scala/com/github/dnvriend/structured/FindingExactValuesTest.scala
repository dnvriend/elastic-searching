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

class FindingExactValuesTest extends TestSpec {

  /**
   * # Finding exact values
   *
   * When working with exact values, you will be working with filters. Filters are important because they are very, very fast.
   * Filters do not calculate relevance (avoiding the entire scoring phase) and are easily cached; you should use filters as often as you can.
   *
   * # 'term filter' with numbers
   *
   * We are going to explore the 'term filter' first because you will use it often.
   * This filter is capable of handling numbers, booleans, dates and text.
   *
   * Let’s look at an example using numbers first by indexing some products. These documents have a price and a productID
   * and are indexed in the beforeAll step. Let's check whether all products are available:
   */

  "FindingExactValuesTest" should "find four products in the index" in {
    val q = search in "my_store/products"
    val products = client.execute(q).map(to[Product]).futureValue
    products.size shouldBe 4
  }

  /**
   * Our goal is to find all products with a certain price. You may be familiar with SQL if you are coming from a relational
   * database background. If we expressed this query as an SQL query, it would look like this:
   *
   * SELECT product
   * FROM   products
   * WHERE  price = 20
   *
   * In the query DSL, we use a 'term filter' to accomplish the same thing. The term filter will look for the exact
   * value that we specify. By itself, a term filter is very simple. It accepts a field name and the value that we wish
   * to find
   */

  it should "filter the full list of products and find only one Product" in {
    val q = search in "my_store/products" query {
      filteredQuery query {
        matchall
      } filter {
        termFilter("price", "20")
      }
    }

    val products = client.execute(q).map(to[Product]).futureValue
    products.size shouldBe 1
  }

  it should "filter the full list of products and find only one Product omitting the matchall query" in {
    val q = search in "my_store/products" query {
      filteredQuery filter {
        termFilter("price", "20")
      }
    }

    val products = client.execute(q).map(to[Product]).futureValue
    products.size shouldBe 1
  }

  /**
   * # 'term filter' with text
   *
   * As mentioned at the top of this section, the 'term filter' can match strings just as easily as numbers.
   * Instead of price, let’s try to find products that have a certain UPC identification code. To do this with SQL,
   * we might use a query like this:
   *
   * SELECT product
   * FROM   products
   * WHERE  productID = 'XHDK-A-1293-#fJ3'
   *
   * Except there is a little hiccup… we don’t get any results back! Why is that? The problem isn’t actually with the
   * 'term query', it is with the way the data has been indexed. By default, ElasticSearch, uses the StandardAnalyzer
   * so the UPC has been tokenized into smaller tokens...
   *
   * The Standard Analyzer is the default analyzer that Elasticsearch us It is the best general choice for analyzing
   * text which may be in any language. It splits the text on word boundaries, as defined by the Unicode Consortium, and
   * removes most punctuation. Finally, it lowercases all terms.
   *
   * It would convert:
   *
   * "Set the shape to semi-transparent by calling set_trans(5)"
   *
   * to
   *
   * set, the, shape, to, semi, transparent, by, calling, set_trans, 5
   *
   */

  it should "filter the full list of products on the field 'productID', but it cannot find the text" in {
    val q = search in "my_store/products" query {
      filteredQuery filter {
        termFilter("productID", "XHDK-A-1293-#fJ3")
      }
    }

    val products = client.execute(q).map(to[Product]).futureValue
    products.size shouldBe 0
  }

  /**
   *
   * There are a couple of important points here:
   *
   * 1. We have four distinct tokens instead of a single token representing the UPC.
   * 2. All letters have been lowercased.
   * 3. We lost the hyphen and the hash (#) sign.
   *
   * It converted the productID to: xhdk a 1293 fj3
   *
   * So when our term filter looks for the exact value 'XHDK-A-1293-#fJ3', it doesn’t find anything because that token
   * does not exist in the inverted index. Instead, there are the four tokens listed above.
   *
   * Obviously, this is not what we want to happen when dealing with identification codes, or any kind of precise enumeration.
   *
   * To prevent this from happening, we need to tell Elasticsearch that this field contains an exact value by setting it to be
   * not_analyzed. To do this, we need to first delete our old index (because it has the incorrect mapping) and create a new
   * index with the correct mappings where the productID field will not be analyzed
   *
   */

  it should "recreate the index with no analyzer on the productID field" in {
    client.execute(delete index "my_store").toTry should be a 'success
    client.execute(productIdFieldNotAnalyzedMyStoreCreateIndexDefinition).toTry should be a 'success
    client.execute(bulkProductIndexDefinition).toTry should be a 'success
  }

  /**
   * Since the productID field is not analyzed, and the term filter performs no analysis, the query finds the exact match
   * and returns document 1 as a hit. Success!
   */

  it should "filter the full list of products on the field 'productID'" in {
    val q = search in "my_store/products" query {
      filteredQuery filter {
        termFilter("productID", "XHDK-A-1293-#fJ3")
      }
    }

    val products = client.execute(q).map(to[Product]).futureValue
    products.size shouldBe 1
  }

  /**
   * # internal filter operation
   *
   * Internally, Elasticsearch is performing several operations when executing a filter:
   *
   * 1. Find Matching Docs:
   * The term filter looks up the term "XHDK-A-1293-#fJ3" in the inverted index and retrieves the
   * list of documents that contain that term. In this case, only document 1 has the term we are looking for
   *
   * 2. Build a Bitset
   * The filter then builds a bitset — an array of 1’s and 0’s — which describes which documents contain the term.
   * Matching documents receive a 1 bit. In our example, the bitset would be: [1,0,0,0]
   *
   * 3. Cache the Bitset
   * Lastly, the bitset is stored in memory, since we can use this in the future and skip steps 1. and 2.
   * This adds a lot of performance and makes filters very fast.
   *
   * When executing a filtered query, the filter is executed before the query. The resulting bitset is given to the query
   * which uses it to simply skip over any documents that have already been excluded by the filter. This is one of the ways
   * that filters can improve performance. Fewer documents evaluated by the query means faster response tim
   *
   */
}
