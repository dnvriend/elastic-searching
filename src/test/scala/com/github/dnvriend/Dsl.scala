package com.github.dnvriend

import com.sksamuel.elastic4s.BulkDefinition
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType.{StringType, IntegerType}
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.action.search.SearchResponse
import spray.httpx.SprayJsonSupport

import scala.util.Try

object Dsl {
  import spray.json._

  case class Product(price: Double, productID: String)

  def to[T](response: GetResponse)(implicit ev: spray.json.JsonReader[T]): Option[T] =
    Option(response.getSourceAsString).map(to[T]).flatten

  def to[T](response: SearchResponse)(implicit ev: spray.json.JsonReader[T]): List[T] =
    response.getHits.getHits.toList.map(_.getSourceAsString).map(to[T]).flatten

  def to[T](jsonString: String)(implicit ev: spray.json.JsonReader[T]): Option[T] = Try(jsonString.parseJson.convertTo[T]).toOption

  def bulkProductIndexDefinition: BulkDefinition =
    bulk (
      index into "my_store/products" fields(
        "price" -> 10, "productID" ->  "XHDK-A-1293-#fJ3"
        ) id 1,
      index into "my_store/products" fields(
        "price" -> 20, "productID" -> "KDKE-B-9947-#kL5"
        ) id 2,
      index into "my_store/products" fields(
        "price" -> 30, "productID" -> "JODL-X-1937-#pV7"
        ) id 3,
      index into "my_store/products" fields(
        "price" -> 30, "productID" -> "QQPX-R-3956-#aD8"
        ) id 4
    )

  def defaultMyStoreCreateIndexDefinition: CreateIndexDefinition =
    create index "my_store" mappings (
      "products" as (
        "price" typed IntegerType,
        "productID" typed StringType
        )
      )

  def productIdFieldNotAnalyzedMyStoreCreateIndexDefinition: CreateIndexDefinition =
    create index "my_store" mappings (
      "products" as (
        "price" typed IntegerType,
        "productID" typed StringType index "not_analyzed"
        )
      )


  object JsonMarshaller extends DefaultJsonProtocol with SprayJsonSupport {
    implicit val productFormat = jsonFormat2(Product)
  }
}