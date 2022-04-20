import com.couchbase.client.kotlin.Cluster
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

fun main() {
    val cluster = Cluster.connect(
        connectionString = "couchbase://127.0.0.1", // <1>
        username = "username",
        password = "password",
    )

    runBlocking { // <2>
        try {
            cluster.waitUntilReady(10.seconds) // <3>
            // ...
        } finally {
            cluster.disconnect()
        }
    }
}
