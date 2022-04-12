import com.couchbase.client.core.error.DocumentExistsException
import com.couchbase.client.core.error.DocumentNotFoundException
import com.couchbase.client.kotlin.Collection
import com.couchbase.client.kotlin.kv.GetResult

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
