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

import java.io.InputStream
import java.util.UUID

import akka.actor.ActorSystem
import akka.event.{ Logging, LoggingAdapter }
import akka.stream.{ ActorMaterializer, Materializer }
import akka.util.Timeout
import com.github.dnvriend.util.{ BlockUntil, CheckConnection, ElasticSugar }
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.CreateIndexDefinition
import org.scalatest._
import org.scalatest.concurrent.{ Eventually, ScalaFutures }
import org.scalatest.exceptions.TestFailedException
import spray.json.DefaultJsonProtocol

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.io.{ Source ⇒ IOSource }
import scala.util.Try

case class Product(price: Double, productID: String)

class TestSpec extends FlatSpec with Matchers with ScalaFutures with ElasticSugar with TryValues with OptionValues with BeforeAndAfterEach with BeforeAndAfterAll with BlockUntil with Eventually with CheckConnection with DefaultJsonProtocol {

  implicit val productFormat = jsonFormat2(Product)
  implicit val timeout: Timeout = Timeout(10.seconds)
  implicit val system: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val mat: Materializer = ActorMaterializer()
  implicit val log: LoggingAdapter = Logging(system, this.getClass)
  implicit val pc: PatienceConfig = PatienceConfig(timeout = 60.seconds)

  def randomId: String = UUID.randomUUID.toString
  val id: String = randomId

  def withInputStream(fileName: String)(f: InputStream ⇒ Unit): Unit = {
    val is = fromClasspathAsStream(fileName)
    try { f(is) } finally { Try(is.close()) }
  }

  implicit class FutureToTry[T](f: Future[T]) {
    def toTry: Try[T] = Try(f.futureValue)
  }

  implicit class MustBeWord[T](self: T) {
    def mustBe(pf: PartialFunction[T, Unit]): Unit =
      if (!pf.isDefinedAt(self)) throw new TestFailedException("Unexpected: " + self, 0)
  }

  def streamToString(is: InputStream): String =
    IOSource.fromInputStream(is).mkString

  def fromClasspathAsString(fileName: String): String =
    streamToString(fromClasspathAsStream(fileName))

  def fromClasspathAsStream(fileName: String): InputStream =
    getClass.getClassLoader.getResourceAsStream(fileName)

  def createIndex(indexDef: CreateIndexDefinition): Unit = {
    {
      for {
        r1 ← client.execute(delete index "my_store") recoverWith { case t: Throwable ⇒ Future.successful(()) }
        r2 ← client.execute(indexDef)
        r3 ← client.execute(bulkProductIndexDefinition)
      } yield r3
    }.toTry recover {
      case t: Throwable ⇒
        t.printStackTrace()
    }
    eventually {
      client.execute(search in "my_store/products").map(to[Product]).futureValue.size shouldBe 4
    }
  }

  override protected def beforeEach(): Unit =
    createIndex(productIdFieldNotAnalyzedMyStoreCreateIndexDefinition)

  override protected def afterAll(): Unit = {
    system.shutdown()
    system.awaitTermination()
  }
}
