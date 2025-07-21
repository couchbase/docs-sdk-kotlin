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

@file:Suppress("unused", "RemoveRedundantQualifierName")

import com.couchbase.client.core.error.CasMismatchException
import com.couchbase.client.core.error.DocumentExistsException
import com.couchbase.client.core.error.DocumentNotFoundException
import com.couchbase.client.kotlin.Collection
import com.couchbase.client.kotlin.CommonOptions
import com.couchbase.client.kotlin.codec.Transcoder
import com.couchbase.client.kotlin.kv.Durability
import com.couchbase.client.kotlin.kv.Expiry
import com.couchbase.client.kotlin.kv.GetResult
import com.couchbase.client.kotlin.kv.MutationResult
import com.couchbase.client.kotlin.kv.ScanType
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

private suspend fun upsert(collection: Collection) {
// tag::upsert[]
    collection.upsert(
        id = "alice",
        content = mapOf("favoriteColor" to "blue"),
    )
// end::upsert[]
}


private suspend fun insert(collection: Collection) {
// tag::insert[]
    try {
        collection.insert(
            id = "alice",
            content = mapOf("favoriteColor" to "blue"), // <1>
        )
    } catch (t: DocumentExistsException) {
        println("Insert failed because the document already exists.")
    }
// end::insert[]
}

private suspend fun replace(collection: Collection) {
// tag::replace[]
    try {
        collection.replace(
            id = "alice",
            content = mapOf("favoriteColor" to "red"),
        )
    } catch (t: DocumentNotFoundException) {
        println("Replace failed because there was no document to replace.")
    }
// end::replace[]
}

private suspend fun remove(collection: Collection) {
// tag::remove[]
    try {
        collection.remove(id = "alice")
    } catch (t: DocumentNotFoundException) {
        println("Remove failed because there was no document to remove.")
    }
// end::remove[]
}

private suspend fun get(collection: Collection) {
// tag::get[]
    try {
        val result: GetResult = collection.get(id = "alice")
        val content = result.contentAs<Map<String, Any?>>()
        println("The character's favorite color is ${content["favoriteColor"]}")
    } catch (t: DocumentNotFoundException) {
        println("Get failed because the document does not exist.")
    }
// end::get[]
}

private suspend fun upsertWithExpiry(collection: Collection) {
// tag::upsertWithExpiry[]
    collection.upsert(
        id = "alice",
        content = mapOf("favoriteColor" to "red"),
        expiry = Expiry.of(3.hours), // <1>
    )
// end::upsertWithExpiry[]
}


private suspend fun touch(collection: Collection) {
// tag::touch[]
    try {
        collection.touch(
            id = "alice",
            expiry = Expiry.of(3.hours),
        )
    } catch (t: DocumentNotFoundException) {
        println("Touch failed because the document does not exist.")
    }
// end::touch[]
}

private suspend fun getAndTouch(collection: Collection) {
// tag::getAndTouch[]
    try {
        val result: GetResult = collection.getAndTouch(
            id = "alice",
            expiry = Expiry.of(3.hours), // <1>
        )
        val content = result.contentAs<Map<String, Any?>>()
        println("The character's favorite color is ${content["favoriteColor"]}")
    } catch (t: DocumentNotFoundException) {
        println("GetAndTouch failed because the document does not exist.")
    }
// end::getAndTouch[]
}

private suspend fun replacePreserveExpiry(collection: Collection) {
// tag::replacePreserveExpiry[]
    collection.replace(
        id = "alice",
        content = mapOf("favoriteColor" to "red"),
        preserveExpiry = true,
    )
// end::replacePreserveExpiry[]
}

private suspend fun replacePreserveExpiryHardWay(collection: Collection) {
// tag::replacePreserveExpiryHardWay[]
    val documentId = "alice"
    val old = collection.get(
        id = documentId,
        withExpiry = true, // <1>
    )
    collection.replace(
        id = documentId,
        content = mapOf("favoriteColor" to "red"),
        expiry = old.expiry,
    )
    // end::replacePreserveExpiryHardWay[]
}


private suspend fun getWithExpiry(collection: Collection) {
// tag::getWithExpiry[]
    val result: GetResult = collection.get(
        id = "alice",
        withExpiry = true, // <1>
    )

    when (val expiry = result.expiry) {
        is Expiry.None -> println("Document does not expire.")
        is Expiry.Absolute -> println("Document expires at ${expiry.instant}.")
        else -> println("Oops, forgot to pass `withExpiry = true`.")
    }
// end::getWithExpiry[]
}


private suspend fun replaceWithCas(collection: Collection) {
// tag::replaceWithCas[]
    while (true) { // <1>
        val result: GetResult = collection.get(id = "alice")

        val oldContent = result.contentAs<Map<String, Any?>>()
        val newContent = oldContent + ("favoriteFood" to "hamburger")

        try {
            collection.replace(
                id = "alice",
                content = newContent,
                cas = result.cas
            )
            return

        } catch (t: CasMismatchException) {
            // Someone else changed the document after we read it!
            // Start again.
        }
    }
// end::replaceWithCas[]
}

// tag::mutate[]
suspend inline fun <reified T> Collection.mutate(
    id: String,
    expiry: Expiry = Expiry.none(),
    preserveExpiry: Boolean = false,
    transcoder: Transcoder? = null,
    durability: Durability = Durability.none(),
    common: CommonOptions = CommonOptions.Default,
    transform: (GetResult) -> T,
): MutationResult {
    while (true) {
        val old = get(
            id = id,
            withExpiry = preserveExpiry,
            common = common,
        )

        val newContent = transform(old)
        val newExpiry = if (preserveExpiry) old.expiry else expiry

        try {
            return replace(
                id = id,
                content = newContent,
                common = common,
                transcoder = transcoder,
                durability = durability,
                expiry = newExpiry,
                cas = old.cas
            )
        } catch (_: CasMismatchException) {
            // Someone else modified the document. Start again.
        }
    }
}
// end::mutate[]


private suspend fun callingMutate(collection: Collection) {
// tag::callingMutate[]
    collection.mutate("alice") { old: GetResult ->
        val oldContent = old.contentAs<Map<String, Any?>>()
        return@mutate oldContent + ("favoriteFood" to "hamburger")
    }
// end::callingMutate[]
}

private suspend fun pessimisticLocking(collection: Collection) {
// tag::pessimisticLocking[]
    val result: GetResult = collection.getAndLock(
        id = "alice",
        lockTime = 15.seconds, // <1>
    )

    val oldContent = result.contentAs<Map<String, Any?>>()
    val newContent = oldContent + ("favoriteFood" to "hamburger")

    collection.replace( // <2>
        id = "alice",
        content = newContent,
        cas = result.cas,
    )
// end::pessimisticLocking[]
}


// tag::bulkGet[]
/**
 * Gets many documents at the same time.
 *
 * @param ids The IDs of the documents to get.
 * @param maxConcurrency Limits how many operations happen
 * at the same time.
 * @return A map where the key is a document ID, and the value
 * is a [kotlin.Result] indicating success or failure.
 */
suspend fun com.couchbase.client.kotlin.Collection.bulkGet(
    ids: Iterable<String>,
    maxConcurrency: Int = 128,
): Map<String, Result<GetResult>> {
    val result = ConcurrentHashMap<String, Result<GetResult>>()
    val semaphore = kotlinx.coroutines.sync.Semaphore(maxConcurrency)

    coroutineScope { // <1>
        ids.forEach { id ->
            launch { // <2>
                semaphore.withPermit { // <3>
                    result[id] = runCatching { get(id) }
                }
            }
        }
    }
    return result
}
// end::bulkGet[]

suspend fun callBulkGet(collection: Collection)  {
// tag::callBulkGet[]
    val ids = listOf("airline_10", "airline_10123", "airline_10226")

    collection.bulkGet(ids).forEach { (id, result) ->
        println("$id = $result")
    }
// end::callBulkGet[]
}

suspend fun rangeScanAllDocuments(collection: Collection)  {
// tag::rangeScanAllDocuments[]
    val results: Flow<GetResult> = collection.scanDocuments(
        type = ScanType.range() // <1>
    )
    results.collect { println(it) }
// end::rangeScanAllDocuments[]
}

suspend fun rangeScanAllDocumentIds(collection: Collection)  {
// tag::rangeScanAllDocumentIds[]
    val ids: Flow<String> = collection.scanIds( // <1>
        type = ScanType.range()
    )
    ids.collect { println(it) }
// end::rangeScanAllDocumentIds[]
}


suspend fun rangeScanPrefix(collection: Collection)  {
// tag::rangeScanPrefix[]
    val results: Flow<GetResult> = collection.scanDocuments(
        type = ScanType.prefix("alice::") // <1>
    )
    results.collect { println(it) }
// end::rangeScanPrefix[]
}

suspend fun rangeScanSample(collection: Collection)  {
// tag::rangeScanSample[]
    val results: Flow<GetResult> = collection.scanDocuments(
        type = ScanType.sample(limit = 100)
    )
    results.collect { println(it) }
// end::rangeScanSample[]
}
