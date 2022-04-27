/*
 * Copyright 2022 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.couchbase.client.kotlin.Cluster
import com.couchbase.client.kotlin.http.CouchbaseHttpResponse
import com.couchbase.client.kotlin.http.HttpBody
import com.couchbase.client.kotlin.http.HttpTarget
import com.couchbase.client.kotlin.http.formatPath
import com.couchbase.client.kotlin.search.*
import kotlinx.coroutines.runBlocking

private suspend fun simpleQuery(cluster: Cluster) {
// tag::simpleQuery[]
    val searchResult: SearchResult = cluster
        .searchQuery(
            indexName = "travel-sample-index",
            query = SearchQuery.queryString("pool"), // <1>
        )
        .execute()

    searchResult.rows.forEach { row ->
        println("Found row: $row")
    }
// end::simpleQuery[]
}

private suspend fun poolOrSauna(cluster: Cluster) {
// tag::poolOrSauna[]
    val saunaOrPool: SearchQuery = SearchQuery.disjunction( // <1>
        SearchQuery.match("sauna") boost 1.5, // <2>
        SearchQuery.match("pool"),
    )
    val searchResult: SearchResult = cluster
        .searchQuery(
            indexName = "travel-sample-index",
            query = saunaOrPool,
        )
        .execute()
// end::poolOrSauna[]
}


private suspend fun totalRows(cluster: Cluster) {
// tag::totalRows[]
    val searchResult: SearchResult = cluster
        .searchQuery(
            indexName = "travel-sample-index",
            query = SearchQuery.queryString("pool"),
            limit = 10,
        )
        .execute()

    val total = searchResult.metadata.metrics.totalRows // <1>
    println("Total matching rows: $total")
// end::totalRows[]
}


private suspend fun streaming(cluster: Cluster) {
// tag::streaming[]
    val searchMetadata: SearchMetadata = cluster
        .searchQuery(
            indexName = "travel-sample-index",
            query = SearchQuery.queryString("pool"),
        )
        .execute { row ->
            println("Found row: $row")
        }
// end::streaming[]

}


private suspend fun disableScoring(cluster: Cluster) {
// tag::disableScoring[]
    val searchResult: SearchResult = cluster
        .searchQuery(
            indexName = "travel-sample-index",
            query = SearchQuery.queryString("pool"),
            score = Score.none(), // <1>
        )
        .execute()
// end::disableScoring[]
}

private suspend fun sortByCountry(cluster: Cluster) {
// tag::sortByCountry[]
    val searchResult: SearchResult = cluster
        .searchQuery(
            indexName = "travel-sample-index",
            query = SearchQuery.queryString("pool"),
            sort = SearchSort.byField("country"), // <1>
        )
        .execute()
// end::sortByCountry[]
}

private suspend fun sortWithStrings() {
// tag::sortWithStrings[]
    val sort: SearchSort = SearchSort.by(
        "country", "state", "city", "-_score"
    )
// end::sortWithStrings[]
}

private suspend fun multiSortThen(cluster: Cluster) {
// tag::multiSortThen[]
    val multiLevelSort: SearchSort =
        SearchSort.byField("country") then SearchSort.byId()
// end::multiSortThen[]
}

private suspend fun multiSortList(cluster: Cluster) {
// tag::multiSortList[]
    val multiLevelSort: SearchSort = SearchSort.of(listOf(
        SearchSort.byField("country"),
        SearchSort.byId(),
    ))
// end::multiSortList[]
}


private suspend fun offsetPagination(cluster: Cluster) {
// tag::offsetPagination[]
    val searchResult: SearchResult = cluster
        .searchQuery(
            indexName = "travel-sample-index",
            query = SearchQuery.queryString("pool"),
            page = SearchPage.startAt(offset = 10), // <1>
            limit = 10,
        )
        .execute()
// end::offsetPagination[]
}

private suspend fun keysetPagination(cluster: Cluster) {
// tag::keysetPagination[]
    val indexName = "travel-sample-index"
    val query = SearchQuery.queryString("pool")
    val sort = SearchSort.byId()
    val pageSize = 10

    val firstPage: SearchResult = cluster
        .searchQuery(
            indexName = indexName,
            query = query,
            sort = sort,
            limit = pageSize,
            page = SearchPage.startAt(offset = 0), // <1>
        )
        .execute()

    check(firstPage.rows.isNotEmpty()) { "Oops, no results!" }
    val lastRowOfFirstPage: SearchRow = firstPage.rows.last()

    val nextPage: SearchResult = cluster
        .searchQuery(
            indexName = indexName,
            query = query,
            sort = sort,
            limit = pageSize,
            page = SearchPage.searchAfter( // <2>
                lastRowOfFirstPage.keyset
            ),
        )
        .execute()

// end::keysetPagination[]
}

internal suspend fun searchQueryWithFacets(cluster: Cluster) {
    // tag::searchQueryWithFacets[]
    // Count results that fall into these "alcohol by volume" ranges.
    // Optionally assign names to the ranges.
    val low = NumericRange.bounds(min = 0, max = 3.5, name = "low")
    val high = NumericRange.lowerBound(3.5, name = "high")
    val abv = SearchFacet.numeric(
        field = "abv",
        ranges = listOf(low, high),
        name = "Alcohol by volume",
    )

    // Find the 5 most frequent values in the "category" field.
    val beerType = SearchFacet.term("category", size = 5)

    val result = cluster.searchQuery(
        indexName = "beer-sample-index",
        query = SearchQuery.matchAll(),
        facets = listOf(abv, beerType),
    ).execute()

    // Print all facet results. Results do not include empty facets
    // or ranges. Categories are ordered by size, descending.
    result.facets.forEach { facet ->
        println(facet.name)
        facet.categories.forEach { println("  $it") }
        facet.other.let { if (it > 0) println("  <other> ($it)") }
        println()
    }

    // Alternatively, print results for a specific facet:
    val abvResult = result[abv]
    if (abvResult == null) {
        println("No search results matched any of the 'abv' facet ranges.")
    } else {
        println("Alcohol by volume (again)")
        println(" low (${abvResult[low]?.count ?: 0})")
        println(" high (${abvResult[high]?.count ?: 0})")
        println()
    }
    // end::searchQueryWithFacets[]
}


private suspend fun installIndex(cluster: Cluster) {
    val indexName = "demoIndex"
    val index = """
        {
          "name": "$indexName",
          "type": "fulltext-index",
          "params": {
            "mapping": {
              "types": {
                "inventory.airline": {
                  "enabled": true,
                  "dynamic": true
                }
              },
              "default_mapping": {
                "enabled": false,
                "dynamic": true
              },
              "default_type": "_default",
              "default_analyzer": "standard",
              "default_datetime_parser": "dateTimeOptional",
              "default_field": "_all",
              "store_dynamic": false,
              "index_dynamic": true,
              "docvalues_dynamic": false
            },
            "store": {
              "indexType": "scorch",
              "kvStoreName": ""
            },
            "doc_config": {
              "mode": "scope.collection.type_field",
              "type_field": "type",
              "docid_prefix_delim": "",
              "docid_regexp": ""
            }
          },
          "sourceType": "couchbase",
          "sourceName": "travel-sample",
          "sourceParams": {},
          "planParams": {
            "maxPartitionsPerPIndex": 1024,
            "numReplicas": 0,
            "indexPartitions": 1
          }
        }
    """.trimIndent()

    cluster.httpClient.put(
        target = HttpTarget.search(),
        path = formatPath("/api/index/{}", indexName),
        body = HttpBody.Companion.json(index)
    ).checkSuccess()
}

private fun CouchbaseHttpResponse.checkSuccess() {
    if (!success) throw RuntimeException(
        "HTTP request failed with status $statusCode and body: $contentAsString")
}

public fun main() {
    val cluster = Cluster.connect("localhost,127.0.0.1", "Administrator", "password")

    runBlocking {
        // installIndex(cluster)
        //delay(3.seconds)
        simpleQuery(cluster)
        cluster.disconnect()
    }
}
