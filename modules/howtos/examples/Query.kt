/*
 * Copyright 2022 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.couchbase.client.kotlin.Cluster
import com.couchbase.client.kotlin.Scope
import com.couchbase.client.kotlin.kv.MutationResult
import com.couchbase.client.kotlin.kv.MutationState
import com.couchbase.client.kotlin.query.*
import java.util.*

private suspend fun simpleQueryDefaultCollection(cluster: Cluster) {
// tag::simpleQueryDefaultCollection[]
    val queryResult: QueryResult = cluster
        .query("SELECT * FROM `travel-sample` LIMIT 10")
        .execute() // <1>

    queryResult.rows.forEach { row: QueryRow ->
        println("Found row: " + row.contentAs<Map<String, Any?>>())
    }
// end::simpleQueryDefaultCollection[]
}

private suspend fun simpleQueryCluster(cluster: Cluster) {
// tag::simpleQueryCluster[]
    val queryResult: QueryResult = cluster
        .query("""
            SELECT * 
            FROM `travel-sample`.inventory.airline 
            LIMIT 10
            """)
        .execute()

    queryResult.rows.forEach { row: QueryRow ->
        println("Found row: " + row.contentAs<Map<String, Any?>>())
    }
// end::simpleQueryCluster[]
}

private suspend fun simpleQueryScope(cluster: Cluster) {
// tag::simpleQueryScope[]
    val scope: Scope = cluster
        .bucket("travel-sample")
        .scope("inventory")

    val queryResult: QueryResult = scope
        .query("SELECT * FROM airline LIMIT 10")
        .execute()

    queryResult.rows.forEach { row: QueryRow ->
        println("Found row: " + row.contentAs<Map<String, Any?>>())
    }
// end::simpleQueryScope[]
}

private suspend fun positionalParameters(cluster: Cluster) {
// tag::positionalParameters[]
    val queryResult: QueryResult = cluster
        .query(
            statement = """
                SELECT *
                FROM `travel-sample`.inventory.airline
                WHERE country = ?
            """,
            parameters = QueryParameters.positional(
                listOf("France")
            )
        )
        .execute()
// end::positionalParameters[]
}

private suspend fun namedParameters(cluster: Cluster) {

// tag::namedParameters[]
    val queryResult: QueryResult = cluster
        .query(
            statement = """
                SELECT *
                FROM `travel-sample`.inventory.airline
                WHERE country = ${"\$country"} // <1>
            """,
            parameters = QueryParameters.named(
                "country" to "France"
            )
        )
        .execute()
// end::namedParameters[]
}

private suspend fun streaming(cluster: Cluster) {
// tag::streaming[]
    val metadata: QueryMetadata = cluster
        .query("SELECT * FROM `travel-sample`.inventory.airline")
        .execute { row: QueryRow ->
            println("Found row: " + row.contentAs<Map<String, Any?>>())
        }
// end::streaming[]
}

private suspend fun metadata(cluster: Cluster) {
// tag::metadata[]
    val queryResult: QueryResult = cluster
        .query(
            statement = "SELECT * FROM `travel-sample`.inventory.airline",
            metrics = true,
            profile = QueryProfile.TIMINGS,
        )
        .execute()

    val metadata: QueryMetadata = queryResult.metadata

    println("Client context ID: ${metadata.clientContextId}")
    println("Request ID: ${metadata.requestId}")
    println("Signature: ${metadata.signature}")
    println("Status: ${metadata.status}")
    println("Warnings: ${metadata.warnings}")

    metadata.metrics?.let { metrics: QueryMetrics ->
        println("Reported execution time: ${metrics.executionTime}")
        println("Other metrics: $metrics")
    }

    metadata.profile?.let { profile: Map<String, Any?> ->
        println("Profile: $profile")
    }

// end::metadata[]
}

private suspend fun preparedStatement(cluster: Cluster) {
// tag::preparedStatement[]
    val queryResult: QueryResult = cluster
        .query(
            statement = "SELECT * FROM `travel-sample` LIMIT 10",
            adhoc = false,
        )
        .execute()
// end::preparedStatement[]
}

private suspend fun readOnly(cluster: Cluster) {
// tag::readOnly[]
    val queryResult: QueryResult = cluster
        .query(
            statement = "SELECT * FROM `travel-sample` LIMIT 10",
            readonly = true,
        )
        .execute()
// end::readOnly[]
}

private suspend fun consistentWith(cluster: Cluster) {
// tag::consistentWith[]
    val collection = cluster
        .bucket("travel-sample")
        .scope("inventory")
        .collection("airline")

    val mutationResult: MutationResult =
        collection.upsert("my-fake-airline", mapOf("id" to 9000))

    val mutationState = MutationState()
    mutationState.add(mutationResult)

    val queryResult: QueryResult = cluster
        .query(
            statement = "SELECT * FROM `travel-sample`.inventory.airline LIMIT 10",
            consistency = QueryScanConsistency
                .consistentWith(mutationState)
        )
        .execute()
// end::consistentWith[]
}

private suspend fun clientContextId(cluster: Cluster) {
// tag::clientContextId[]
    val queryResult: QueryResult = cluster
        .query(
            statement = "SELECT * FROM `travel-sample` LIMIT 10",
            clientContextId = "user-44-" + UUID.randomUUID(),
        )
        .execute()
// end::clientContextId[]
}

