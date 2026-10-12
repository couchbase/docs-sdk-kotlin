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
import com.couchbase.client.core.error.subdoc.PathExistsException
import com.couchbase.client.core.error.subdoc.PathNotFoundException
import com.couchbase.client.kotlin.Cluster
import com.couchbase.client.kotlin.Collection
import com.couchbase.client.kotlin.kv.LookupInSpec
import com.couchbase.client.kotlin.kv.StoreSemantics
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds
// end::imports[]

fun main() = runBlocking<Unit> {
    val cluster = Cluster.connect("127.0.0.1", "Administrator", "password")
    val bucket = cluster.bucket("travel-sample").waitUntilReady(10.seconds)
    val collection = bucket.scope("_default").collection("_default")

    mapExamples(collection)
    listExamples(collection)
    setExamples(collection)
    queueExamples(collection)

    cluster.disconnect()
}

suspend fun mapExamples(collection: Collection) {
    // tag::map-create[]
    // A map is just a JSON object stored in a document.
    collection.upsert("favorites", emptyMap<String, String>())
    // end::map-create[]

    // tag::map-put[]
    // Add (or overwrite) one key, without reading or sending the rest of the document.
    collection.mutateIn("favorites") {
        upsert("color", "blue")
    }
    // end::map-put[]

    // tag::map-get[]
    val spec = object : LookupInSpec() {
        val color = get("color")
    }
    val color: String = collection.lookupIn("favorites", spec) {
        with(spec) { color.contentAs<String>() }
    }
    println("Favorite color: $color")
    // end::map-get[]

    // tag::map-remove[]
    collection.mutateIn("favorites") {
        remove("color")
    }
    // end::map-remove[]
}

suspend fun listExamples(collection: Collection) {
    // tag::list-create[]
    // A list is just a JSON array stored in a document.
    collection.upsert("my-list", emptyList<String>())
    // end::list-create[]

    // tag::list-add[]
    collection.mutateIn("my-list") {
        arrayAppend("", listOf("last"))
        arrayPrepend("", listOf("first"))
    }
    // end::list-add[]

    // tag::list-get[]
    val spec = object : LookupInSpec() {
        val first = get("[0]")
        val size = count("")
    }
    collection.lookupIn("my-list", spec) {
        with(spec) {
            println("First element: ${first.contentAs<String>()}")
            println("List size: ${size.value}")
        }
    }
    // end::list-get[]

    // tag::list-set-remove[]
    collection.mutateIn("my-list") {
        replace("[0]", "new first value")
        remove("[-1]") // the last element
    }
    // end::list-set-remove[]
}

suspend fun setExamples(collection: Collection) {
    // tag::set-add[]
    // A set is a list that never contains the same value twice.
    collection.upsert("my-set", emptyList<String>())

    suspend fun addToSet(value: String): Boolean =
        try {
            collection.mutateIn("my-set") {
                arrayAddUnique("", value)
            }
            true // the value was added
        } catch (e: PathExistsException) {
            false // the value was already in the set
        }

    println(addToSet("apple")) // true
    println(addToSet("apple")) // false
    // end::set-add[]
}

// tag::queue[]
// A queue is a list where you add to one end and remove from the other.
// Here the "front" of the list is the newest item, and the last element is
// the oldest -- the next one to be removed.

suspend fun Collection.enqueue(queueId: String, item: String) {
    mutateIn(queueId) {
        arrayPrepend("", listOf(item))
    }
}

/** Removes and returns the oldest item, or null if the queue is empty. */
suspend fun Collection.dequeue(queueId: String): String? {
    while (true) {
        val spec = object : LookupInSpec() {
            val oldest = get("[-1]")
        }

        // Read the oldest item, and remember the document's CAS.
        val (item, cas) = try {
            lookupIn(queueId, spec) {
                with(spec) { oldest.contentAs<String>() } to cas
            }
        } catch (e: PathNotFoundException) {
            return null // the queue is empty
        }

        // Remove it, but only if nobody else changed the document in the meantime.
        try {
            mutateIn(queueId, storeSemantics = StoreSemantics.replace(cas = cas)) {
                remove("[-1]")
            }
            return item
        } catch (e: CasMismatchException) {
            // Somebody else changed the queue. Go around the loop and try again.
        }
    }
}
// end::queue[]

suspend fun queueExamples(collection: Collection) {
    collection.upsert("my-queue", emptyList<String>())

    // tag::queue-use[]
    collection.enqueue("my-queue", "job123")
    collection.enqueue("my-queue", "job124")

    println(collection.dequeue("my-queue")) // job123
    println(collection.dequeue("my-queue")) // job124
    println(collection.dequeue("my-queue")) // null
    // end::queue-use[]
}
