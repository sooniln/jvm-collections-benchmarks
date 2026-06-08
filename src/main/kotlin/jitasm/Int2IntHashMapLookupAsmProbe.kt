package io.github.sooniln.jvmcollectionsbenchmark.jitasm

import com.koloboke.collect.hash.HashConfig
import com.koloboke.collect.map.hash.HashIntIntMaps
import io.github.sooniln.fastcollect.ints.Int2IntHashMap
import java.util.*

object Int2IntHashMapLookupAsmProbe {
    private val N_KEYS = 1 shl 20

    @JvmStatic
    fun main(args: Array<String>) {
        val rnd = Random()
        val m1 = Int2IntHashMap(N_KEYS)
        val m2 = HashIntIntMaps.getDefaultFactory().withHashConfig(HashConfig.fromLoads(.75/2, .75, .75)).newMutableMap()
        val inElements = IntArray(N_KEYS)
        val outElements = IntArray(N_KEYS)

        var i = 0
        while (i < N_KEYS) {
            val r = rnd.nextInt()
            if (!m1.containsKey(r)) {
                m1[r] = r
                m2.put(r, r)
                inElements[i++] = r
            }
        }

        i = 0
        while (i < N_KEYS) {
            val r = rnd.nextInt()
            if (!m1.containsKey(r)) {
                outElements[i++] = r
            }
        }

        /*
        i = 0;
        while (i < N_KEYS) {
            int r = rnd.nextInt(N_KEYS * 10);
            if (!m.contains(r)) {
                outElements[i++] = r;
            }
        }*/

        // Warmup + measurement-ish loop. We want a very hot call-site so C2 compiles it.
        var insum = 0
        i = 0
        while (i < outElements.size) {
            insum += m1[inElements[i]]
            insum += m1[outElements[i]]
            insum += m2.get(inElements[i])
            insum += m2.get(outElements[i])
            i++
        }
        println(insum)
    }
}
