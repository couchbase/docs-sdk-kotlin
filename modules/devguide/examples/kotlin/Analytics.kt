import com.couchbase.client.core.error.CouchbaseException
import com.couchbase.client.kotlin.Cluster
import com.couchbase.client.kotlin.analytics.AnalyticsParameters
import com.couchbase.client.kotlin.analytics.AnalyticsPriority
import com.couchbase.client.kotlin.analytics.AnalyticsScanConsistency
import com.couchbase.client.kotlin.analytics.execute
import kotlinx.coroutines.runBlocking
import java.util.UUID

// Update these variables to point to your Couchbase instance and credentials.
const val connectionString = "couchbase://localhost"
const val username = "Administrator"
const val password = "password"

fun main() = runBlocking {
    val cluster = Cluster.connect(connectionString, username, password)
    try {
        simpleQuery(cluster)
        namedParameters(cluster)
        positionalParameters(cluster)
        analyticsResult(cluster)
        metadata(cluster)
        scanConsistency(cluster)
        clientContextId(cluster)
        priority(cluster)
        readonly(cluster)
        streaming(cluster)
        collectionQuery(cluster)
        scopeQuery(cluster)
    } finally {
        cluster.disconnect()
    }
}

// Getting Started: a complete example of an analytics query and its results.
suspend fun simpleQuery(cluster: Cluster) {
    try {
        val result = cluster
            .analyticsQuery("select \"hello\" as greeting")
            .execute()

        for (row in result.rows) {
            println("Found row: " + row.content.decodeToString())
        }

        println("Reported execution time: " + result.metadata.metrics.executionTime)
        println()
    } catch (ex: CouchbaseException) {
        ex.printStackTrace()
    }
}

// Parameterized queries: by name...
suspend fun namedParameters(cluster: Cluster) {
    val result = cluster.analyticsQuery(
        "select count(*) from airport where country = \$country",
        parameters = AnalyticsParameters.named("country" to "France"),
    ).execute()
}

// ...and by position.
suspend fun positionalParameters(cluster: Cluster) {
    val result = cluster.analyticsQuery(
        "select count(*) from airport where country = ?",
        parameters = AnalyticsParameters.positional(listOf("France")),
    ).execute()
}

// The Analytics Result: rows can be read as raw JSON, a Map, or your own data class.
data class AirportRow(val airport: Map<String, Any?>)

suspend fun analyticsResult(cluster: Cluster) {
    val result = cluster.analyticsQuery("select * from airport limit 3").execute()
    for (row in result.rows) {
        println("Found row: " + row.content.decodeToString())
        // Alternatives:
        // val asMap = row.contentAs<Map<String, Any?>>()
        // val asObject = row.contentAs<AirportRow>()
    }
}

// Metadata: requestId, clientContextId, status, signature, metrics, warnings.
suspend fun metadata(cluster: Cluster) {
    val result = cluster.analyticsQuery("select 1=1").execute()
    System.err.println("Execution time: " + result.metadata.metrics.executionTime)
}

// Options: scan consistency.
suspend fun scanConsistency(cluster: Cluster) {
    val result = cluster.analyticsQuery(
        "select count(*) from airport where country = 'France'",
        consistency = AnalyticsScanConsistency.requestPlus(),
    ).execute()
}

// Options: client context ID.
suspend fun clientContextId(cluster: Cluster) {
    val result = cluster.analyticsQuery(
        "select count(*) from airport where country = 'France'",
        clientContextId = "user-44" + UUID.randomUUID(),
    ).execute()
}

// Options: priority.
suspend fun priority(cluster: Cluster) {
    val result = cluster.analyticsQuery(
        "select count(*) from airport where country = 'France'",
        priority = AnalyticsPriority.high(),
    ).execute()
}

// Options: readonly.
suspend fun readonly(cluster: Cluster) {
    val result = cluster.analyticsQuery(
        "select count(*) from airport where country = 'France'",
        readonly = true,
    ).execute()
}

// Streaming: the Kotlin equivalent of the reactive example.
// Rows are processed as they arrive instead of being buffered in memory.
suspend fun streaming(cluster: Cluster) {
    val metadata = cluster
        .analyticsQuery("select 1=1")
        .execute { row -> println("Found row: " + row.content.decodeToString()) }
}

// Scoped queries on named collections: fully qualified keyspace...
suspend fun collectionQuery(cluster: Cluster) {
    val result = cluster.analyticsQuery(
        "SELECT airportname, country FROM `travel-sample`.inventory.airport WHERE country='France' LIMIT 3"
    ).execute()
}

// ...or querying from the Scope object.
suspend fun scopeQuery(cluster: Cluster) {
    val bucket = cluster.bucket("travel-sample")
    val scope = bucket.scope("inventory")
    val result = scope.analyticsQuery(
        "SELECT airportname, country FROM `airport` WHERE country='France' LIMIT 4"
    ).execute()
}
