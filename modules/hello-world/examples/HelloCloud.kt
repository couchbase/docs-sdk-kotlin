import com.couchbase.client.kotlin.Cluster
import com.couchbase.client.kotlin.query.execute
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

fun main() {
    // Replace with your cluster address.
    val address = "--your-cluster--.cloud.couchbase.com"

    val cluster = Cluster.connect(
        connectionString = "couchbases://$address", // <1>
        username = "username", // Replace with credentials
        password = "password", // of a database user account.
    )

    try {
        runBlocking {
            val collection = cluster
                .bucket("travel-sample")
                .waitUntilReady(10.seconds)
                .defaultCollection()

            // Execute a N1QL query
            val queryResult = cluster
                .query("select * from `travel-sample` limit 3")
                .execute()
            queryResult.rows.forEach { println(it) }
            println(queryResult.metadata)

            // Get a document from the K/V service
            val getResult = collection.get("airline_10")
            println(getResult)
            println(getResult.contentAs<Map<String, Any?>>())
        }
    } finally {
        runBlocking { cluster.disconnect() }
    }
}
