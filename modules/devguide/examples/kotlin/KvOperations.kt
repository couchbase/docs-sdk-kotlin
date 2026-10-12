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
import com.couchbase.client.core.error.CasMismatchException
import com.couchbase.client.core.error.CouchbaseException
import com.couchbase.client.core.error.DocumentExistsException
import com.couchbase.client.core.error.DocumentNotFoundException
import com.couchbase.client.core.error.DurabilityImpossibleException
import com.couchbase.client.core.error.ReplicaNotConfiguredException
import com.couchbase.client.kotlin.Bucket
import com.couchbase.client.kotlin.Cluster
import com.couchbase.client.kotlin.Collection
import com.couchbase.client.kotlin.Scope
import com.couchbase.client.kotlin.kv.CounterResult
import com.couchbase.client.kotlin.kv.Durability
import com.couchbase.client.kotlin.kv.Expiry
import com.couchbase.client.kotlin.kv.GetResult
import com.couchbase.client.kotlin.kv.MutationResult
import com.couchbase.client.kotlin.kv.PersistTo
import com.couchbase.client.kotlin.kv.ReplicateTo
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
// end::imports[]
import com.couchbase.client.core.error.FeatureNotAvailableException
import com.couchbase.client.kotlin.kv.ScanType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

// tag::bulk-get-helper[]
/**
 * Gets many documents at the same time, running at most
 * [maxConcurrency] requests concurrently.
 *
 * Returns a map where each key is a document ID, and each value
 * is a [Result] holding either the document or the failure.
 */
suspend fun Collection.bulkGet(
    ids: Iterable<String>,
    maxConcurrency: Int = 128,
): Map<String, Result<GetResult>> {
    val semaphore = Semaphore(maxConcurrency)

    // coroutineScope doesn't return until every child coroutine finishes.
    return coroutineScope {
        ids.map { id ->
            async {
                semaphore.withPermit {
                    id to runCatching { get(id) }
                        // Don't swallow coroutine cancellation.
                        .onFailure { if (it is CancellationException) throw it }
                }
            }
        }.awaitAll().toMap()
    }
}
// end::bulk-get-helper[]

fun main() = runBlocking<Unit> {
    val cluster = Cluster.connect("localhost", "Administrator", "password")
    val bucket: Bucket = cluster.bucket("travel-sample").waitUntilReady(10.seconds)
    val scope: Scope = bucket.scope("_default")
    val collection: Collection = scope.collection("_default")

    val json = mapOf(
        "title" to "My Blog Post",
        "author" to "mike",
    )

    // There is no Kotlin equivalent of the Java SDK's collection.async() and
    // collection.reactive(). Every Kotlin KV method is a suspend function, so
    // you just call it from a coroutine -- like this one, started by runBlocking.

    run {
        println("\nExample: [upsert]")
        // tag::upsert[]
        val content = mapOf(
            "author" to "mike",
            "title" to "My Blog Post 1",
        )
        val result: MutationResult = collection.upsert("document-key", content)
        // end::upsert[]
    }

    run {
        println("\nExample: [insert]")
        // tag::insert[]
        try {
            val content = mapOf("title" to "My Blog Post 2")
            val insertResult: MutationResult = collection.insert("document-key2", content)
        } catch (ex: DocumentExistsException) {
            System.err.println("The document already exists!")
        } catch (ex: CouchbaseException) {
            System.err.println("Something else happened: $ex")
        }
        // end::insert[]
    }

    run {
        println("\nExample: [get-simple]")
        // tag::get-simple[]
        try {
            val getResult: GetResult = collection.get("document-key")
            val title = getResult.contentAs<Map<String, Any?>>()["title"]
            println(title) // title == "My Blog Post 1"
        } catch (ex: DocumentNotFoundException) {
            println("Document not found!")
        }
        // end::get-simple[]
    }

    run {
        println("\nExample: [get-or-null]")
        // tag::get-or-null[]
        // getOrNull returns null instead of throwing DocumentNotFoundException.
        val getResult: GetResult? = collection.getOrNull("document-key")
        if (getResult == null) {
            println("Document not found!")
        } else {
            println(getResult.contentAs<Map<String, Any?>>()["title"])
        }
        // end::get-or-null[]
    }

    run {
        println("\nExample: [get]")
        // tag::get[]
        val found: GetResult = collection.get("document-key")
        val content = found.contentAs<Map<String, Any?>>()
        if (content["author"] == "mike") {
            // do something
        } else {
            // do something else
        }
        // end::get[]
    }

    run {
        println("\nExample: [get-project]")
        // tag::get-project[]
        // Fetch only the "title" field instead of the whole document.
        val partial: GetResult = collection.get("document-key", project = listOf("title"))
        println(partial.contentAs<Map<String, Any?>>())
        // end::get-project[]
    }

    run {
        println("\nExample: [replace]")
        // tag::replace[]
        collection.upsert("my-document", mapOf("initial" to true))

        val result: GetResult = collection.get("my-document")
        val content = result.contentAs<Map<String, Any?>>()
        // A Kotlin Map is immutable, so build a new Map with the changes.
        val newContent = content + mapOf("modified" to true, "initial" to false)
        collection.replace("my-document", newContent, cas = result.cas)
        // end::replace[]
    }

    run {
        println("\nExample: [replace-retry]")
        // tag::replace-retry[]
        val id = "my-document"
        collection.upsert(id, mapOf("initial" to true))

        while (true) {
            val found: GetResult = collection.get(id)
            val content = found.contentAs<Map<String, Any?>>()
            val newContent = content + mapOf("modified" to true, "initial" to false)

            try {
                collection.replace(id, newContent, cas = found.cas)
                break // if successful, break out of the retry loop
            } catch (ex: CasMismatchException) {
                // don't do anything, we'll retry the loop
            }
        }
        // end::replace-retry[]
    }

    run {
        println("\nExample: [pessimistic-lock]")
        // tag::pessimistic-lock[]
        val locked: GetResult = collection.getAndLock(
            id = "my-document",
            lockTime = 15.seconds, // between 1 and 30 seconds
        )
        val content = locked.contentAs<Map<String, Any?>>()

        // Replacing with the CAS value from getAndLock also releases the lock.
        // If you decide not to modify the document, call collection.unlock(id, cas) instead.
        collection.replace(
            id = "my-document",
            content = content + mapOf("modified" to true),
            cas = locked.cas,
        )
        // end::pessimistic-lock[]
    }

    run {
        println("\nExample: [remove]")
        // tag::remove[]
        try {
            collection.remove("my-document")
        } catch (ex: DocumentNotFoundException) {
            println("Document did not exist when trying to remove")
        }
        // end::remove[]
    }

    run {
        println("\nExample: [durability]")
        try {
            // tag::durability[]
            collection.upsert(
                id = "my-document",
                content = mapOf("doc" to true),
                durability = Durability.majority(),
            )
            // end::durability[]
        } catch (di: DurabilityImpossibleException) {
            println(di)
        }
    }

    run {
        println("\nExample: [durability-observed]")
        try {
            // tag::durability-observed[]
            collection.upsert(
                id = "my-document",
                content = mapOf("doc" to true),
                durability = Durability.clientVerified(PersistTo.NONE, ReplicateTo.TWO),
            )
            // end::durability-observed[]
        } catch (rnc: ReplicaNotConfiguredException) {
            println(rnc)
        }
    }

    run {
        println("\nExample: [expiry-insert]")
        // tag::expiry-insert[]
        val insertResult: MutationResult = collection.insert(
            id = "my-document2",
            content = json,
            expiry = Expiry.of(2.hours),
        )
        // end::expiry-insert[]
    }

    run {
        println("\nExample: [expiry-insert-instant]")
        // tag::expiry-insert-instant[]
        val insertResult: MutationResult = collection.insert(
            id = "my-document3",
            content = json,
            expiry = Expiry.of(Instant.now().plus(62, ChronoUnit.DAYS)),
        )
        // end::expiry-insert-instant[]
    }

    run {
        println("\nExample: [expiry-get]")
        // tag::expiry-get[]
        val result: GetResult = collection.get("my-document3", withExpiry = true)
        when (val expiry = result.expiry) {
            is Expiry.Absolute -> println("Expiry of found doc: ${expiry.instant}")
            else -> println("Document does not expire.")
        }
        // end::expiry-get[]
        println("cas value: ${result.cas}")
    }

    run {
        println("\nExample: [expiry-replace]")
        // tag::expiry-replace[]
        val found: GetResult = collection.get("my-document3", withExpiry = true)

        val result: MutationResult = collection.replace(
            id = "my-document3",
            content = json,
            expiry = found.expiry,
        )
        // end::expiry-replace[]
    }

    try {
        println("\nExample: [preserve-expiry]")
        // tag::preserve-expiry[]
        collection.replace(
            id = "my-document3",
            content = json,
            preserveExpiry = true,
        )
        // end::preserve-expiry[]
    } catch (e: FeatureNotAvailableException) {
        println("Couldn't run preserveExpiry example: $e")
    }

    run {
        println("\nExample: [expiry-touch]")
        // tag::expiry-touch[]
        val result: GetResult = collection.getAndTouch("my-document3", Expiry.of(1.days))
        // end::expiry-touch[]
        println("cas value: ${result.cas}")
    }

    run {
        println("\nExample: [counter]")
        // tag::counter[]
        // The counter document is created with initialValue if it doesn't exist.
        val counterResult: CounterResult = collection.binary.increment(
            id = "my-counter",
            delta = 1u,
            initialValue = 10u,
        )
        println("Counter is now ${counterResult.content}")
        // end::counter[]
    }

    run {
        println("\nExample: [named-collection-upsert]")
        // tag::named-collection-upsert[]
        val agentScope: Scope = bucket.scope("tenant_agent_00")
        val usersCollection: Collection = agentScope.collection("users")

        val content = mapOf(
            "name" to "John Doe",
            "preferred_email" to "johndoe111@test123.test",
        )
        val result: MutationResult = usersCollection.upsert("user-key", content)
        // end::named-collection-upsert[]
        println("cas value: ${result.cas}")
    }

    run {
        println("\nExample: [bulk-get]")
        // tag::bulk-get[]
        val ids = listOf("document-key", "document-key2", "no-such-document")

        collection.bulkGet(ids).forEach { (id, result) ->
            println("$id = $result")
        }
        // end::bulk-get[]
    }

    try {
        println("\nExample: [range-scan-range]")
        // tag::rangeScanAllDocuments[]
        val results: Flow<GetResult> = collection.scanDocuments(
            type = ScanType.range(), // <1>
        )
        results.collect { println(it) }
        // end::rangeScanAllDocuments[]
    } catch (e: FeatureNotAvailableException) {
        println("Couldn't run range-scan-range example: $e")
    }

    try {
        println("\nExample: [range-scan-prefix]")
        // tag::rangeScanPrefix[]
        val results: Flow<GetResult> = collection.scanDocuments(
            type = ScanType.prefix("alice::"), // <1>
        )
        results.collect { println(it) }
        // end::rangeScanPrefix[]
    } catch (e: FeatureNotAvailableException) {
        println("Couldn't run range-scan-prefix example: $e")
    }

    try {
        println("\nExample: [range-scan-sample]")
        // tag::rangeScanSample[]
        val results: Flow<GetResult> = collection.scanDocuments(
            type = ScanType.sample(limit = 100), // <1>
        )
        results.collect { println(it) }
        // end::rangeScanSample[]
    } catch (e: FeatureNotAvailableException) {
        println("Couldn't run range-scan-sample example: $e")
    }

    try {
        println("\nExample: [range-scan-ids-only]")
        // tag::rangeScanAllDocumentIds[]
        val ids: Flow<String> = collection.scanIds( // <1>
            type = ScanType.range(),
        )
        ids.collect { println(it) }
        // end::rangeScanAllDocumentIds[]
    } catch (e: FeatureNotAvailableException) {
        println("Couldn't run range-scan-ids-only example: $e")
    }

    // Cleans up example data from the _default collection
    // to avoid errors when running the sample code (mainly for `insert` examples).
    cleanupData(collection)

    cluster.disconnect()
}

private suspend fun cleanupData(collection: Collection) {
    println("\nCleaning up example data....")
    collection.remove("document-key2")
    collection.remove("my-document2")
    collection.remove("my-document3")
    collection.remove("my-counter")
}