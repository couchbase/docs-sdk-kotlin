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
import com.couchbase.client.kotlin.Collection
import com.couchbase.client.kotlin.codec.Content
import com.couchbase.client.kotlin.codec.JacksonJsonSerializer
import com.couchbase.client.kotlin.codec.MoshiJsonSerializer
import com.couchbase.client.kotlin.codec.RawJsonTranscoder
import com.couchbase.client.kotlin.kv.GetResult
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

private suspend fun dataBindingMap(collection: Collection) {
// tag::dataBindingMap[]
    collection.upsert(
        id = "alice",
        content = mapOf("favoriteColor" to "blue"),
    )

    val result: GetResult = collection.get("alice")

    println("Content in Couchbase: " + String(result.content.bytes))
    println("Content as Map: " + result.contentAs<Map<String, Any?>>())
// end::dataBindingMap[]
}

private suspend fun dataBindingUserDefined(collection: Collection) {
    // tag::dataBindingUserDefined[]
    data class MyClass(
        val favoriteColor: String,
    )

    collection.upsert(
        id = "alice",
        content = MyClass(favoriteColor = "blue"),
    )

    val result: GetResult = collection.get("alice")

    println("Content in Couchbase: " + String(result.content.bytes))
    println("Content as MyClass: " + result.contentAs<MyClass>())
// end::dataBindingUserDefined[]
}

private suspend fun skipDataBinding(collection: Collection) {
    // tag::skipDataBinding[]
    val jsonContent = """{"favoriteColor":"blue"}"""

    // Option A
    collection.upsert(
        id = "alice",
        content = jsonContent,
        transcoder = RawJsonTranscoder, // <1>
    )

    // Option B
    collection.upsert(
        id = "alice",
        content = Content.json(jsonContent), // <1>
    )
// end::skipDataBinding[]
}

private suspend fun skipDataBindingRead(collection: Collection) {
    // tag::skipDataBindingRead[]
    val result: GetResult = collection.get(id = "alice")
    val jsonBytes: ByteArray = result.content.bytes // <1>
// end::skipDataBindingRead[]
}

private suspend fun failToSkipDataBinding(collection: Collection) {
    // tag::failToSkipDataBinding[]
    // Don't do this!
    collection.upsert(
        id = "alice",
        content = """{"favoriteColor":"blue"}"""
    )
// end::failToSkipDataBinding[]
}

private suspend fun dataBindingWithTreeNode(collection: Collection) {
    // tag::dataBindingWithTreeNode[]
    val json = collection.get(id = "alice").contentAs<JsonNode>()
    when {
        json is ArrayNode -> println("Content is a JSON Array")
        json is ObjectNode -> println("Content is a JSON Object")
        else -> println("Content is a JSON primitive")
    }
// end::dataBindingWithTreeNode[]
}

private fun getMyCustomJsonMapper() = jsonMapper()

private suspend fun customObjectMapper(
    collection: Collection,
    connectionString: String,
    username: String,
    password: String,
) {
    // tag::customObjectMapper[]
    val jsonMapper: JsonMapper = getMyCustomJsonMapper()

    val cluster = Cluster.connect(connectionString, username, password) {
        jsonSerializer = JacksonJsonSerializer(jsonMapper)
    }
// end::customObjectMapper[]
}

private suspend fun moshi(collection: Collection) {
    val connectionString = "127.0.0.1"
    val username = "Administrator"
    val password = "password"
    // tag::moshi[]
    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val cluster = Cluster.connect(connectionString, username, password) {
        jsonSerializer = MoshiJsonSerializer(moshi) // <1>
    }
// end::moshi[]
}
