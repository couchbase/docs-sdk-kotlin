import com.couchbase.client.kotlin.Cluster
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

fun main() {
    val cluster = Cluster.connect(
        connectionString = "127.0.0.1",
        username = "Administrator",
        password = "password",
    )

    runBlocking { // <1>
        try {
            cluster.waitUntilReady(10.seconds) // <2>
            // ...
        } finally {
            cluster.disconnect()
        }
    }
}
