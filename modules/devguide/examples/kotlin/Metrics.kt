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
import com.couchbase.client.metrics.opentelemetry.OpenTelemetryMeter
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.metrics.Aggregation
import io.opentelemetry.sdk.metrics.InstrumentSelector
import io.opentelemetry.sdk.metrics.InstrumentType
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.View
import io.opentelemetry.sdk.metrics.export.MetricExporter
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader
import io.opentelemetry.sdk.resources.Resource
import kotlinx.coroutines.runBlocking
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

fun main() = runBlocking<Unit> {
    run {
        // tag::metrics-enable-custom[]
        val cluster = Cluster.connect("localhost", "Administrator", "password") {
            loggingMeter {
                enabled = true
                emitInterval = 30.seconds
            }
        }
        // end::metrics-enable-custom[]
        cluster.disconnect()
    }

    run {
        val hostname = "localhost"
        val username = "Administrator"
        val password = "password"

        // tag::metrics-otel-prometheus[]
        // Setup an exporter.
        // This exporter exports metrics on the OTLP protocol over GRPC to localhost:4317.
        val exporter: MetricExporter = OtlpGrpcMetricExporter.builder()
            .setCompression("gzip")
            .setEndpoint("http://localhost:4317")
            .build()

        // Create the OpenTelemetry SDK's SdkMeterProvider.
        val sdkMeterProvider: SdkMeterProvider = SdkMeterProvider.builder()
            .setResource(
                Resource.getDefault()
                    .merge(
                        Resource.builder()
                            // An OpenTelemetry service name generally reflects the name of your microservice,
                            // e.g. "shopping-cart-service".
                            .put("service.name", "YOUR_SERVICE_NAME_HERE")
                            .build()
                    )
            )
            // Operation durations are in nanoseconds, which are too large for the default OpenTelemetry histogram buckets.
            .registerView(
                InstrumentSelector.builder().setType(InstrumentType.HISTOGRAM).build(),
                View.builder()
                    .setAggregation(
                        Aggregation.explicitBucketHistogram(
                            listOf(
                                100000.0,
                                250000.0,
                                500000.0,
                                1000000.0,
                                10000000.0,
                                100000000.0,
                                1000000000.0,
                                10000000000.0,
                            )
                        )
                    )
                    .build()
            )
            .registerMetricReader(PeriodicMetricReader.builder(exporter).setInterval(Duration.ofSeconds(1)).build())
            .build()

        // Create the OpenTelemetry SDK's OpenTelemetry object.
        val openTelemetry: OpenTelemetry = OpenTelemetrySdk.builder()
            .setMeterProvider(sdkMeterProvider)
            .buildAndRegisterGlobal()

        // Provide the OpenTelemetry object as part of the Cluster configuration.
        val cluster = Cluster.connect(hostname, username, password) {
            meter = OpenTelemetryMeter.wrap(openTelemetry)
        }
        // end::metrics-otel-prometheus[]

        // A basic test to check the metrics are exporting.
        cluster.bucket("default").defaultCollection().upsert("doc", emptyMap<String, Any?>())

        cluster.disconnect()
    }
}
