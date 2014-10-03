package com.github.dnvriend

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

  object JsonMarshaller extends DefaultJsonProtocol with SprayJsonSupport {
    implicit val productFormat = jsonFormat2(Product)
  }
}