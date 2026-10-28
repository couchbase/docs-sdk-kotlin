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

import com.couchbase.client.core.env.Authenticator
import com.couchbase.client.core.env.CertificateAuthenticator
import com.couchbase.client.core.env.PasswordAuthenticator
import com.couchbase.client.kotlin.Cluster
import com.couchbase.client.kotlin.env.dsl.TrustSource
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths
import java.security.KeyStore
import kotlin.time.Duration.Companion.seconds

fun main() = runBlocking<Unit> {

    run {
        // tag::rbac-simple[]
        val cluster = Cluster.connect("127.0.0.1", "Administrator", "password")
        // end::rbac-simple[]
        cluster.disconnect()
    }

    run {
        // tag::rbac-env[]
        // Credentials plus custom client settings. The trailing lambda
        // configures the cluster environment.
        val cluster = Cluster.connect("127.0.0.1", "Administrator", "password") {
            timeout {
                kvTimeout = 5.seconds
            }
        }
        // end::rbac-env[]
        cluster.disconnect()
    }

    run {
        // tag::rbac-pwd[]
        val authenticator = PasswordAuthenticator.builder()
            .username("Administrator")
            .password("password")
            // enables only the PLAIN authentication mechanism, used with LDAP
            .onlyEnablePlainSaslMechanism()
            .build()

        val cluster = Cluster.connect("127.0.0.1", authenticator)
        // end::rbac-pwd[]
        cluster.disconnect()
    }

    run {
        try {
            // tag::certauth[]
            // Replace the following line with code that gets your actual key store.
            // The key store contains the client's certificate and private key.
            val keyStore: KeyStore = loadKeyStore()

            val authenticator: Authenticator = CertificateAuthenticator.fromKeyStore(
                keyStore,
                "keyStorePassword",
            )

            val cluster = Cluster.connect(
                "couchbases://127.0.0.1",
                authenticator,
            ) {
                security {
                    // Tell the client to trust the cluster's root certificate.
                    // If your cluster's root certificate is from a well-known
                    // Certificate Authority (CA), you can skip this.
                    trust = TrustSource.certificate(Paths.get("/path/to/ca-cert.pem"))
                }
            }
            // end::certauth[]
            cluster.disconnect()
        } catch (e: Exception) {
            // The code requires a valid keystore, catching the exception for
            // example purposes only.
        }
    }
}

private fun loadKeyStore(): KeyStore {
    // Stub for example purposes: returns an empty key store.
    return KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null, null) }
}