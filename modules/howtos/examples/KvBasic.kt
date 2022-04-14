import com.couchbase.client.core.error.CasMismatchException
import com.couchbase.client.core.error.DocumentExistsException
import com.couchbase.client.core.error.DocumentNotFoundException
import com.couchbase.client.kotlin.Collection
import com.couchbase.client.kotlin.kv.Expiry
import com.couchbase.client.kotlin.kv.GetResult
import kotlin.time.Duration.Companion.hours

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
            content = mapOf("favoriteColor" to "blue"),
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
        println("The user's favorite color is ${content["favoriteColor"]}")
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
        println("The user's favorite color is ${content["favoriteColor"]}")
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


private suspend fun replacePreserveExpiryHardWayLocking(collection: Collection) {

// tag::replacePreserveExpiryHardWayLocking[]
    val documentId = "alice"

    while (true) {
        val old = collection.get(
            id = documentId,
            withExpiry = true, // <1>
        )

        val oldContent = old.contentAs<Map<String, Any?>>()
        val newContent = oldContent +
                ("favoriteFood" to "hamburger")

        try {
            collection.replace(
                id = documentId,
                content = newContent,
                expiry = old.expiry,
                cas = old.cas, // <2>
            )
            break

        } catch (_: CasMismatchException) {
            // Someone else changed the document,
            // and the expiry might have changed too.
            // Start again.
        }
    }
// end::replacePreserveExpiryHardWayLocking[]
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
