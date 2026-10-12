import com.couchbase.client.core.error.CouchbaseException
import com.couchbase.client.kotlin.Cluster
import com.couchbase.client.kotlin.query.execute
import kotlinx.coroutines.runBlocking

// tag::class[]
// Update these variables to point to your Couchbase instance and credentials.
const val connectionString = "couchbase://localhost"
const val username = "Administrator"
const val password = "password"

fun main() = runBlocking {
    val cluster = Cluster.connect(connectionString, username, password)

    try {
        val result = cluster
            .query("select * from `travel-sample`.inventory.airline limit 100", metrics = true)
            .execute()

        for (row in result.rows) {
            println("Found row: " + row.content.decodeToString())
        }

        println("Reported execution time: " + result.metadata.metrics?.executionTime)
    } catch (ex: CouchbaseException) {
        ex.printStackTrace()
    } finally {
        cluster.disconnect()
    }
}
// end::class[]
