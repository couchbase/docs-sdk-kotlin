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

@file:Suppress("UNUSED_VARIABLE", "unused")

import com.couchbase.client.core.env.IoConfig
import com.couchbase.client.core.service.ServiceType
import com.couchbase.client.kotlin.Cluster
import com.couchbase.client.kotlin.env.ClusterEnvironment
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

private const val username = "Administrator"
private const val password = "password"
private const val connectionString = "127.0.0.1"

fun connectLambda() {
// tag::lambda[]
    val cluster = Cluster.connect(connectionString, username, password) {
        io {
            enableDnsSrv = false
        }
    }
// end::lambda[]
}

fun connectLambdaMultiple() {
// tag::lambdaMultiple[]
    val cluster = Cluster.connect(connectionString, username, password) {
        io {
            enableDnsSrv = false

            kvCircuitBreaker {
                enabled = true
            }
        }
    }
// end::lambdaMultiple[]
}

fun connectBuilder() {
// tag::builder[]
    val envBuilder = ClusterEnvironment.builder()
        .ioConfig(IoConfig.enableDnsSrv(false))

    val cluster = Cluster.connect(connectionString, username, password, envBuilder)
// end::builder[]
}
