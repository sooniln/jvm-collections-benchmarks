package io.github.sooniln.jvmcollectionsbenchmark.jitasm

import io.github.sooniln.fastcollect.ints.Int2IntHashMap
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import java.util.*

object Int2IntHashMapIterateAsmProbe {
    private val N_KEYS = 1 shl 20

    @JvmStatic
    fun main(args: Array<String>) {
        val rnd = Random()
        val m1 = Int2IntHashMap(N_KEYS)
        val m2 = Int2IntOpenHashMap(N_KEYS)
        val inElements = IntArray(N_KEYS)

        var i = 0
        while (i < N_KEYS) {
            val r = rnd.nextInt()
            if (!m1.containsKey(r)) {
                m1[r] = r
                m2.put(r, r)
                inElements[i++] = r
            }
        }

        // Warmup + measurement-ish loop. We want a very hot call-site so C2 compiles it.
        println(iterate1(m1))
        println(iterate2(m2))
    }

    fun iterate1(map: Int2IntHashMap): Int {
        var sum = 0
        for ((key, entry) in map) sum += key + entry
        return sum
    }

    fun iterate2(map: Int2IntOpenHashMap): Int {
        var sum = 0
        for ((key, entry) in map) sum += key + entry
        return sum
    }
}
