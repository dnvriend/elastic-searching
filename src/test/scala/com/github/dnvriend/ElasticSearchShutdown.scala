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

import akka.actor.ActorSystem
import com.github.dnvriend.elasticsearch.extension.ElasticSearch
import org.scalatest.Matchers
import org.scalatest.concurrent.{ Eventually, ScalaFutures }

import scala.concurrent.ExecutionContext
import scala.util.Try

trait ElasticSearchShutdown extends BlockUntil with Eventually with ScalaFutures with CheckConnection with Matchers {
  def awaitStarted(implicit system: ActorSystem, ec: ExecutionContext): Unit = {
    blockUntil {
      Try(checkConnection("http://localhost:9200").futureValue) should not be None
    }
  }

  def shutdownElasticSearch(implicit system: ActorSystem, ec: ExecutionContext): Unit = {
    ElasticSearch(system).client.shutdown.futureValue
    blockUntil {
      Try(checkConnection("http://localhost:9200").futureValue) shouldBe None
    }
  }
}
