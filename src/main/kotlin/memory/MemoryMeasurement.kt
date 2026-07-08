package io.github.sooniln.jvmcollectionsbenchmark.memory

import io.github.sooniln.jvmcollectionsbenchmark.BenchmarkableIntList
import io.github.sooniln.jvmcollectionsbenchmark.BenchmarkableIntMap
import io.github.sooniln.jvmcollectionsbenchmark.BenchmarkableIntSet
import io.github.sooniln.jvmcollectionsbenchmark.BenchmarkableLongList
import io.github.sooniln.jvmcollectionsbenchmark.BenchmarkableLongMap
import io.github.sooniln.jvmcollectionsbenchmark.BenchmarkableLongSet
import org.openjdk.jol.info.GraphLayout.parseInstance
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Measures retained memory (full object graph via JOL) for each collection type at geometrically
 * increasing sizes. Output is CSV on stdout; redirect to a file and graph bytes vs size.
 *
 * Each collection is created once and grown incrementally between measurement points.
 *
 * Run via: ./gradlew :benchmark:runMemoryMeasurement > memory_results.csv
 */
fun main() {
    println("benchmark,library,order,size,median,mean,stdev,unit")

    // Lists

    for ((type, list) in BenchmarkableIntList.all()) {
        try {
            var prev = 0
            val start = System.nanoTime()
            outer@ for (size in sizes) {
                list.ensureCapacity(size)
                for (i in prev until size) {
                    if ((System.nanoTime() - start).nanoseconds > 60.seconds) break@outer
                    list.add(i)
                }
                prev = size

                val mem = parseInstance(list.rawList).totalSize()
                println("IntList.memory,$type,,$size,$mem,$mem,0,bytes")
            }
        } catch (e: OutOfMemoryError) {
            System.err.println(e.message)
        }
    }

    for ((type, list) in BenchmarkableLongList.all()) {
        try {
            var prev = 0
            val start = System.nanoTime()
            outer@ for (size in sizes) {
                list.ensureCapacity(size)
                for (i in prev until size) {
                    if ((System.nanoTime() - start).nanoseconds > 60.seconds) break@outer
                    list.add(i.toLong())
                }
                prev = size

                val mem = parseInstance(list.rawList).totalSize()
                println("LongList.memory,$type,,$size,$mem,$mem,0,bytes")
            }
        } catch (e: OutOfMemoryError) {
            System.err.println(e.message)
        }
    }

    // Sets

    for ((type, set) in BenchmarkableIntSet.all()) {
        try {
            var prev = 0
            val start = System.nanoTime()
            outer@ for (size in sizes) {
                set.ensureCapacity(size)
                for (i in prev until size) {
                    if ((System.nanoTime() - start).nanoseconds > 60.seconds) break@outer
                    set.add(i)
                }
                prev = size

                val mem = parseInstance(set.rawSet).totalSize()
                println("IntSet.memory,$type,,$size,$mem,$mem,0,bytes")
            }
        } catch (e: OutOfMemoryError) {
            System.err.println(e.message)
        }
    }

    for ((type, set) in BenchmarkableLongSet.all()) {
        try {
            var prev = 0
            val start = System.nanoTime()
            outer@ for (size in sizes) {
                set.ensureCapacity(size)
                for (i in prev until size) {
                    if ((System.nanoTime() - start).nanoseconds > 60.seconds) break@outer
                    set.add(i.toLong())
                }
                prev = size

                val mem = parseInstance(set.rawSet).totalSize()
                println("LongSet.memory,$type,,$size,$mem,$mem,0,bytes")
            }
        } catch (e: OutOfMemoryError) {
            System.err.println(e.message)
        }
    }

    // Maps

    for ((type, map) in BenchmarkableIntMap.all()) {
        try {
            var prev = 0
            val start = System.nanoTime()
            outer@ for (size in sizes) {
                map.ensureCapacity(size)
                for (i in prev until size) {
                    if ((System.nanoTime() - start).nanoseconds > 60.seconds) break@outer
                    map.put(i, i)
                }
                prev = size

                val mem = parseInstance(map.rawMap).totalSize()
                println("IntMap.memory,$type,,$size,$mem,$mem,0,bytes")
            }
        } catch (e: OutOfMemoryError) {
            System.err.println(e.message)
        }
    }

    for ((type, map) in BenchmarkableLongMap.all()) {
        try {
            var prev = 0
            val start = System.nanoTime()
            outer@ for (size in sizes) {
                map.ensureCapacity(size)
                for (i in prev until size) {
                    if ((System.nanoTime() - start).nanoseconds > 60.seconds) break@outer
                    map.put(i.toLong(), i)
                }
                prev = size

                val mem = parseInstance(map.rawMap).totalSize()
                println("LongMap.memory,$type,,$size,$mem,$mem,0,bytes")
            }
        } catch (e: OutOfMemoryError) {
            System.err.println(e.message)
        }
    }
}

private val sizes = listOf(0,514,766,1026,1534,2050,3070,4098,6142,8194,12286,16386,24574,32770,49150,65538,98302,131074,196606,262146,393214,524290,786430,1048578,1572862,2097154,3145726,4194306,6291454,8388610,12582910,16777218,25165822,33554434,50331646,67108866,100663294)
