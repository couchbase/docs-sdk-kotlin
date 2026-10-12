import com.couchbase.client.kotlin.Bucket
import com.couchbase.client.kotlin.Cluster
import com.couchbase.client.kotlin.Collection
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

// tag::managing_connections_9[]
fun main() = runBlocking {
    val cluster = Cluster.connect("couchbase://127.0.0.1", "Administrator", "password")
    try {
        cluster.waitUntilReady(10.seconds)
        val bucket: Bucket = cluster.bucket("travel-sample")
        val collection: Collection = bucket.defaultCollection()
    } finally {
        cluster.disconnect()
    }
}
// end::managing_connections_9[]
