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

import com.couchbase.client.core.cnc.Event
import com.couchbase.client.core.logging.LogRedaction
import com.couchbase.client.core.logging.RedactionLevel
import com.couchbase.client.kotlin.Cluster
import com.couchbase.client.kotlin.env.ClusterEnvironment
import kotlinx.coroutines.runBlocking
import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

private const val connectionString = "couchbase://localhost"
private const val username = "Administrator"
private const val password = "password"
private const val bucketName = "travel-sample"

fun main() = runBlocking {
    collectingInformationAndLogging1()
    collectingInformationAndLogging4()
    connection1()
    collectingInformationAndLogging3()
    println("Done.")
}

private suspend fun connection1() {
    // tag::connection_1[]
    val cluster = Cluster.connect(connectionString, username, password)
    val bucket = cluster.bucket(bucketName)
    val scope = bucket.scope("inventory")
    val collection = scope.collection("airport")
    // end::connection_1[]
    cluster.disconnect()
}

private fun collectingInformationAndLogging1() {
    // tag::collecting_information_and_logging_1[]
    val logger = Logger.getLogger("com.couchbase.client")
    logger.level = Level.FINE
    for (handler in logger.parent.handlers) {
        if (handler is ConsoleHandler) {
            handler.level = Level.FINE
        }
    }
    // end::collecting_information_and_logging_1[]
}

private suspend fun collectingInformationAndLogging3() {
    // tag::collecting_information_and_logging_3[]
    val environment = ClusterEnvironment.builder().build() // <1>

    environment.eventBus().subscribe { event ->
        // handle events as they arrive
        if (event.severity() == Event.Severity.INFO || event.severity() == Event.Severity.WARN) {
            println(event)
        }
    }

    val cluster = Cluster.connectUsingSharedEnvironment( // <2>
        connectionString,
        username,
        password,
        environment,
    )

    val bucket = cluster.bucket(bucketName)
    // end::collecting_information_and_logging_3[]

    // tag::shutdown[]
    cluster.disconnect()
    environment.shutdownSuspend() // <1>
    // end::shutdown[]
}

private fun collectingInformationAndLogging4() {
    // tag::collecting_information_and_logging_4[]
    LogRedaction.setRedactionLevel(RedactionLevel.FULL)
    // end::collecting_information_and_logging_4[]
}
