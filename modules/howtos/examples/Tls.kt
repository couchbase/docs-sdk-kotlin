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

@file:Suppress("unused", "UNUSED_VARIABLE")

import com.couchbase.client.core.deps.io.netty.handler.ssl.util.InsecureTrustManagerFactory
import com.couchbase.client.core.env.SecurityConfig
import com.couchbase.client.kotlin.Cluster
import com.couchbase.client.kotlin.env.dsl.TrustSource
import java.nio.file.Paths
import java.security.cert.X509Certificate

private const val username = "Administrator"
private const val password = "password"
private const val connectionString = "127.0.0.1"

fun secureConnectionCertificate() {
// tag::certificate[]
    val cluster = Cluster.connect(connectionString, username, password) {
        security {
            enableTls = true
            trust = TrustSource.certificate( // <1>
                Paths.get("/path/to/ca.pem") // <2>
            )
        }
    }
// end::certificate[]
}

fun secureConnectionKeyStore() {
// tag::trustStoreFile[]
    val cluster = Cluster.connect(connectionString, username, password) {
        security {
            enableTls = true
            trust = TrustSource.trustStore(
                path = Paths.get("/path/to/trust-store.p12"),
                password = "password",
            )
        }
    }
// end::trustStoreFile[]
}

fun secureConnectionParseYourOwn() {
// tag::trustStoreParseYourOwn[]
    val pemEncodedCertificates = """
        -----BEGIN CERTIFICATE-----
        MIIDAjCCAeqgAwIBAgIIFpZtHpcc9cgwDQYJKoZIhvcNAQELBQAwJDEiMCAGA1UE
        ...
        xFptQ/XVtEO/zh0gqSnUD/dROeUG28zbDKdP4Q1b70XE87HKnjYDcpfwfyJwo0Xg
        -----END CERTIFICATE-----
    """

    val decodedCertificates: List<X509Certificate> =
        SecurityConfig.decodeCertificates(listOf(pemEncodedCertificates))

    val cluster = Cluster.connect(connectionString, username, password) {
        security {
            enableTls = true
            trust = TrustSource.certificates(decodedCertificates)
        }
    }
// end::trustStoreParseYourOwn[]
}

fun secureConnectionTrustFactory() {
// tag::trustStoreFactory[]
    val cluster = Cluster.connect(connectionString, username, password) {
        security {
            enableTls = true
            trust = TrustSource.factory(InsecureTrustManagerFactory.INSTANCE) // <1>
        }
    }
// end::trustStoreFactory[]
}
