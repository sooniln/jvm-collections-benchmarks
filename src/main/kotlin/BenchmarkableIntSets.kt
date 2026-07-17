package io.github.sooniln.jvmcollectionsbenchmark

import androidx.collection.MutableIntSet
import com.carrotsearch.hppc.procedures.IntProcedure
import com.koloboke.collect.hash.HashConfig
import com.koloboke.collect.set.hash.HashIntSet
import com.koloboke.collect.set.hash.HashIntSets
import gnu.trove.impl.Constants
import gnu.trove.set.hash.TIntHashSet
import io.github.sooniln.fastcollect.ints.IntHashSet
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import java.util.function.IntConsumer

interface BenchmarkableIntSet<T> {
    val rawSet: T

    val size: Int

    fun newInstance(): BenchmarkableIntSet<T>
    fun copyInstance(): BenchmarkableIntSet<T> {
        val copy = newInstance()
        copy.addAll(this)
        return copy
    }

    fun ensureCapacity(capacity: Int) {}
    fun forEach(action: (Int) -> Unit) = iterate(action)
    fun iterate(action: (Int) -> Unit)
    fun contains(key: Int): Boolean
    fun add(key: Int): Boolean
    fun remove(key: Int): Boolean
    fun addAll(otherSet: BenchmarkableIntSet<T>): Boolean
    fun clear()

    companion object {
        private val map = mapOf(
            "JRE" to { JreSet() },
            "FastCollect" to { FastCollectSet() },
            "Fastutil" to { FastutilSet() },
            "AndroidX" to { AndroidXSet() },
            "Trove" to { TroveSet() },
            "Koloboke" to { KolobokeSet() },
            "Eclipse" to { EclipseSet() },
            "HPPC" to { HPPCSet() },
            "Agrona" to { AgronaSet() },
        )

        fun from(type: String): BenchmarkableIntSet<*> = map.getOrElse(type) { throw IllegalArgumentException("Unknown type: $type") }.invoke()

        fun all(): Sequence<Pair<String, BenchmarkableIntSet<*>>> = map.entries.asSequence().map { it.key to it.value.invoke() }

        private class JreSet : BenchmarkableIntSet<HashSet<Int>> {

            override val rawSet: HashSet<Int> = HashSet()

            override val size: Int get() = rawSet.size

            override fun newInstance(): BenchmarkableIntSet<HashSet<Int>> = JreSet()
            override fun iterate(action: (Int) -> Unit) { for (key in rawSet) action(key) }
            override fun contains(key: Int) = rawSet.contains(key)
            override fun add(key: Int) = rawSet.add(key)
            override fun remove(key: Int) = rawSet.remove(key)
            override fun addAll(otherSet: BenchmarkableIntSet<HashSet<Int>>) = rawSet.addAll(otherSet.rawSet)
            override fun clear() = rawSet.clear()
        }

        private class FastCollectSet : BenchmarkableIntSet<IntHashSet> {

            override val rawSet: IntHashSet = IntHashSet()

            override val size: Int get() = rawSet.size

            override fun newInstance(): BenchmarkableIntSet<IntHashSet> = FastCollectSet()
            override fun ensureCapacity(capacity: Int) = rawSet.ensureCapacity(capacity)
            override fun forEach(action: (Int) -> Unit) = rawSet.forEach { key -> action(key) }
            override fun iterate(action: (Int) -> Unit) { for (key in rawSet) action(key) }
            override fun contains(key: Int) = rawSet.contains(key)
            override fun add(key: Int) = rawSet.add(key)
            override fun remove(key: Int) = rawSet.remove(key)
            override fun addAll(otherSet: BenchmarkableIntSet<IntHashSet>) = rawSet.addAll(otherSet.rawSet)
            override fun clear() = rawSet.clear()
        }

        private class FastutilSet : BenchmarkableIntSet<IntOpenHashSet> {

            override val rawSet: IntOpenHashSet = IntOpenHashSet()

            override val size: Int get() = rawSet.size

            override fun newInstance(): BenchmarkableIntSet<IntOpenHashSet> = FastutilSet()
            override fun ensureCapacity(capacity: Int) = rawSet.ensureCapacity(capacity)
            override fun forEach(action: (Int) -> Unit) = rawSet.forEach(IntConsumer { key -> action(key) })
            @Suppress("DEPRECATION")
            override fun iterate(action: (Int) -> Unit) { for (key in rawSet) action(key) }
            override fun contains(key: Int) = rawSet.contains(key)
            override fun add(key: Int) = rawSet.add(key)
            override fun remove(key: Int) = rawSet.remove(key)
            override fun addAll(otherSet: BenchmarkableIntSet<IntOpenHashSet>) = rawSet.addAll(otherSet.rawSet)
            override fun clear() = rawSet.clear()
        }

        private class AndroidXSet : BenchmarkableIntSet<MutableIntSet> {

            override val rawSet: MutableIntSet = MutableIntSet()

            override val size: Int get() = rawSet.size

            override fun newInstance(): BenchmarkableIntSet<MutableIntSet> = AndroidXSet()
            override fun forEach(action: (Int) -> Unit) = rawSet.forEach { action(it) }
            override fun iterate(action: (Int) -> Unit) = throw UnsupportedOperationException()
            override fun contains(key: Int) = rawSet.contains(key)
            override fun add(key: Int) = rawSet.add(key)
            override fun remove(key: Int) = rawSet.remove(key)
            override fun addAll(otherSet: BenchmarkableIntSet<MutableIntSet>) = rawSet.addAll(otherSet.rawSet)
            override fun clear() = rawSet.clear()
        }

        private class TroveSet : BenchmarkableIntSet<TIntHashSet> {

            override val rawSet: TIntHashSet = TIntHashSet(Constants.DEFAULT_CAPACITY, 0.75f)

            override val size: Int get() = rawSet.size()

            override fun newInstance(): BenchmarkableIntSet<TIntHashSet> = TroveSet()
            override fun ensureCapacity(capacity: Int) = rawSet.ensureCapacity(capacity)
            override fun forEach(action: (Int) -> Unit) { rawSet.forEach { action(it); return@forEach true } }
            override fun iterate(action: (Int) -> Unit) { for (key in rawSet) action(key) }
            override fun contains(key: Int) = rawSet.contains(key)
            override fun add(key: Int) = rawSet.add(key)
            override fun remove(key: Int) = rawSet.remove(key)
            override fun addAll(otherSet: BenchmarkableIntSet<TIntHashSet>) = rawSet.addAll(otherSet.rawSet)
            override fun clear() = rawSet.clear()
        }

        private class KolobokeSet : BenchmarkableIntSet<HashIntSet> {

            override val rawSet: HashIntSet = HashIntSets.getDefaultFactory().withHashConfig(HashConfig.fromLoads(.75/2, .75, .75)).newMutableSet()

            override val size: Int get() = rawSet.size

            override fun newInstance(): BenchmarkableIntSet<HashIntSet> = KolobokeSet()
            override fun ensureCapacity(capacity: Int) { rawSet.ensureCapacity(capacity.toLong()) }
            override fun forEach(action: (Int) -> Unit) = rawSet.forEach { key -> action(key) }
            @Suppress("DEPRECATION")
            override fun iterate(action: (Int) -> Unit) { for (key in rawSet) action(key) }
            override fun contains(key: Int) = rawSet.contains(key)
            override fun add(key: Int): Boolean = rawSet.add(key)
            override fun remove(key: Int) = rawSet.removeInt(key)
            override fun addAll(otherSet: BenchmarkableIntSet<HashIntSet>) = rawSet.addAll(otherSet.rawSet)
            override fun clear() = rawSet.clear()
        }

        private class EclipseSet : BenchmarkableIntSet<org.eclipse.collections.impl.set.mutable.primitive.IntHashSet> {

            override val rawSet: org.eclipse.collections.impl.set.mutable.primitive.IntHashSet = org.eclipse.collections.impl.set.mutable.primitive.IntHashSet()

            override val size: Int get() = rawSet.size()

            override fun newInstance(): BenchmarkableIntSet<org.eclipse.collections.impl.set.mutable.primitive.IntHashSet> = EclipseSet()
            override fun ensureCapacity(capacity: Int) {}
            override fun forEach(action: (Int) -> Unit) = rawSet.forEach { key -> action(key) }
            override fun iterate(action: (Int) -> Unit) = throw UnsupportedOperationException()
            override fun contains(key: Int) = rawSet.contains(key)
            override fun add(key: Int): Boolean = rawSet.add(key)
            override fun remove(key: Int) = rawSet.remove(key)
            override fun addAll(otherSet: BenchmarkableIntSet<org.eclipse.collections.impl.set.mutable.primitive.IntHashSet>) = rawSet.addAll(otherSet.rawSet)
            override fun clear() = rawSet.clear()
        }

        private class HPPCSet : BenchmarkableIntSet<com.carrotsearch.hppc.IntHashSet> {

            override val rawSet: com.carrotsearch.hppc.IntHashSet = com.carrotsearch.hppc.IntHashSet()

            override val size: Int get() = rawSet.size()

            override fun newInstance(): BenchmarkableIntSet<com.carrotsearch.hppc.IntHashSet> = HPPCSet()
            override fun ensureCapacity(capacity: Int) { rawSet.ensureCapacity(capacity) }
            override fun forEach(action: (Int) -> Unit) { rawSet.forEach(IntProcedure { key -> action(key) }) }
            override fun iterate(action: (Int) -> Unit) { for (key in rawSet) action(key.value) }
            override fun contains(key: Int) = rawSet.contains(key)
            override fun add(key: Int): Boolean = rawSet.add(key)
            override fun remove(key: Int) = rawSet.remove(key)
            override fun addAll(otherSet: BenchmarkableIntSet<com.carrotsearch.hppc.IntHashSet>) = rawSet.addAll(otherSet.rawSet) > 0
            override fun clear() = rawSet.clear()
        }

        private class AgronaSet : BenchmarkableIntSet<org.agrona.collections.IntHashSet> {

            override val rawSet: org.agrona.collections.IntHashSet = org.agrona.collections.IntHashSet(8, .75f)

            override val size: Int get() = rawSet.size

            override fun newInstance(): BenchmarkableIntSet<org.agrona.collections.IntHashSet> = AgronaSet()
            override fun forEach(action: (Int) -> Unit) = rawSet.forEachInt { key -> action(key) }
            override fun iterate(action: (Int) -> Unit) { for (key in rawSet) action(key) }
            override fun contains(key: Int) = rawSet.contains(key)
            override fun add(key: Int): Boolean = rawSet.add(key)
            override fun remove(key: Int) = rawSet.remove(key)
            override fun addAll(otherSet: BenchmarkableIntSet<org.agrona.collections.IntHashSet>) = rawSet.addAll(otherSet.rawSet)
            override fun clear() = rawSet.clear()
        }
    }
}
