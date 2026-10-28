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

import com.couchbase.client.kotlin.Cluster
import com.couchbase.client.kotlin.Scope
import com.couchbase.client.kotlin.search.SearchSpec
import com.couchbase.client.kotlin.search.execute
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

// These examples assume Search indexes called `vector-index` and `vector-and-fts-index`
// exist on the "inventory" scope, and index a field called `vector_field`.

fun main() = runBlocking<Unit> {
    val cluster = Cluster.connect("localhost", "Administrator", "password")
    val bucket = cluster.bucket("travel-sample").waitUntilReady(10.seconds)
    val scope = bucket.scope("inventory")

    // These will come from an external source, such as an embeddings API.
    val vectorQuery = floatArrayOf(0.1f, 0.2f, 0.3f)
    val anotherVectorQuery = floatArrayOf(0.3f, 0.2f, 0.1f)

    vector1(scope, vectorQuery)
    vectorBase64(scope, "zczMPc3MTD6amZk+")
    prefilterMatch(scope, vectorQuery)
    prefilterQueryString(scope, vectorQuery)
    vector3(scope, vectorQuery, anotherVectorQuery)
    vector2(scope, vectorQuery)
    vector4(scope)
    vectorAllOf(scope, vectorQuery, anotherVectorQuery)

    cluster.disconnect()
}

suspend fun vector1(scope: Scope, vectorQuery: FloatArray) {
    // tag::vector1[]
    val result = scope.search(
        indexName = "vector-index",
        spec = SearchSpec.vector("vector_field", vectorQuery),
    ).execute()
    // end::vector1[]

    for (row in result.rows) {
        println("${row.id}: ${row.score}")
    }
}

suspend fun vectorBase64(scope: Scope, base64Vector: String) {
    // tag::vector-base64[]
    // The vector is a Base64-encoded sequence of little-endian IEEE 754 floats.
    val result = scope.search(
        indexName = "vector-index",
        spec = SearchSpec.vector("vector_field", base64Vector),
    ).execute()
    // end::vector-base64[]
}

suspend fun prefilterMatch(scope: Scope, vectorQuery: FloatArray) {
    // tag::prefilter-match[]
    val result = scope.search(
        indexName = "vector-and-fts-index",
        spec = SearchSpec.vector(
            field = "vector_field",
            vector = vectorQuery,
            numCandidates = 5,
            prefilter = SearchSpec.match("primary", field = "color_wheel_pos"),
        ),
    ).execute()
    // end::prefilter-match[]
}

suspend fun prefilterQueryString(scope: Scope, vectorQuery: FloatArray) {
    // tag::prefilter-querystring[]
    val result = scope.search(
        indexName = "vector-and-fts-index",
        spec = SearchSpec.vector(
            field = "vector_field",
            vector = vectorQuery,
            prefilter = SearchSpec.queryString("+description:sea -color_hex:fff5ee"),
        ),
    ).execute()
    // end::prefilter-querystring[]
}

suspend fun vector3(scope: Scope, vectorQuery: FloatArray, anotherVectorQuery: FloatArray) {
    // tag::vector3[]
    val result = scope.search(
        indexName = "vector-and-fts-index",
        spec = SearchSpec.anyOf(
            SearchSpec.vector("vector_field", vectorQuery, numCandidates = 2).boost(0.3),
            SearchSpec.vector("vector_field", anotherVectorQuery, numCandidates = 5).boost(0.7),
        ),
    ).execute()
    // end::vector3[]
}

suspend fun vectorAllOf(scope: Scope, vectorQuery: FloatArray, anotherVectorQuery: FloatArray) {
    // tag::vector-allof[]
    // allOf combines the vector queries with AND, where anyOf uses OR.
    val result = scope.search(
        indexName = "vector-and-fts-index",
        spec = SearchSpec.allOf(
            SearchSpec.vector("vector_field", vectorQuery, numCandidates = 2),
            SearchSpec.vector("vector_field", anotherVectorQuery, numCandidates = 5),
        ),
    ).execute()
    // end::vector-allof[]
}

suspend fun vector2(scope: Scope, vectorQuery: FloatArray) {
    // tag::vector2[]
    val result = scope.search(
        indexName = "vector-index",
        spec = SearchSpec.mixedMode(
            SearchSpec.matchAll(),
            SearchSpec.vector("vector_field", vectorQuery),
        ),
    ).execute()
    // end::vector2[]
}

suspend fun vector4(scope: Scope) {
    // tag::vector4[]
    val result = scope.search(
        indexName = "travel-sample-index",
        spec = SearchSpec.matchAll(),
    ).execute()
    // end::vector4[]
}
