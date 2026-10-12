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

import com.couchbase.client.core.error.TimeoutException
import com.couchbase.client.kotlin.Cluster
import com.couchbase.client.metrics.micrometer.MicrometerMeter
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.config.MeterFilter
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.exporter.HTTPServer
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress

fun main() = runBlocking<Unit> {
    val hostname = "localhost"
    val username = "Administrator"
    val password = "password"

    // tag::metrics-micrometer-prometheus[]
    val prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    // Micrometer won't create a histogram by default, configure that.
    prometheusRegistry.config().meterFilter(
        object : MeterFilter {
            override fun configure(id: Meter.Id, config: DistributionStatisticConfig): DistributionStatisticConfig {
                if (id.type == Meter.Type.DISTRIBUTION_SUMMARY) {
                    return DistributionStatisticConfig.builder()
                        .percentilesHistogram(true)
                        .build()
                        .merge(config)
                }
                return config
            }
        }
    )

    // Setup an HTTP server running on port 10000 that Prometheus can scrape for Micrometer metrics.
    HTTPServer(InetSocketAddress(10000), prometheusRegistry.prometheusRegistry, true)

    // Provide the Micrometer registry as part of the Cluster configuration.
    val cluster = Cluster.connect(hostname, username, password) {
        meter = MicrometerMeter.wrap(prometheusRegistry)
    }
    // end::metrics-micrometer-prometheus[]

    val collection = cluster.bucket("default").defaultCollection()
    while (true) {
        try {
            // A basic test to check the metrics are exporting.
            collection.upsert("doc", emptyMap<String, Any?>())
        } catch (err: TimeoutException) {
        }
    }
}
