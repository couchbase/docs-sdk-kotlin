import com.couchbase.client.core.error.CouchbaseException
import com.couchbase.client.kotlin.Cluster
import com.couchbase.client.kotlin.query.execute
import kotlinx.coroutines.runBlocking

// tag::class[]
// Update these variables to point to your Couchbase Capella instance and credentials.
private const val CONNECTION_STRING = "couchbases://cb.<your-endpoint-here>.cloud.couchbase.com"
private const val USERNAME = "Administrator"
private const val PASSWORD = "password"

fun main() = runBlocking<Unit> {
    val cluster = Cluster.connect(CONNECTION_STRING, USERNAME, PASSWORD) {
        // Use the "wan-development" profile, because the application
        // is probably not running in the same network as the cluster.
        applyProfile("wan-development")
    }

    try {
        // cluster.query() returns a Flow. The execute() function collects
        // the Flow into a QueryResult, which is fine for a result this small.
        val result = cluster
            .query(
                "select * from `travel-sample`.inventory.airline limit 100",
                metrics = true,
            )
            .execute()

        for (row in result.rows) {
            println("Found row: ${row.contentAs<Map<String, Any?>>()}")
        }

        println("Reported execution time: ${result.metadata.metrics?.executionTime}")
    } catch (ex: CouchbaseException) {
        ex.printStackTrace()
    } finally {
        cluster.disconnect()
    }
}
// end::class[]
