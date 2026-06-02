package io.github.sooniln.jvmcollectionsbenchmark.memory

import io.github.sooniln.jvmcollectionsbenchmark.BenchmarkableIntList
import io.github.sooniln.jvmcollectionsbenchmark.BenchmarkableIntMap
import io.github.sooniln.jvmcollectionsbenchmark.BenchmarkableIntSet
import io.github.sooniln.jvmcollectionsbenchmark.BenchmarkableLongList
import io.github.sooniln.jvmcollectionsbenchmark.BenchmarkableLongMap
import io.github.sooniln.jvmcollectionsbenchmark.BenchmarkableLongSet
import org.openjdk.jol.info.GraphLayout.parseInstance

/**
 * Measures the total retained memory of distributing a fixed number of integers across N
 * collections, where N grows geometrically from 1 to TOTAL_ELEMENTS. At each step all N
 * collections hold an equal share of the data. Since all N collections are structurally
 * identical, one is measured with JOL and the result is multiplied by N. Output is CSV on stdout.
 *
 * Run via: ./gradlew :benchmark:runSplitMemoryMeasurement > split_memory_results.csv
 */
fun main() {
    println("collection,numCollections,totalBytes")

    val totalElements = 1_000_000
    val splits = growingSplitCounts(totalElements)

    // Lists

    for ((type, list) in BenchmarkableIntList.all()) {
        emptySplitRow(type, totalElements) { list.newInstance() }

        for (numCollections in splits) {
            val size = totalElements / numCollections
            list.ensureCapacity(size)
            for (i in list.size until size) list.add(i)
            splitRow("${type}IntList", numCollections, list.rawList)
        }
    }

    for ((type, list) in BenchmarkableLongList.all()) {
        emptySplitRow(type, totalElements) { list.newInstance() }

        for (numCollections in splits) {
            val size = totalElements / numCollections
            list.ensureCapacity(size)
            for (i in list.size until size) list.add(i.toLong())
            splitRow("${type}LongList", numCollections, list.rawList)
        }
    }

    // Sets

    for ((type, set) in BenchmarkableIntSet.all()) {
        emptySplitRow(type, totalElements) { set.newInstance() }

        for (numCollections in splits) {
            val size = totalElements / numCollections
            set.ensureCapacity(size)
            for (i in set.size until size) set.add(i)
            splitRow("${type}IntList", numCollections, set.rawSet)
        }
    }

    for ((type, set) in BenchmarkableLongSet.all()) {
        emptySplitRow(type, totalElements) { set.newInstance() }

        for (numCollections in splits) {
            val size = totalElements / numCollections
            set.ensureCapacity(size)
            for (i in set.size until size) set.add(i.toLong())
            splitRow("${type}LongList", numCollections, set.rawSet)
        }
    }

    // Maps

    for ((type, map) in BenchmarkableIntMap.all()) {
        emptySplitRow(type, totalElements) { map.newInstance() }

        for (numCollections in splits) {
            val size = totalElements / numCollections
            map.ensureCapacity(size)
            for (i in map.size until size) map.put(i, i)
            splitRow("${type}IntList", numCollections, map.rawMap)
        }
    }

    for ((type, map) in BenchmarkableLongMap.all()) {
        emptySplitRow(type, totalElements) { map.newInstance() }

        for (numCollections in splits) {
            val size = totalElements / numCollections
            map.ensureCapacity(size)
            for (i in map.size until size) map.put(i.toLong(), i.toLong())
            splitRow("${type}LongList", numCollections, map.rawMap)
        }
    }
}

private fun emptySplitRow(collection: String, numCollections: Int, constructor: () -> Any) {
    val array = Array(numCollections) { constructor() }
    println("empty$collection,$numCollections,${parseInstance(*array).totalSize()}")
}

private fun splitRow(collection: String, numCollections: Int, obj: Any?) {
    println("$collection,$numCollections,${parseInstance(obj).totalSize() * numCollections}")
}

// Returns all divisors of TOTAL_ELEMENTS in ascending order, so every split is exact.
private fun growingSplitCounts(totalElements: Int): List<Int> {
    val divisors = mutableListOf<Int>()
    var i = 1
    while (i * i <= totalElements) {
        if (totalElements % i == 0) {
            divisors += i
            if (i != totalElements / i) divisors += totalElements / i
        }
        i++
    }
    return divisors.sortedDescending()
}
