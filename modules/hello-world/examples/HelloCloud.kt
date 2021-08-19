import com.couchbase.client.core.deps.io.netty.handler.ssl.util.InsecureTrustManagerFactory
import com.couchbase.client.core.env.SecurityConfig
import com.couchbase.client.kotlin.Cluster
import com.couchbase.client.kotlin.env.dsl.TrustSource
import kotlinx.coroutines.runBlocking
import java.time.Duration

public fun main() {
    // Update these variables to point to your Cloud instance and credentials.
    val endpoint = "--your-instance--.dp.cloud.couchbase.com"
    val username = "username"
    val password = "password"
    val bucketName = "bucket"

    val encodedTlsCertificate =
"""
-----BEGIN CERTIFICATE-----
... your certificate content in here ...
-----END CERTIFICATE-----
"""
    val tlsCertificates = SecurityConfig.decodeCertificates(
        listOf(encodedTlsCertificate)
    );

    // Connect and open a bucket
    val cluster = Cluster.connect(endpoint, username, password) {
        security {
            enableTls = true

            // See TrustSource for alternate ways to load certificates
            // (from filesystem, from KeyStore, etc.)
            trust = TrustSource.certificates(tlsCertificates)

            // During development, if you want to trust all certificates then
            // use InsecureTrustManagerFactory as the trust source.
            // As the name points out, this is INSECURE!
            // trust = TrustSource.factory(InsecureTrustManagerFactory.INSTANCE)
        }
    }

    try {
        val bucket = cluster.bucket(bucketName)
        val collection = bucket.defaultCollection()

        runBlocking {
            bucket.waitUntilReady(Duration.ofSeconds(10))

            // Create a JSON Document
            val arthur = mapOf(
                "name" to "Arthur",
                "email" to "kingarthur@couchbase.com",
                "interests" to listOf("Holy Grail", "African Swallows")
            )

            // Store the Document
            collection.upsert("u:king_arthur", arthur)

            // Load the document and print it (content and metadata)
            println(collection.get("u:king_arthur"))
        }
    } finally {
        runBlocking { cluster.disconnect() }
    }
}
