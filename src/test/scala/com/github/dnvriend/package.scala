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

package com.github

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType.{ IntegerType, StringType }
import com.sksamuel.elastic4s.{ BulkDefinition, CreateIndexDefinition }
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.search.aggregations.metrics.avg.Avg
import org.elasticsearch.search.aggregations.metrics.max.Max
import org.elasticsearch.search.aggregations.metrics.min.Min
import org.elasticsearch.search.aggregations.metrics.sum.Sum
import spray.json._

import scala.collection.JavaConversions._
import scala.util.Try

package object dnvriend {
  def to[T](response: GetResponse)(implicit ev: spray.json.JsonReader[T]): Option[T] =
    Option(response.getSourceAsString).flatMap(to[T])

  def to[T](response: SearchResponse)(implicit ev: spray.json.JsonReader[T]): List[T] =
    response.getHits.getHits.toList.map(_.getSourceAsString).map(to[T]).flatten

  def to[T](jsonString: String)(implicit ev: spray.json.JsonReader[T]): Option[T] = Try(jsonString.parseJson.convertTo[T]).toOption

  def aggr(response: SearchResponse): Map[String, Double] =
    response.getAggregations.asMap().toMap.mapValues {
      case e: Min ⇒ e.getValue
      case e: Max ⇒ e.getValue
      case e: Avg ⇒ e.getValue
      case e: Sum ⇒ e.getValue
    }

  def bulkProductIndexDefinition: BulkDefinition =
    bulk(
      index into "my_store/products" fields (
        "price" -> 10, "productID" -> "XHDK-A-1293-#fJ3"
      ) id 1,
      index into "my_store/products" fields (
        "price" -> 20, "productID" -> "KDKE-B-9947-#kL5"
      ) id 2,
      index into "my_store/products" fields (
        "price" -> 30, "productID" -> "JODL-X-1937-#pV7"
      ) id 3,
      index into "my_store/products" fields (
        "price" -> 30, "productID" -> "QQPX-R-3956-#aD8"
      ) id 4
    )

  def defaultMyStoreCreateIndexDefinition: CreateIndexDefinition =
    create index "my_store" mappings (
      "products" as (
        "price" typed IntegerType,
        "productID" typed StringType index StringFieldMappingAttributes.Analyzed
      )
    )

  def productIdFieldNotAnalyzedMyStoreCreateIndexDefinition: CreateIndexDefinition =
    create index "my_store" mappings (
      "products" as (
        "price" typed IntegerType,
        "productID" typed StringType index StringFieldMappingAttributes.NotAnalyzed
      )
    )

  def productIdFieldNoAnalyzerMyStoreCreateIndexDefinition: CreateIndexDefinition =
    create index "my_store" mappings (
      "products" as (
        "price" typed IntegerType,
        "productID" typed StringType index StringFieldMappingAttributes.No
        )
      )

  object StringFieldMappingAttributes {
    /**
     * (Default) First analyze the string and then index it.
     * In other words, index this field as full text.
     */
    final val Analyzed = "analyzed"
    /**
     * Index this field, so it is searchable, but index the
     * value exactly as specified. Do not analyze it.
     */
    final val NotAnalyzed = "not_analyzed"
    /**
     * Don’t index this field at all. This field will not be searchable.
     */
    final val No = "no"
  }
}
