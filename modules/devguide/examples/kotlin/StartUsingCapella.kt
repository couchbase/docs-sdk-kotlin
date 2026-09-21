/*
 * Copyright (c) 2026 Couchbase, Inc.
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

// tag::imports[]
import com.couchbase.client.core.error.CouchbaseException
import com.couchbase.client.kotlin.Cluster
import com.couchbase.client.kotlin.kv.Durability
import com.couchbase.client.kotlin.kv.Expiry
import com.couchbase.client.kotlin.kv.GetResult
import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
// end::imports[]

fun main() = runBlocking {
    // tag::connect[]
    // Update this to your cluster
    val endpoint = "cb.<your-endpoint>.cloud.couchbase.com"
    val username = "username"
    val password = "Password!123"
    val bucketName = "travel-sample"

    // For Capella, the connection string starts with "couchbases://"
    // (note the final 's') to enable a secure connection with TLS.
    val cluster = Cluster.connect(
        connectionString = "couchbases://$endpoint",
        username = username,
        password = password,
    ) {
        // Sets a pre-configured profile called "wan-development" to help avoid latency issues
        // when accessing Capella from a different Wide Area Network
        // or Availability Zone (e.g. your laptop).
        applyProfile("wan-development")
    }
    // end::connect[]

    // tag::bucket[]
    val bucket = cluster.bucket(bucketName).waitUntilReady(30.seconds)
    // end::bucket[]

    // tag::collection[]
    val collection = bucket.scope("inventory").collection("airport")
    // end::collection[]

    // tag::json[]
    val json = mapOf("status" to "awesome")
    // end::json[]

    // tag::upsert[]
    val docId = UUID.randomUUID().toString()
    try {
        collection.upsert(docId, json)
    } catch (e: CouchbaseException) {
        System.err.println("Error: ${e.message}")
    }
    // end::upsert[]

    // tag::get[]
    // Get a document
    try {
        val result: GetResult = collection.get(docId)
        val content = result.contentAs<Map<String, Any?>>()
        val status = content["status"]
        println("Couchbase is $status")
    } catch (e: Exception) {
        System.err.println("Error getting document: ${e.message}")
    }
    // end::get[]

    // tag::get-for[]
    try {
        val status = collection.get(docId)
            .contentAs<Map<String, Any?>>()["status"]
        println("Couchbase is $status")
    } catch (e: Exception) {
        System.err.println("Error: ${e.message}")
    }
    // end::get-for[]

    // tag::replace-options[]
    try {
        collection.replace(
            id = docId,
            content = json,
            expiry = Expiry.of(10.seconds),
            durability = Durability.majority(),
        )
    } catch (e: Exception) {
        System.err.println("Error: ${e.message}")
    }
    // end::replace-options[]

    // tag::replace-named[]
    try {
        collection.replace(
            id = docId,
            content = json,
            durability = Durability.majority(),
        )
    } catch (e: Exception) {
        System.err.println("Error: ${e.message}")
    }
    // end::replace-named[]

    cluster.disconnect()
}