package io.github.sooniln.jvmcollectionsbenchmark.jitasm

import io.github.sooniln.fastcollect.ints.IntHashSet
import java.util.*

/**
 * A harness which runs [IntHashSet.contains] in a tight loop to force C2 compilation and allows for JIT ASM output of
 * the resulting compilations.
 */
object IntHashSetContainsAsmProbe {
    private val N_KEYS = 1 shl 14

    @JvmStatic
    fun main(args: Array<String>) {
        val rnd = Random()
        val m = IntHashSet(N_KEYS)
        val n = org.eclipse.collections.impl.set.mutable.primitive.IntHashSet(N_KEYS)
        val inElements = IntArray(N_KEYS)
        val outElements = IntArray(N_KEYS)

        var i = 0
        while (i < N_KEYS) {
            val r = rnd.nextInt(N_KEYS)
            if (!m.contains(r)) {
                m.add(r)
                n.add(r)
                inElements[i++] = r
            }
        }

        i = 0
        while (i < N_KEYS) {
            val r = rnd.nextInt(N_KEYS * 10)
            if (!m.contains(r)) {
                outElements[i++] = r
            }
        }

        // Warmup + measurement-ish loop. We want a very hot call-site so C2 compiles it.
        var insum: Long = 0
        var outsum: Long = 0
        i = 0
        while (i < outElements.size) {
            if (m.contains(outElements[i])) ++insum
            if (n.contains(outElements[i])) ++outsum
            i++
        }
        println(insum + outsum)
    }
}
