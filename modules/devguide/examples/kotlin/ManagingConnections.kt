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

import com.couchbase.client.kotlin.Bucket
import com.couchbase.client.kotlin.Cluster
import com.couchbase.client.kotlin.Collection
import com.couchbase.client.kotlin.Scope
import com.couchbase.client.kotlin.codec.JsonSerializer
import com.couchbase.client.kotlin.codec.TypeRef
import com.couchbase.client.kotlin.env.ClusterEnvironment
import com.couchbase.client.kotlin.env.dsl.TrustSource
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths
import kotlin.time.Duration.Companion.seconds

private class MyCustomJsonSerializer : JsonSerializer {
    override fun <T> serialize(value: T, type: TypeRef<T>): ByteArray {
        throw UnsupportedOperationException("just an example")
    }

    override fun <T> deserialize(json: ByteArray, type: TypeRef<T>): T {
        throw UnsupportedOperationException("just an example")
    }
}

fun main() = runBlocking {

    run {
        // tag::simpleconnect[]
        val cluster: Cluster = Cluster.connect("127.0.0.1", "username", "password")
        val bucket: Bucket = cluster.bucket("travel-sample")
        val collection: Collection = bucket.defaultCollection()

        // You can access multiple buckets using the same Cluster object.
        val anotherBucket: Bucket = cluster.bucket("beer-sample")

        // You can access collections other than the default
        // if your version of Couchbase Server supports this feature.
        val customerA: Scope = bucket.scope("customer-a")
        val widgets: Collection = customerA.collection("widgets")

        // For a graceful shutdown, disconnect from the cluster when the program ends.
        cluster.disconnect()
        // end::simpleconnect[]
    }

    run {
        // tag::multinodeconnect[]
        val cluster = Cluster.connect("192.168.56.101,192.168.56.102", "username", "password")
        // end::multinodeconnect[]
        cluster.disconnect()
    }

    run {
        // tag::customenv[]
        // Connect to a cluster using custom client settings.
        // The trailing lambda configures the Cluster's ClusterEnvironment
        // using the environment config DSL. No need to call build() yourself,
        // the SDK takes care of that.
        val cluster = Cluster.connect(
            connectionString = "127.0.0.1",
            username = "username",
            password = "password",
        ) {
            // For example, set the default JSON serializer.
            jsonSerializer = MyCustomJsonSerializer()

            // For example, set the default SQL++ query timeout to 30 seconds.
            timeout {
                queryTimeout = 30.seconds
            }
        }

        // Shut down gracefully.
        cluster.disconnect()
        // end::customenv[]
    }

    run {
        // tag::shareclusterenvironment[]
        val sharedEnvironment: ClusterEnvironment = ClusterEnvironment.builder {
            timeout {
                kvTimeout = 5.seconds
            }
        }.build()

        val clusterA = Cluster.connectUsingSharedEnvironment(
            connectionString = "clusterA.example.com",
            username = "username",
            password = "password",
            env = sharedEnvironment,
        )
        val clusterB = Cluster.connectUsingSharedEnvironment(
            connectionString = "clusterB.example.com",
            username = "username",
            password = "password",
            env = sharedEnvironment,
        )

        // ...

        // For a graceful shutdown, disconnect from the clusters
        // AND shut down the custom environment when the program ends.
        clusterA.disconnect()
        clusterB.disconnect()
        sharedEnvironment.shutdownSuspend()
        // end::shareclusterenvironment[]
    }

    run {
        // tag::connectionstringparams[]
        val cluster = Cluster.connect(
            "127.0.0.1?io.maxHttpConnections=23&io.networkResolution=external",
            "username",
            "password",
        )
        // end::connectionstringparams[]
        cluster.disconnect()
    }

    // tag::blockingtoasync[]
    // Unlike the Java SDK, the Kotlin SDK doesn't have separate Async/Reactive
    // Cluster classes, or bucket.async() / bucket.reactive() views. There's a
    // single API surface: methods are Kotlin coroutine `suspend` functions
    // (called from a coroutine, e.g. inside runBlocking or launch), and
    // streaming results (like query rows) are exposed as a kotlinx.coroutines
    // Flow instead of a Reactor Flux/Mono. So there's no Kotlin equivalent of
    // this Java example -- one Cluster type covers all three styles.
    // end::blockingtoasync[]

    // tag::reactivecluster[]
    // No ReactiveCluster equivalent -- see the note above. Where Java would
    // use ReactiveCluster.query(...) returning a Flux, the Kotlin SDK's
    // cluster.query(...) returns a Flow you collect from a coroutine.
    // end::reactivecluster[]

    // tag::asynccluster[]
    // No AsyncCluster equivalent -- see the note above. Where Java would use
    // AsyncCluster.connect(...) returning a CompletableFuture<Cluster>, the
    // Kotlin SDK's Cluster.connect(...) returns a Cluster directly (it
    // connects in the background), and disconnect() is itself a suspend
    // function you call from a coroutine, rather than something you join()/
    // block() on.
    // end::asynccluster[]

    run {
        // tag::tls[]
        val cluster = Cluster.connect(
            connectionString = "couchbases://<your-endpoint-or-ip-address>",
            username = "username",
            password = "password",
        ) {
            security {
                trust = TrustSource.certificate(Paths.get("/path/to/cluster.cert"))
            }
        }
        // end::tls[]
        cluster.disconnect()
    }

    run {
        // tag::dnssrv[]
        val env = ClusterEnvironment.builder {
            io {
                enableDnsSrv = true
            }
        }.build()
        // end::dnssrv[]
        env.shutdownSuspend()
    }
}