package io.github.sooniln.jvmcollectionsbenchmark.memory

import io.github.sooniln.jvmcollectionsbenchmark.BenchmarkableIntList
import io.github.sooniln.jvmcollectionsbenchmark.BenchmarkableIntMap
import io.github.sooniln.jvmcollectionsbenchmark.BenchmarkableIntSet
import io.github.sooniln.jvmcollectionsbenchmark.BenchmarkableLongList
import io.github.sooniln.jvmcollectionsbenchmark.BenchmarkableLongMap
import io.github.sooniln.jvmcollectionsbenchmark.BenchmarkableLongSet
import org.openjdk.jol.info.GraphLayout.parseInstance

/**
 * Measures retained memory (full object graph via JOL) for each collection type at geometrically
 * increasing sizes. Output is CSV on stdout; redirect to a file and graph bytes vs size.
 *
 * Each collection is created once and grown incrementally between measurement points.
 *
 * Run via: ./gradlew :benchmark:runMemoryMeasurement > memory_results.csv
 */
fun main() {
    println("collection,size,totalBytes")

    // Lists

    for ((type, list) in BenchmarkableIntList.all()) {
        var prev = 0
        for (size in growingSizes(from = 0, to = 1_000_000)) {
            for (i in prev until size) list.add(i)
            prev = size

            println("${type}IntList,$size,${parseInstance(list.rawList).totalSize()}")
        }
    }

    for ((type, list) in BenchmarkableLongList.all()) {
        var prev = 0
        for (size in growingSizes(from = 0, to = 1_000_000)) {
            for (i in prev until size) list.add(i.toLong())
            prev = size

            println("${type}LongList,$size,${parseInstance(list.rawList).totalSize()}")
        }
    }

    // Sets

    for ((type, set) in BenchmarkableIntSet.all()) {
        var prev = 0
        for (size in growingSizes(from = 0, to = 1_000_000)) {
            for (i in prev until size) set.add(i)
            prev = size

            println("${type}IntSet,$size,${parseInstance(set.rawSet).totalSize()}")
        }
    }

    for ((type, set) in BenchmarkableLongSet.all()) {
        var prev = 0
        for (size in growingSizes(from = 0, to = 1_000_000)) {
            for (i in prev until size) set.add(i.toLong())
            prev = size

            println("${type}LongSet,$size,${parseInstance(set.rawSet).totalSize()}")
        }
    }

    // Maps

    for ((type, map) in BenchmarkableIntMap.all()) {
        var prev = 0
        for (size in growingSizes(from = 0, to = 1_000_000)) {
            for (i in prev until size) map.put(i, i)
            prev = size

            println("${type}IntMap,$size,${parseInstance(map.rawMap).totalSize()}")
        }
    }

    for ((type, map) in BenchmarkableLongMap.all()) {
        var prev = 0
        for (size in growingSizes(from = 0, to = 1_000_000)) {
            for (i in prev until size) map.put(i.toLong(), i)
            prev = size

            println("${type}LongMap,$size,${parseInstance(map.rawMap).totalSize()}")
        }
    }
}

private fun growingSizes(from: Int, to: Int): List<Int> {
    val sizes = mutableListOf<Int>()
    var s = from
    while (s <= to) {
        sizes += s
        val next = maxOf(s + 10, (s * 1.1).toInt())
        s = next
    }
    if (sizes.last() != to) sizes += to
    return sizes
}
