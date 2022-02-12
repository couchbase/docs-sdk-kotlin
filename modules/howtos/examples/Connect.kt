import com.couchbase.client.kotlin.Cluster
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

fun main() {
    val username = "Administrator"
    val password = "password"
    val connectionString = "127.0.0.1"

    val cluster = Cluster.connect(connectionString, username, password)

    runBlocking { // <1>
        try {
            cluster.waitUntilReady(10.seconds) // <2>
            // ...
        } finally {
            cluster.disconnect()
        }
    }
}
