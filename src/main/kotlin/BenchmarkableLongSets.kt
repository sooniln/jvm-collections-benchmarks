package io.github.sooniln.jvmcollectionsbenchmark

import androidx.collection.MutableLongSet
import com.carrotsearch.hppc.procedures.LongProcedure
import com.koloboke.collect.hash.HashConfig
import com.koloboke.collect.set.hash.HashLongSet
import com.koloboke.collect.set.hash.HashLongSets
import gnu.trove.impl.Constants
import gnu.trove.set.hash.TLongHashSet
import io.github.sooniln.fastcollect.*
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import java.util.function.LongConsumer

interface BenchmarkableLongSet<T> {
    val rawSet: T

    val size: Int

    fun newInstance(): BenchmarkableLongSet<T>
    fun copyInstance(): BenchmarkableLongSet<T> {
        val copy = newInstance()
        copy.addAll(this)
        return copy
    }

    fun ensureCapacity(capacity: Int) {}
    fun forEach(action: LongConsumer) = iterate(action)
    fun iterate(action: LongConsumer)
    fun contains(key: Long): Boolean
    fun add(key: Long): Boolean
    fun remove(key: Long): Boolean
    fun addAll(otherSet: BenchmarkableLongSet<T>): Boolean
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
            "LibGDX" to { LibGdxSet() },
        )

        fun from(type: String): BenchmarkableLongSet<*> = map.getOrElse(type) { throw IllegalArgumentException("Unknown type: $type") }.invoke()

        fun all(): Sequence<Pair<String, BenchmarkableLongSet<*>>> = map.entries.asSequence().map { it.key to it.value.invoke() }

        private class JreSet : BenchmarkableLongSet<HashSet<Long>> {

            override val rawSet: HashSet<Long> = HashSet()

            override val size: Int get() = rawSet.size

            override fun newInstance(): BenchmarkableLongSet<HashSet<Long>> = JreSet()
            override fun iterate(action: LongConsumer) { for (key in rawSet) action.accept(key) }
            override fun contains(key: Long) = rawSet.contains(key)
            override fun add(key: Long) = rawSet.add(key)
            override fun remove(key: Long) = rawSet.remove(key)
            override fun addAll(otherSet: BenchmarkableLongSet<HashSet<Long>>) = rawSet.addAll(otherSet.rawSet)
            override fun clear() = rawSet.clear()
        }

        private class FastCollectSet : BenchmarkableLongSet<LongHashSet> {

            override val rawSet: LongHashSet = LongHashSet()

            override val size: Int get() = rawSet.size

            override fun newInstance(): BenchmarkableLongSet<LongHashSet> = FastCollectSet()
            override fun ensureCapacity(capacity: Int) = rawSet.ensureCapacity(capacity)
            override fun forEach(action: LongConsumer) = rawSet.foreach { key -> action.accept(key) }
            override fun iterate(action: LongConsumer) { for (key in rawSet) action.accept(key) }
            override fun contains(key: Long) = rawSet.contains(key)
            override fun add(key: Long) = rawSet.add(key)
            override fun remove(key: Long) = rawSet.remove(key)
            override fun addAll(otherSet: BenchmarkableLongSet<LongHashSet>) = rawSet.addAll(otherSet.rawSet)
            override fun clear() = rawSet.clear()
        }

        private class FastutilSet : BenchmarkableLongSet<LongOpenHashSet> {

            override val rawSet: LongOpenHashSet = LongOpenHashSet()

            override val size: Int get() = rawSet.size

            override fun newInstance(): BenchmarkableLongSet<LongOpenHashSet> = FastutilSet()
            override fun ensureCapacity(capacity: Int) = rawSet.ensureCapacity(capacity)
            override fun forEach(action: LongConsumer) = rawSet.forEach(LongConsumer { key -> action.accept(key) })
            @Suppress("DEPRECATION")
            override fun iterate(action: LongConsumer) { for (key in rawSet) action.accept(key) }
            override fun contains(key: Long) = rawSet.contains(key)
            override fun add(key: Long) = rawSet.add(key)
            override fun remove(key: Long) = rawSet.remove(key)
            override fun addAll(otherSet: BenchmarkableLongSet<LongOpenHashSet>) = rawSet.addAll(otherSet.rawSet)
            override fun clear() = rawSet.clear()
        }

        private class AndroidXSet : BenchmarkableLongSet<MutableLongSet> {

            override val rawSet: MutableLongSet = MutableLongSet()

            override val size: Int get() = rawSet.size

            override fun newInstance(): BenchmarkableLongSet<MutableLongSet> = AndroidXSet()
            override fun forEach(action: LongConsumer) = rawSet.forEach { action.accept(it) }
            override fun iterate(action: LongConsumer) = throw UnsupportedOperationException()
            override fun contains(key: Long) = rawSet.contains(key)
            override fun add(key: Long) = rawSet.add(key)
            override fun remove(key: Long) = rawSet.remove(key)
            override fun addAll(otherSet: BenchmarkableLongSet<MutableLongSet>) = rawSet.addAll(otherSet.rawSet)
            override fun clear() = rawSet.clear()
        }

        private class TroveSet : BenchmarkableLongSet<TLongHashSet> {

            override val rawSet: TLongHashSet = TLongHashSet(Constants.DEFAULT_CAPACITY, 0.75f)

            override val size: Int get() = rawSet.size()

            override fun newInstance(): BenchmarkableLongSet<TLongHashSet> = TroveSet()
            override fun ensureCapacity(capacity: Int) = rawSet.ensureCapacity(capacity)
            override fun forEach(action: LongConsumer) { rawSet.forEach { action.accept(it); return@forEach true } }
            override fun iterate(action: LongConsumer) { for (key in rawSet) action.accept(key) }
            override fun contains(key: Long) = rawSet.contains(key)
            override fun add(key: Long) = rawSet.add(key)
            override fun remove(key: Long) = rawSet.remove(key)
            override fun addAll(otherSet: BenchmarkableLongSet<TLongHashSet>) = rawSet.addAll(otherSet.rawSet)
            override fun clear() = rawSet.clear()
        }

        private class KolobokeSet : BenchmarkableLongSet<HashLongSet> {

            override val rawSet: HashLongSet = HashLongSets.getDefaultFactory().withHashConfig(HashConfig.fromLoads(.75/2, .75, .75)).newMutableSet()

            override val size: Int get() = rawSet.size

            override fun newInstance(): BenchmarkableLongSet<HashLongSet> = KolobokeSet()
            override fun ensureCapacity(capacity: Int) { rawSet.ensureCapacity(capacity.toLong()) }
            override fun forEach(action: LongConsumer) = rawSet.forEach { key -> action.accept(key) }
            @Suppress("DEPRECATION")
            override fun iterate(action: LongConsumer) { for (key in rawSet) action.accept(key) }
            override fun contains(key: Long) = rawSet.contains(key)
            override fun add(key: Long): Boolean = rawSet.add(key)
            override fun remove(key: Long) = rawSet.removeLong(key)
            override fun addAll(otherSet: BenchmarkableLongSet<HashLongSet>) = rawSet.addAll(otherSet.rawSet)
            override fun clear() = rawSet.clear()
        }

        private class EclipseSet : BenchmarkableLongSet<org.eclipse.collections.impl.set.mutable.primitive.LongHashSet> {

            override val rawSet: org.eclipse.collections.impl.set.mutable.primitive.LongHashSet = org.eclipse.collections.impl.set.mutable.primitive.LongHashSet()

            override val size: Int get() = rawSet.size()

            override fun newInstance(): BenchmarkableLongSet<org.eclipse.collections.impl.set.mutable.primitive.LongHashSet> = EclipseSet()
            override fun ensureCapacity(capacity: Int) {}
            override fun forEach(action: LongConsumer) = rawSet.forEach { key -> action.accept(key) }
            override fun iterate(action: LongConsumer) = throw UnsupportedOperationException()
            override fun contains(key: Long) = rawSet.contains(key)
            override fun add(key: Long): Boolean = rawSet.add(key)
            override fun remove(key: Long) = rawSet.remove(key)
            override fun addAll(otherSet: BenchmarkableLongSet<org.eclipse.collections.impl.set.mutable.primitive.LongHashSet>) = rawSet.addAll(otherSet.rawSet)
            override fun clear() = rawSet.clear()
        }

        private class HPPCSet : BenchmarkableLongSet<com.carrotsearch.hppc.LongHashSet> {

            override val rawSet: com.carrotsearch.hppc.LongHashSet = com.carrotsearch.hppc.LongHashSet()

            override val size: Int get() = rawSet.size()

            override fun newInstance(): BenchmarkableLongSet<com.carrotsearch.hppc.LongHashSet> = HPPCSet()
            override fun ensureCapacity(capacity: Int) { rawSet.ensureCapacity(capacity) }
            override fun forEach(action: LongConsumer) { rawSet.forEach(LongProcedure { key -> action.accept(key) }) }
            override fun iterate(action: LongConsumer) { for (key in rawSet) action.accept(key.value) }
            override fun contains(key: Long) = rawSet.contains(key)
            override fun add(key: Long): Boolean = rawSet.add(key)
            override fun remove(key: Long) = rawSet.remove(key)
            override fun addAll(otherSet: BenchmarkableLongSet<com.carrotsearch.hppc.LongHashSet>) = rawSet.addAll(otherSet.rawSet) > 0
            override fun clear() = rawSet.clear()
        }

        private class AgronaSet : BenchmarkableLongSet<org.agrona.collections.LongHashSet> {

            override val rawSet: org.agrona.collections.LongHashSet = org.agrona.collections.LongHashSet(8, .75f)

            override val size: Int get() = rawSet.size

            override fun newInstance(): BenchmarkableLongSet<org.agrona.collections.LongHashSet> = AgronaSet()
            override fun forEach(action: LongConsumer) = rawSet.forEachLong { key -> action.accept(key) }
            override fun iterate(action: LongConsumer) { for (key in rawSet) action.accept(key) }
            override fun contains(key: Long) = rawSet.contains(key)
            override fun add(key: Long): Boolean = rawSet.add(key)
            override fun remove(key: Long) = rawSet.remove(key)
            override fun addAll(otherSet: BenchmarkableLongSet<org.agrona.collections.LongHashSet>) = rawSet.addAll(otherSet.rawSet)
            override fun clear() = rawSet.clear()
        }

        private class LibGdxSet : BenchmarkableLongSet<com.badlogic.gdx.utils.LongSet> {

            override val rawSet: com.badlogic.gdx.utils.LongSet = com.badlogic.gdx.utils.LongSet()

            override val size: Int get() = rawSet.size

            override fun newInstance(): BenchmarkableLongSet<com.badlogic.gdx.utils.LongSet> = LibGdxSet()
            override fun ensureCapacity(capacity: Int) = rawSet.ensureCapacity(capacity - rawSet.size)
            override fun iterate(action: LongConsumer) { val iterator = rawSet.iterator(); while (iterator.hasNext) action.accept(iterator.next()) }
            override fun contains(key: Long) = rawSet.contains(key)
            override fun add(key: Long): Boolean = rawSet.add(key)
            override fun remove(key: Long) = rawSet.remove(key)
            override fun addAll(otherSet: BenchmarkableLongSet<com.badlogic.gdx.utils.LongSet>) = rawSet.addAll(otherSet.rawSet)
            override fun clear() = rawSet.clear()
        }
    }
}
