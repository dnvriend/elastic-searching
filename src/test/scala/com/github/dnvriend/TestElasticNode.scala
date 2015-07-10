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

import java.io.{ File, PrintWriter }
import java.util.UUID

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import org.elasticsearch.common.settings.ImmutableSettings
import org.scalatest.Suite

object TestElasticNode extends Logging {

  val tempFile = File.createTempFile("elasticsearchtests", "tmp")
  val homeDir = new File(tempFile.getParent + "/" + UUID.randomUUID().toString)
  val confDir = new File(homeDir.getAbsolutePath + "/config")

  homeDir.mkdir()
  confDir.mkdir()

  homeDir.deleteOnExit()
  confDir.deleteOnExit()
  tempFile.deleteOnExit()

  logger.info("Setting ES home dir [{}]", homeDir)
  logger.info("Setting ES conf dir [{}]", confDir)

  //println("Setting ES home dir [{}]", homeDir)
  //println("Setting ES conf dir [{}]", confDir)

  val settings = ImmutableSettings.settingsBuilder()
    .put("node.http.enabled", false)
    .put("http.enabled", false)
    .put("cluster.name", "test")
    .put("path.home", homeDir.getAbsolutePath)
    .put("path.conf", confDir.getAbsolutePath)
    .put("path.repo", homeDir.getAbsolutePath)
    .put("index.number_of_shards", 1)
    .put("index.number_of_replicas", 0)
    .put("index.refresh_interval", "250ms")
    //.put("indices.memory.index_buffer_size", "20%")
    //.put("index.translog.flush_threshold_size", "500mb")
    //.put("index.store.throttle.max_bytes_per_sec", "500mb")
    .put("es.logger.level", "INFO")

  val newStopListFile = new PrintWriter(new File(confDir.getAbsolutePath + "/stoplist.txt"))
  newStopListFile.write("a\nan\nthe\nis\nand\nwhich") // writing the stop words to the file
  newStopListFile.close()

  //println("################################################")
  //println(s"File exisits?: " + new File(confDir.getAbsolutePath + "/stoplist.txt").exists())
  //println(s"File path: " + confDir.getAbsolutePath + "/stoplist.txt")
  //println("################################################")

  implicit val client = ElasticClient.local(settings.build)
}

trait ElasticSugar extends Logging {

  this: Suite ⇒

  val client = TestElasticNode.client

  def refresh(indexes: String*) {
    val i = indexes.size match {
      case 0 ⇒ Seq("_all")
      case _ ⇒ indexes
    }
    val listener = client.client.admin().indices().prepareRefresh(i: _*).execute()
    listener.actionGet()
  }

  def blockUntil(explain: String)(predicate: () ⇒ Boolean): Unit = {
    var backoff = 0
    var done = false

    while (backoff <= 16 && !done) {
      if (backoff > 0) Thread.sleep(1000)
      backoff = backoff + 1
      try {
        done = predicate()
      } catch {
        case e: Throwable ⇒ logger.warn("problem while testing predicate", e)
      }
    }

    require(done, s"Failed waiting on: $explain")
  }

  def blockUntilCount(expected: Long, index: String, types: String*): Unit =
    blockUntil(s"Expected count of $expected") { () ⇒
      val actual = client.execute {
        count from index types types
      }.await.getCount
      expected <= actual
    }
}
