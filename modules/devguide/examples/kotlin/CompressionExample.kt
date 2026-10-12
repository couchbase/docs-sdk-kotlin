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

import com.couchbase.client.kotlin.Cluster
import kotlinx.coroutines.runBlocking

fun main() = runBlocking<Unit> {
    compression1()
    compression2()
    compression3()
    println("Done.")
}

suspend fun compression1() {
    // tag::compression_1[]
    val cluster = Cluster.connect(
        connectionString = "localhost",
        username = "Administrator",
        password = "password",
    ) {
        compression {
            // start compressing at 1024 bytes
            minSize = 1024
        }
    }
    // end::compression_1[]
    cluster.disconnect()
}

suspend fun compression2() {
    // tag::compression_2[]
    val cluster = Cluster.connect(
        connectionString = "localhost",
        username = "Administrator",
        password = "password",
    ) {
        compression {
            // never compress documents
            enable = false
        }
    }
    // end::compression_2[]
    cluster.disconnect()
}

suspend fun compression3() {
    // tag::compression_3[]
    val cluster = Cluster.connect(
        connectionString = "localhost",
        username = "Administrator",
        password = "password",
    ) {
        compression {
            // send the compressed form only if it is at most 70% of the original size
            minRatio = 0.7
        }
    }
    // end::compression_3[]
    cluster.disconnect()
}
