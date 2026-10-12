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

import com.couchbase.client.core.diagnostics.ClusterState
import com.couchbase.client.core.service.ServiceType
import com.couchbase.client.kotlin.Bucket
import com.couchbase.client.kotlin.Cluster
import com.couchbase.client.kotlin.diagnostics.DiagnosticsResult
import com.couchbase.client.kotlin.diagnostics.PingResult
import kotlinx.coroutines.runBlocking

fun main() {
    val cluster = Cluster.connect("couchbase://127.0.0.1", "Administrator", "password")

    val bucket = cluster.bucket("travel-sample")
    val scope = bucket.scope("inventory")
    val collection = scope.collection("hotel")

    runBlocking {
        try {
            pingBasic(cluster)
            pingJsonExport(cluster)
            pingOptions(cluster)
            pingBucket(bucket)
            diagnosticsBasic(cluster)
            diagnosticsOptions(cluster)
            diagnosticsState(cluster)
        } finally {
            cluster.disconnect()
        }
    }
}

private suspend fun pingBasic(cluster: Cluster) {
    // tag::ping-basic[]
    val pingResult: PingResult = cluster.ping()
    for ((serviceType, reports) in pingResult.endpoints) {
        for (report in reports) {
            System.err.println("$serviceType: ${report.remote} took ${report.latency}")
        }
    }
    // end::ping-basic[]
}

private suspend fun pingJsonExport(cluster: Cluster) {
    // tag::ping-json-export[]
    val pingResult: PingResult = cluster.ping()
    println(pingResult.exportToJson())
    // end::ping-json-export[]
}

private suspend fun pingOptions(cluster: Cluster) {
    // tag::ping-options[]
    val pingResult: PingResult = cluster.ping(services = setOf(ServiceType.QUERY))
    println(pingResult.exportToJson())
    // end::ping-options[]
}

private suspend fun pingBucket(bucket: Bucket) {
    // tag::ping-bucket[]
    val pingResult: PingResult = bucket.ping(services = setOf(ServiceType.KV))
    println(pingResult.exportToJson())
    // end::ping-bucket[]
}

private fun diagnosticsBasic(cluster: Cluster) {
    // tag::diagnostics-basic[]
    val diagnosticsResult: DiagnosticsResult = cluster.diagnostics()
    for ((serviceType, endpoints) in diagnosticsResult.endpoints) {
        for (endpoint in endpoints) {
            System.err.println(
                "$serviceType: ${endpoint.remote} last activity ${endpoint.lastActivity}"
            )
        }
    }
    // end::diagnostics-basic[]
}

private fun diagnosticsOptions(cluster: Cluster) {
    // tag::diagnostics-options[]
    val diagnosticsResult: DiagnosticsResult = cluster.diagnostics()
    println(diagnosticsResult.exportToJson())
    // end::diagnostics-options[]
}

private fun diagnosticsState(cluster: Cluster) {
    // tag::diagnostics-state[]
    when (cluster.diagnostics().state) {
        ClusterState.ONLINE -> println("All endpoints are connected.")
        ClusterState.DEGRADED -> println("Some endpoints are not connected.")
        ClusterState.OFFLINE -> println("No endpoints are connected.")
    }
    // end::diagnostics-state[]
}
