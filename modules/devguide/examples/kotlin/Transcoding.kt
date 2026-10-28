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

import com.couchbase.client.core.error.DecodingFailureException
import com.couchbase.client.kotlin.Cluster
import com.couchbase.client.kotlin.Collection
import com.couchbase.client.kotlin.codec.Content
import com.couchbase.client.kotlin.codec.JacksonJsonSerializer
import com.couchbase.client.kotlin.codec.JsonSerializer
import com.couchbase.client.kotlin.codec.JsonTranscoder
import com.couchbase.client.kotlin.codec.RawBinaryTranscoder
import com.couchbase.client.kotlin.codec.RawJsonTranscoder
import com.couchbase.client.kotlin.codec.RawStringTranscoder
import com.couchbase.client.kotlin.codec.Transcoder
import com.couchbase.client.kotlin.codec.TypeRef
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.msgpack.MessagePack
import kotlin.time.Duration.Companion.seconds

// tag::gson-serializer[]
class GsonSerializer : JsonSerializer {
    private val gson = Gson()

    override fun <T> serialize(value: T, type: TypeRef<T>): ByteArray =
        gson.toJson(value).encodeToByteArray()

    override fun <T> deserialize(json: ByteArray, type: TypeRef<T>): T =
        gson.fromJson(json.decodeToString(), type.type)
}
// end::gson-serializer[]

// tag::msgpack-serializer[]
class MsgPackSerializer : JsonSerializer {
    private val msgpack = MessagePack()

    override fun <T> serialize(value: T, type: TypeRef<T>): ByteArray =
        msgpack.write(value)

    @Suppress("UNCHECKED_CAST")
    override fun <T> deserialize(json: ByteArray, type: TypeRef<T>): T {
        // MessagePack can only decode into a plain class, not a generic type like List<String>.
        val target = type.type as? Class<T>
            ?: throw DecodingFailureException("Does not support decoding to generic type $type.")
        return msgpack.read(json, target)
    }
}
// end::msgpack-serializer[]

// tag::msgpack-transcoder[]
class MsgPackTranscoder : Transcoder {
    private val serializer = MsgPackSerializer()

    override fun <T> doEncode(input: T, type: TypeRef<T>): Content =
        Content.binary(serializer.serialize(input, type))

    override fun <T> decode(content: Content, type: TypeRef<T>): T =
        serializer.deserialize(content.bytes, type)
}
// end::msgpack-transcoder[]

data class User(val name: String, val age: Int)

fun main() = runBlocking<Unit> {
    val cluster = Cluster.connect("127.0.0.1", "Administrator", "password")
    val bucket = cluster.bucket("travel-sample").waitUntilReady(30.seconds)
    val scope = bucket.scope("tenant_agent_00")
    val collection = scope.collection("users")

    gson(collection)
    gsonCustom(collection)
    gsonRegister()
    customJackson()
    msgpack(collection)
    string(collection)
    binary(collection)
    contentPassthrough(collection)

    cluster.disconnect()
}

suspend fun gson(collection: Collection) {
    // tag::gson-encode[]
    // User is a simple data class
    val user = User("John Smith", 27)

    val gson = Gson()
    val json = gson.toJson(user)

    collection.upsert("john-smith", json, transcoder = RawJsonTranscoder)
    // end::gson-encode[]

    // tag::gson-decode[]
    val result = collection.get("john-smith")

    val returnedJson = result.contentAs<String>(RawJsonTranscoder)
    val returnedUser = gson.fromJson(returnedJson, User::class.java)
    // end::gson-decode[]

    check(json == returnedJson)
    check(user == returnedUser)
}

suspend fun gsonCustom(collection: Collection) {
    // tag::gson-custom-encode[]
    val serializer = GsonSerializer()
    val transcoder = JsonTranscoder(serializer)

    val user = User("John Smith", 27)

    collection.upsert("john-smith", user, transcoder = transcoder)
    // end::gson-custom-encode[]

    // tag::gson-custom-decode[]
    val result = collection.get("john-smith")

    val returnedUser = result.contentAs<User>(transcoder)

    check(user == returnedUser)
    // end::gson-custom-decode[]
}

suspend fun gsonRegister() {
    // tag::gson-register-1[]
    val cluster = Cluster.connect("localhost", "Administrator", "password") {
        jsonSerializer = GsonSerializer()
    }
    // end::gson-register-1[]

    val coll = cluster.bucket("default").defaultCollection()

    // tag::gson-register-2[]
    val user = User("John Smith", 27)

    coll.upsert("john-smith", user)

    val result = coll.get("john-smith")

    val returnedUser = result.contentAs<User>()
    // end::gson-register-2[]

    check(user == returnedUser)

    cluster.disconnect()
}

suspend fun customJackson() {
    // tag::custom-jackson[]
    val customMapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build()) // keeps support for Kotlin data classes
        // add other modules or customize other settings
        .build()

    val cluster = Cluster.connect("localhost", "Administrator", "password") {
        jsonSerializer = JacksonJsonSerializer(customMapper)
    }
    // end::custom-jackson[]

    cluster.disconnect()
}

suspend fun msgpack(collection: Collection) {
    // tag::msgpack-encode[]
    val transcoder = MsgPackTranscoder()

    val input = "hello world!"

    collection.upsert("msgpack-doc", input, transcoder = transcoder)

    val result = collection.get("msgpack-doc")

    val output = result.contentAs<String>(transcoder)
    // end::msgpack-encode[]

    check(input == output)
}

suspend fun string(collection: Collection) {
    val docId = "doc"
    // tag::string[]
    collection.upsert(docId, "hello world", transcoder = RawStringTranscoder)

    val result = collection.get(docId)

    val returned = result.contentAs<String>(RawStringTranscoder)
    // end::string[]
    check(returned == "hello world")
}

suspend fun binary(collection: Collection) {
    val docId = "doc"
    // tag::binary[]
    val input = "hello world"
    val bytes = input.encodeToByteArray()

    collection.upsert(docId, bytes, transcoder = RawBinaryTranscoder)

    val result = collection.get(docId)

    val returned = result.contentAs<ByteArray>(RawBinaryTranscoder)
    // end::binary[]
    check(returned.contentEquals(bytes))
}

suspend fun contentPassthrough(collection: Collection) {
    // tag::content-passthrough[]
    // Content that is already encoded skips the transcoder completely.
    collection.upsert("binary-doc", Content.binary(byteArrayOf(1, 2, 3)))
    collection.upsert("json-doc", Content.json("""{"name":"John Smith","age":27}"""))
    collection.upsert("string-doc", Content.string("hello world"))

    // The raw bytes of any document are available without a transcoder.
    val rawBytes: ByteArray = collection.get("binary-doc").content.bytes
    // end::content-passthrough[]
    check(rawBytes.contentEquals(byteArrayOf(1, 2, 3)))
}
