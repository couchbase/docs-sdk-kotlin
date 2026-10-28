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

import com.couchbase.client.core.error.IndexExistsException
import com.couchbase.client.kotlin.Bucket
import com.couchbase.client.kotlin.Cluster
import com.couchbase.client.kotlin.Collection
import com.couchbase.client.kotlin.Keyspace
import com.couchbase.client.kotlin.Scope
import com.couchbase.client.kotlin.query.QueryParameters
import com.couchbase.client.kotlin.query.QueryScanConsistency
import com.couchbase.client.kotlin.query.execute
import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class N1qlQueryExample {

    private val connectionString = "localhost"
    private val username = "Administrator"
    private val password = "password"
    private val bucketName = "travel-sample"
    private val id = "user::${UUID.randomUUID()}"

    lateinit var cluster: Cluster
    private lateinit var bucket: Bucket
    private lateinit var scope: Scope
    private lateinit var collection: Collection

    suspend fun init() {
        cluster = Cluster.connect(connectionString, username, password)
        bucket = cluster.bucket(bucketName).waitUntilReady(10.seconds)
        scope = bucket.scope("inventory")
        collection = scope.collection("airport")
    }

    suspend fun n1qlQuery1() {
        // tag::n1ql_query_1[]
        // cluster.query() returns a Flow, and the query only runs when the Flow
        // is collected. The execute() function does that, and gathers the rows.
        val result = cluster.query(
            "select count(*) from `travel-sample`.inventory.airport where country = ?",
            adhoc = false,
            parameters = QueryParameters.positional { param("France") },
        ).execute()
        // end::n1ql_query_1[]
    }

    suspend fun n1qlQuery2() {
        try {
            setupDropIndexes()
            // tag::n1ql_query_2[]
            // The cluster-level manager works on the keyspace you give it. A Keyspace with
            // only a bucket name means that bucket's default collection.
            // With Couchbase Server 7.0 or later, collection.queryIndexes is the
            // alternative for indexing a named collection.
            val indexManager = cluster.queryIndexes
            val keyspace = Keyspace(bucketName)

            indexManager.createPrimaryIndex(keyspace)
            indexManager.createIndex(keyspace, "ix_name", listOf("name"))
            indexManager.createIndex(keyspace, "ix_email", listOf("preferred_email"))
            // end::n1ql_query_2[]
        } catch (e: IndexExistsException) {
            System.err.println(e)
        }
    }

    suspend fun n1qlQuery3() {
        try {
            setupDropIndexes()
            // tag::n1ql_query_3[]
            val indexManager = cluster.queryIndexes
            val keyspace = Keyspace(bucketName)

            indexManager.createPrimaryIndex(keyspace, deferred = true)
            indexManager.createIndex(keyspace, "ix_name", listOf("name"), deferred = true)
            indexManager.createIndex(keyspace, "ix_email", listOf("preferred_email"), deferred = true)
            indexManager.buildDeferredIndexes(keyspace)
            indexManager.watchIndexes(keyspace, listOf("ix_name", "ix_email"), timeout = 5.minutes)
            // end::n1ql_query_3[]
        } catch (e: IndexExistsException) {
            System.err.println(e)
        }
    }

    suspend fun n1qlQuery4() {
        // tag::n1ql_query_4[]
        val id = "user::${UUID.randomUUID()}"
        collection.insert(
            id,
            mapOf("value" to true),
        )

        cluster.query(
            "select * from `$bucketName`.inventory.airport where META().id = \$id",
            parameters = QueryParameters.named { param("id", id) },
        ).execute()
        // end::n1ql_query_4[]
    }

    suspend fun n1qlQuery5() {
        // tag::n1ql_query_5[]
        cluster.query(
            "select * from `$bucketName`.inventory.airport where META().id = \$id",
            parameters = QueryParameters.named { param("id", id) },
            consistency = QueryScanConsistency.requestPlus(),
        ).execute()
        // end::n1ql_query_5[]
    }

    private suspend fun setupDropIndexes() {
        val indexManager = cluster.queryIndexes
        val keyspace = Keyspace(bucketName)

        // ignoreIfNotExists means there is no exception if there is no index to drop.
        indexManager.dropPrimaryIndex(keyspace, ignoreIfNotExists = true)
        indexManager.dropIndex(keyspace, "ix_name", ignoreIfNotExists = true)
        indexManager.dropIndex(keyspace, "ix_email", ignoreIfNotExists = true)
    }
}

fun main() = runBlocking<Unit> {
    val obj = N1qlQueryExample()
    obj.init()
    obj.n1qlQuery1()
    obj.n1qlQuery2()
    obj.n1qlQuery3()
    obj.n1qlQuery4()
    obj.n1qlQuery5()
    println("Done.")
    obj.cluster.disconnect()
}
