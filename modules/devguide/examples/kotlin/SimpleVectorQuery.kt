/*
 * Copyright (c) 2026 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// tag::imports[]
import com.couchbase.client.core.error.CouchbaseException
import com.couchbase.client.kotlin.Cluster
import com.couchbase.client.kotlin.query.execute
// end::imports[]
import kotlinx.coroutines.runBlocking

private const val CONNECTION_STRING = "couchbases://cb.<your-endpoint-here>.cloud.couchbase.com"
private const val USERNAME = "Administrator"
private const val PASSWORD = "password"

fun main() = runBlocking<Unit> {
    val cluster = Cluster.connect(CONNECTION_STRING, USERNAME, PASSWORD) {
        applyProfile("wan-development")
    }

    try {
        // tag::hyperscale[]
        val result = cluster.query(
            "SELECT d.id, d.question, d.wanted_similar_color_from_search, " +
                "  ARRAY_CONCAT( " +
                "d.couchbase_search_query.knn[0].vector[0:4], " +
                "['...'] " +
                ") AS vector " +
                "FROM `vector-sample`.`color`.`rgb-questions` AS d " +
                "WHERE d.id = '#87CEEB';",
            metrics = true,
        ).execute()

        for (row in result.rows) {
            println(row.contentAs<Map<String, Any?>>())
        }
        // end::hyperscale[]
    } catch (ex: CouchbaseException) {
        ex.printStackTrace()
    } finally {
        cluster.disconnect()
    }
}
