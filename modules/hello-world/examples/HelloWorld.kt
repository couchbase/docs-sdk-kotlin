import com.couchbase.client.kotlin.Cluster
import com.couchbase.client.kotlin.query.execute
import kotlinx.coroutines.runBlocking

public fun main() {
    // Assumes you have Couchbase running locally
    // and the "travel-sample" sample bucket loaded.

    // Connect and open a bucket
    val cluster = Cluster.connect("127.0.0.1", "Administrator", "password")
    try {
        val bucket = cluster.bucket("travel-sample")
        val collection = bucket.defaultCollection()

        runBlocking {
            // Perform a N1QL query and buffer the results
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
