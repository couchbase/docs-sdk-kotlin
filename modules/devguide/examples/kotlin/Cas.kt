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

import com.couchbase.client.core.error.CasMismatchException
import com.couchbase.client.kotlin.Bucket
import com.couchbase.client.kotlin.Cluster
import com.couchbase.client.kotlin.Collection
import com.couchbase.client.kotlin.Scope
import com.couchbase.client.kotlin.kv.GetResult
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

fun main() = runBlocking<Unit> {
    val cluster = Cluster.connect("localhost", "Administrator", "password")
    val bucket: Bucket = cluster.bucket("travel-sample").waitUntilReady(10.seconds)
    val scope: Scope = bucket.scope("inventory")
    val collection: Collection = scope.collection("airline")

    run {
        // tag::handlingerrors[]
        val maxRetries = 10
        for (i in 0 until maxRetries) {
            // Get the current document contents
            val getResult: GetResult = collection.get("airline_10")

            // Increment id on the airline.
            // A Kotlin Map is immutable, so build a new Map with the change.
            val content = getResult.contentAs<Map<String, Any?>>()
            val newId = (content.getValue("id") as Number).toLong() + 1
            val newContent = content + ("id" to newId)

            try {
                // Attempt to replace the document with cas
                collection.replace("airline_10", newContent, cas = getResult.cas)
                break
            } catch (ex: CasMismatchException) {
                // continue the loop on cas mismatch to try again
                // note that any other exception will be raised and break the loop as well
            }
        }
        // end::handlingerrors[]
    }

    run {
        // tag::locking[]
        val getAndLockResult: GetResult = collection.getAndLock("airline_1191", lockTime = 2.seconds)
        val lockedCas: Long = getAndLockResult.cas

        /*
         * an example of simply unlocking the document: collection.unlock("key", lockedCas)
         */

        collection.replace("airline_1191", "new value", cas = lockedCas)
        // end::locking[]
    }

    cluster.disconnect()
}