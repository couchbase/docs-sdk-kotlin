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
import com.couchbase.client.core.error.DocumentUnretrievableException
import com.couchbase.client.core.error.DurabilityImpossibleException
import com.couchbase.client.core.error.subdoc.PathExistsException
import com.couchbase.client.core.error.subdoc.PathNotFoundException
import com.couchbase.client.kotlin.Bucket
import com.couchbase.client.kotlin.Cluster
import com.couchbase.client.kotlin.Collection
import com.couchbase.client.kotlin.Scope
import com.couchbase.client.kotlin.kv.Durability
import com.couchbase.client.kotlin.kv.LookupInSpec
import com.couchbase.client.kotlin.kv.MutateInSpec
import com.couchbase.client.kotlin.kv.PersistTo
import com.couchbase.client.kotlin.kv.ReplicateTo
import com.couchbase.client.kotlin.kv.StoreSemantics
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds
// end::imports[]

fun main() = runBlocking<Unit> {
    val cluster = Cluster.connect("127.0.0.1", "Administrator", "password")
    val bucket: Bucket = cluster.bucket("travel-sample")
    val scope: Scope = bucket.scope("inventory")
    val collection: Collection = scope.collection("hotel")

    bucket.waitUntilReady(10.seconds)

    getFunc(collection)
    existsFunc(collection)
    combine(collection)
    upsertFunc(collection)
    insertFunc(collection)
    multiFunc(collection)
    arrayAppendObjFunc(collection)
    arrayAppendFunc(collection)
    arrayPrependObjFunc(collection)
    arrayPrependFunc(collection)
    fullDocReplaceFunc(collection)
    createAndPopulateArrays(collection)
    arrayCreate(collection)
    arrayUnique(collection)
    arrayInsertFunc(collection)
    counterInc(collection)
    counterDec(collection)
    createPath(collection)
    concurrent(collection)
    cas(collection)
    casFail(collection)
    oldDurability(collection)
    newDurability(collection)

    cluster.disconnect()
}

// There are no Kotlin equivalents of the Java SDK's get-future and get-reactive
// examples, because there is no collection.async() or collection.reactive().
// Every Kotlin KV method is a suspend function, so the examples below are
// already asynchronous. See the concurrent() example for running several
// operations at the same time.

suspend fun getFunc(collection: Collection) {
    // tag::get[]
    // A LookupInSpec describes which paths to fetch. Declare each path as a property
    // of the spec, so you can use it later to pick that path's value out of the result.
    val spec = object : LookupInSpec() {
        val lat = get("geo.lat")
    }

    try {
        collection.lookupIn("hotel_1368", spec) {
            with(spec) {
                println("getFunc: Latitude = ${lat.contentAs<Double>()}")
            }
        }
    } catch (e: PathNotFoundException) {
        e.printStackTrace()
    }
    // end::get[]
}

suspend fun existsFunc(collection: Collection) {
    // tag::exists[]
    val spec = object : LookupInSpec() {
        val doesNotExist = exists("address.does_not_exist")
    }

    collection.lookupIn("hotel_1368", spec) {
        val pathExists = with(spec) { doesNotExist.value }
        println("Non-existent path exists? $pathExists")
    }
    // end::exists[]
}

suspend fun combine(collection: Collection) {
    // tag::combine[]
    val spec = object : LookupInSpec() {
        val lat = get("geo.lat")
        val otherExists = exists("address.does_not_exist")
        val likeCount = count("public_likes")
    }

    collection.lookupIn("hotel_1368", spec) {
        with(spec) {
            println("Latitude: ${lat.contentAs<Double>()}")
            println("Non-existent path exists? ${otherExists.value}")
            println("Number of likes: ${likeCount.value}")
        }
    }
    // end::combine[]
}

suspend fun upsertFunc(collection: Collection) {
    // tag::upsert[]
    collection.mutateIn("hotel_1368") {
        upsert("email", "hotel96@hotmail.com")
    }
    // end::upsert[]
    println("upsertFunc: hotel_1368 email")
}

suspend fun insertFunc(collection: Collection) {
    // tag::insert[]
    try {
        collection.mutateIn("hotel_1368") {
            insert("alt_email", "alt_hotel96@hotmail.com")
        }
    } catch (err: PathExistsException) {
        println("insertFunc: exception caught, path already exists")
    }
    // end::insert[]
}

suspend fun multiFunc(collection: Collection) {
    try {
        collection.mutateIn("hotel_1368") { upsert("tz", "EST") }
    } catch (e: PathExistsException) {
    }

    try {
        collection.mutateIn("hotel_1368") { remove("alt_email") }
    } catch (e: PathNotFoundException) {
    }

    // tag::multi[]
    collection.mutateIn("hotel_1368") {
        remove("tz")
        insert("alt_email", "hotel84@hotmail.com")
    }
    // end::multi[]
    println("upsertFunc: hotel_1368 alt_email")
}

suspend fun arrayAppendObjFunc(collection: Collection) {
    val docContent = mapOf(
        "content" to "very nice hotel",
        "author" to "Nedra Cronin",
        "date" to "2012-01-14 02:15:51 +0300",
    )

    // tag::array-appendobj[]
    collection.mutateIn("hotel_1368") {
        arrayAppend("reviews", listOf(docContent))
    }
    // end::array-appendobj[]
    println("arrayAppendObjFunc: hotel_1368 reviews")
}

suspend fun arrayAppendFunc(collection: Collection) {
    // tag::array-append[]
    val result = collection.mutateIn("hotel_1368") {
        arrayAppend("public_likes", listOf("Mike Rutherford"))
    }
    /*
      public_likes is now:
      ["Georgette Rutherford V", "Ms. Devante Bruen", "Anderson Schmidt", "Mr. Kareem Harvey", "Tessie Shields",
      "Floyd Bradtke III", "Maurice McDermott", "Michel Franecki", "Laila Ernser", "Mike Rutherford"]
    */
    // end::array-append[]
}

suspend fun arrayPrependObjFunc(collection: Collection) {
    val docContent = mapOf(
        "content" to "not a very nice hotel",
        "author" to "Terry Smith",
        "date" to "2013-01-14 03:16:51 +0300",
    )

    // tag::array-prependobj[]
    collection.mutateIn("hotel_1501") {
        arrayPrepend("reviews", listOf(docContent))
    }
    // end::array-prependobj[]
    println("arrayAppendObjFunc: hotel_1501 reviews")
}

suspend fun arrayPrependFunc(collection: Collection) {
    // tag::array-prepend[]
    val result = collection.mutateIn("hotel_1368") {
        arrayPrepend("public_likes", listOf("John Smith"))
    }
    /*
      public_likes is now:
      ["John Smith", "Georgette Rutherford V", "Ms. Devante Bruen", "Anderson Schmidt", "Mr. Kareem Harvey", "Tessie Shields",
      "Floyd Bradtke III", "Maurice McDermott", "Michel Franecki", "Laila Ernser", "Mike Rutherford"]
    */
    // end::array-prepend[]
}

suspend fun fullDocReplaceFunc(collection: Collection) {
    // tag::full_doc_replace[]
    val docContent = mapOf("body" to "value")
    collection.mutateIn("hotel_14006") {
        upsert("foo", "bar", xattr = true)
        replace("", docContent)
    }
    // end::full_doc_replace[]
    println("fullDocReplaceFunc: hotel_14006 foo")
}

suspend fun createAndPopulateArrays(collection: Collection) {
    // tag::array-create[]
    collection.upsert("my_array", emptyList<String>())

    collection.mutateIn("my_array") {
        arrayAppend("", listOf("some element"))
    }
    // the document my_array is now ["some element"]
    // end::array-create[]
    println("createAndPopulateArrays: my_array some element")
}

suspend fun arrayCreate(collection: Collection) {
    // tag::array-upsert[]
    // Missing parent paths ("some") are created automatically.
    val result = collection.mutateIn("hotel_14225") {
        arrayAppend("some.array", listOf("hello world"))
    }
    // end::array-upsert[]
    println("arrayCreate: hotel_14225 some.array $result")
}

suspend fun arrayUnique(collection: Collection) {
    collection.mutateIn("hotel_14226") { upsert("unique", listOf(88)) }

    // tag::array-unique[]
    collection.mutateIn("hotel_14226") { arrayAddUnique("unique", 95) }

    try {
        collection.mutateIn("hotel_14226") { arrayAddUnique("unique", 95) }
        throw RuntimeException("should have thrown PathExistsException")
    } catch (err: PathExistsException) {
        println("arrayUnique: caught exception, path already exists")
    }
    // end::array-unique[]
}

suspend fun arrayInsertFunc(collection: Collection) {
    collection.mutateIn("hotel_1501") { upsert("foo", listOf(88)) }

    // tag::array-insert[]
    val result = collection.mutateIn("hotel_1501") {
        arrayInsert("foo[1]", listOf("cruel"))
    }
    // end::array-insert[]
    println("arrayInsertFunc: hotel_1501 foo $result")
}

suspend fun counterInc(collection: Collection) {
    collection.mutateIn("hotel_1368") { upsert("logins", 1) }

    // tag::counter-inc[]
    val spec = MutateInSpec()
    val logins = spec.incrementAndGet("logins", delta = 1)

    val result = collection.mutateIn("hotel_1368", spec)

    // Counter operations return the updated count
    val count: Long = logins.get(result)
    // end::counter-inc[]
    println("counterInc: hotel_1368 logins $count")
}

suspend fun counterDec(collection: Collection) {
    collection.mutateIn("hotel_1368") { upsert("logouts", 1000) }

    // tag::counter-dec[]
    val spec = MutateInSpec()
    val logouts = spec.decrementAndGet("logouts", delta = 150)

    val result = collection.mutateIn("hotel_1368", spec)

    // Counter operations return the updated count
    val count: Long = logouts.get(result)
    // end::counter-dec[]
    println("counterDec: hotel_1368 logouts $count")
}

suspend fun createPath(collection: Collection) {
    // tag::create-path[]
    // By default, upsert creates any missing parent paths
    // ("level_0", "level_1", "foo", and "bar" in this example).
    val result = collection.mutateIn("hotel_1368") {
        upsert(
            "level_0.level_1.foo.bar.phone",
            mapOf("num" to "311-555-0101", "ext" to 16),
        )
    }
    // end::create-path[]
    println("createPath: hotel_1368 $result")
}

suspend fun noCreatePath(collection: Collection) {
    // tag::no-create-path[]
    // To turn off automatic creation of parent paths, pass createParent = false.
    // The mutation then fails with PathNotFoundException if a parent is missing.
    try {
        collection.mutateIn(
            id = "hotel_1368",
            storeSemantics = StoreSemantics.replace(createParent = false),
        ) {
            upsert("level_0.level_1.foo.bar.phone", mapOf("num" to "311-555-0101"))
        }
    } catch (e: PathNotFoundException) {
        println("noCreatePath: a parent path does not exist")
    }
    // end::no-create-path[]
}

suspend fun lookupInAnyReplica(collection: Collection) {
    // tag::lookup-in-any-replica[]
    val spec = object : LookupInSpec() {
        val lat = get("geo.lat")
    }

    try {
        collection.lookupInAnyReplica("hotel_1368", spec) {
            with(spec) {
                println("getFunc: Latitude = ${lat.contentAs<Double>()}")
            }
        }
    } catch (e: PathNotFoundException) {
        println(
            "The version of the document" +
                " on the server node that responded quickest" +
                " did not have the requested field."
        )
    } catch (e: DocumentUnretrievableException) {
        println("Document was not present on any server node.")
    }
    // end::lookup-in-any-replica[]
}

suspend fun lookupInAllReplicas(collection: Collection) {
    // tag::lookup-in-all-replicas[]
    val spec = object : LookupInSpec() {
        val lat = get("geo.lat")
    }

    collection.lookupInAllReplicas("hotel_1368", spec).collect { replicaResult ->
        try {
            with(replicaResult) {
                with(spec) {
                    println("getFunc: Latitude = ${lat.contentAs<Double>()}")
                }
            }
        } catch (e: PathNotFoundException) {
            println(
                "The version of the document" +
                    " on one of the server nodes" +
                    " did not have the requested field."
            )
        }
    }
    // end::lookup-in-all-replicas[]
}

suspend fun concurrent(collection: Collection) {
    // tag::concurrent[]
    // coroutineScope waits for both coroutines to finish.
    coroutineScope {
        launch {
            collection.mutateIn("hotel_1501") {
                arrayAppend("foo", listOf(99))
            }
        }
        launch {
            collection.mutateIn("hotel_1501") {
                arrayAppend("foo", listOf(101))
            }
        }
    }
    // end::concurrent[]

    val doc = collection.get("hotel_1501")
    println("concurrent: hotel_1501 $doc")
}

suspend fun cas(collection: Collection) {
    // tag::cas[]
    val doc = collection.get("hotel_1368")

    val result = collection.mutateIn(
        id = "hotel_1368",
        storeSemantics = StoreSemantics.replace(cas = doc.cas),
    ) {
        decrementAndGet("logouts", delta = 150)
    }
    // end::cas[]
    println("cas: hotel_1368 $doc")
}

suspend fun casFail(collection: Collection) {
    // tag::cas_fail[]
    val doc = collection.get("hotel_1368")

    try {
        val result = collection.mutateIn(
            id = "hotel_1368",
            storeSemantics = StoreSemantics.replace(cas = doc.cas + 1),
        ) {
            decrementAndGet("logouts", delta = 150)
        }
    } catch (e: CasMismatchException) {
        println("cas_fail: $e")
    }
    // end::cas_fail[]
}

suspend fun oldDurability(collection: Collection) {
    try {
        // tag::old-durability[]
        val result = collection.mutateIn(
            id = "hotel_1368",
            durability = Durability.clientVerified(PersistTo.ACTIVE, ReplicateTo.ONE),
        ) {
            upsert("foo", "bar")
        }
        // end::old-durability[]
        println("oldDurability: $result")
    } catch (e: RuntimeException) {
        println("oldDurability: replication not possible: $e")
    }
}

suspend fun newDurability(collection: Collection) {
    try {
        // tag::new-durability[]
        val result = collection.mutateIn(
            id = "hotel_1368",
            durability = Durability.majority(),
        ) {
            upsert("foo", "bar")
        }
        // end::new-durability[]
        println("newDurability: $result")
    } catch (e: DurabilityImpossibleException) {
        println("newDurability: replication not possible: $e")
    }
}
