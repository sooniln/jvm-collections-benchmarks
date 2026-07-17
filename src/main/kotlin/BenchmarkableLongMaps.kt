package io.github.sooniln.jvmcollectionsbenchmark

import androidx.collection.MutableLongIntMap
import com.carrotsearch.hppc.procedures.LongIntProcedure
import com.koloboke.collect.hash.HashConfig
import com.koloboke.collect.map.hash.HashLongIntMap
import com.koloboke.collect.map.hash.HashLongIntMaps
import com.koloboke.function.LongIntConsumer
import gnu.trove.impl.Constants
import gnu.trove.map.hash.TLongIntHashMap
import io.github.sooniln.fastcollect.longs.Long2IntHashMap
import io.github.sooniln.fastcollect.longs.getOrDefault
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap
import org.eclipse.collections.impl.map.mutable.primitive.LongIntHashMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

interface BenchmarkableLongMap<T> {
    val rawMap: T

    val size: Int

    fun newInstance(): BenchmarkableLongMap<T>
    fun copyInstance(): BenchmarkableLongMap<T> {
        val copy = newInstance()
        copy.putAll(this)
        return copy
    }

    fun ensureCapacity(capacity: Int) {}
    fun forEach(action: (Long, Int) -> Unit) = iterate(action)
    fun iterate(action: (Long, Int) -> Unit)
    fun get(key: Long): Int
    fun put(key: Long, value: Int)
    fun remove(key: Long)
    fun putAll(otherMap: BenchmarkableLongMap<T>)
    fun clear()

    companion object {
        private val map = mapOf(
            "JRE" to { JreMap() },
            "FastCollect" to { FastCollectMap() },
            "Fastutil" to { FastutilMap() },
            "AndroidX" to { AndroidXMap() },
            "Trove" to { TroveMap() },
            "Koloboke" to { KolobokeMap() },
            "Eclipse" to { EclipseMap() },
            "HPPC" to { HPPCMap() },
            "Agrona" to { AgronaMap() },
        )

        fun from(type: String): BenchmarkableLongMap<*> = map.getOrElse(type) { throw IllegalArgumentException("Unknown type: $type") }.invoke()

        fun all(): Sequence<Pair<String, BenchmarkableLongMap<*>>> = map.entries.asSequence().map { it.key to it.value.invoke() }

        private class JreMap : BenchmarkableLongMap<HashMap<Long, Int>> {

            override val rawMap: HashMap<Long, Int> = HashMap()

            override val size: Int get() = rawMap.size

            override fun newInstance(): BenchmarkableLongMap<HashMap<Long, Int>> = JreMap()
            override fun iterate(action: (Long, Int) -> Unit) { for ((key, value) in rawMap) action(key, value) }
            override fun get(key: Long): Int = rawMap.getOrDefault(key, 0)
            override fun put(key: Long, value: Int) { rawMap[key] = value }
            override fun remove(key: Long) { rawMap.remove(key) }
            override fun putAll(otherMap: BenchmarkableLongMap<HashMap<Long, Int>>) = rawMap.putAll(otherMap.rawMap)
            override fun clear() = rawMap.clear()
        }

        private class FastCollectMap : BenchmarkableLongMap<Long2IntHashMap> {

            override val rawMap: Long2IntHashMap = Long2IntHashMap()

            override val size: Int get() = rawMap.size

            override fun newInstance(): BenchmarkableLongMap<Long2IntHashMap> = FastCollectMap()
            override fun ensureCapacity(capacity: Int) = rawMap.ensureCapacity(capacity)
            override fun forEach(action: (Long, Int) -> Unit) = rawMap.forEach { key, value -> action(key, value) }
            override fun iterate(action: (Long, Int) -> Unit) { for ((key, value) in rawMap) action(key, value) }
            override fun get(key: Long): Int = rawMap.getOrDefault(key, 0)
            override fun put(key: Long, value: Int) { rawMap[key] = value }
            override fun remove(key: Long) { rawMap.remove(key) }
            override fun putAll(otherMap: BenchmarkableLongMap<Long2IntHashMap>) = rawMap.putAll(otherMap.rawMap)
            override fun clear() = rawMap.clear()
        }

        private class FastutilMap : BenchmarkableLongMap<Long2IntOpenHashMap> {

            override val rawMap: Long2IntOpenHashMap = Long2IntOpenHashMap()

            override val size: Int get() = rawMap.size

            override fun newInstance(): BenchmarkableLongMap<Long2IntOpenHashMap> = FastutilMap()
            override fun ensureCapacity(capacity: Int) = rawMap.ensureCapacity(capacity)
            @Suppress("JavaMapForEach")
            override fun forEach(action: (Long, Int) -> Unit) = rawMap.forEach { key, value -> action(key, value) }
            override fun iterate(action: (Long, Int) -> Unit) { for ((key, value) in rawMap) action(key, value) }
            override fun get(key: Long): Int = rawMap.getOrDefault(key, 0)
            override fun put(key: Long, value: Int) { rawMap.put(key, value) }
            override fun remove(key: Long) { rawMap.remove(key) }
            override fun putAll(otherMap: BenchmarkableLongMap<Long2IntOpenHashMap>) = rawMap.putAll(otherMap.rawMap)
            override fun clear() = rawMap.clear()
        }

        private class AndroidXMap : BenchmarkableLongMap<MutableLongIntMap> {

            override val rawMap: MutableLongIntMap = MutableLongIntMap()

            override val size: Int get() = rawMap.size

            override fun newInstance(): BenchmarkableLongMap<MutableLongIntMap> = AndroidXMap()
            override fun forEach(action: (Long, Int) -> Unit) = rawMap.forEach { key, value -> action(key, value) }
            override fun iterate(action: (Long, Int) -> Unit) = throw UnsupportedOperationException()
            override fun get(key: Long): Int = rawMap.getOrDefault(key, 0)
            override fun put(key: Long, value: Int) { rawMap[key] = value }
            override fun remove(key: Long) { rawMap.remove(key) }
            override fun putAll(otherMap: BenchmarkableLongMap<MutableLongIntMap>) = rawMap.putAll(otherMap.rawMap)
            override fun clear() = rawMap.clear()
        }

        private class TroveMap : BenchmarkableLongMap<TLongIntHashMap> {

            override val rawMap: TLongIntHashMap = TLongIntHashMap(Constants.DEFAULT_CAPACITY, 0.75f, 0, 0)

            override val size: Int get() = rawMap.size()

            override fun newInstance(): BenchmarkableLongMap<TLongIntHashMap> = TroveMap()
            override fun ensureCapacity(capacity: Int) = rawMap.ensureCapacity(capacity)
            override fun forEach(action: (Long, Int) -> Unit) { rawMap.forEachEntry { key, value -> action(key, value); return@forEachEntry true } }
            override fun iterate(action: (Long, Int) -> Unit) { throw UnsupportedOperationException() }
            override fun get(key: Long): Int = rawMap.get(key)
            override fun put(key: Long, value: Int) { rawMap.put(key, value) }
            override fun remove(key: Long) { rawMap.remove(key) }
            override fun putAll(otherMap: BenchmarkableLongMap<TLongIntHashMap>) = rawMap.putAll(otherMap.rawMap)
            override fun clear() = rawMap.clear()
        }

        private class KolobokeMap : BenchmarkableLongMap<HashLongIntMap> {

            override val rawMap: HashLongIntMap = HashLongIntMaps.getDefaultFactory().withHashConfig(HashConfig.fromLoads(.75/2, .75, .75)).newMutableMap()

            override val size: Int get() = rawMap.size

            override fun newInstance(): BenchmarkableLongMap<HashLongIntMap> = KolobokeMap()
            override fun ensureCapacity(capacity: Int) { rawMap.ensureCapacity(capacity.toLong()) }
            override fun forEach(action: (Long, Int) -> Unit) = rawMap.forEach(LongIntConsumer { key, value -> action(key, value) } )
            override fun iterate(action: (Long, Int) -> Unit) { for ((key, value) in rawMap) action(key, value) }
            override fun get(key: Long): Int = rawMap.getOrDefault(key, 0)
            override fun put(key: Long, value: Int) { rawMap.put(key, value)}
            override fun remove(key: Long) { rawMap.remove(key) }
            override fun putAll(otherMap: BenchmarkableLongMap<HashLongIntMap>) = rawMap.putAll(otherMap.rawMap)
            override fun clear() = rawMap.clear()
        }

        private class EclipseMap : BenchmarkableLongMap<LongIntHashMap> {

            override val rawMap: LongIntHashMap = LongIntHashMap()

            override val size: Int get() = rawMap.size()

            override fun newInstance(): BenchmarkableLongMap<LongIntHashMap> = EclipseMap()
            override fun ensureCapacity(capacity: Int) {}
            override fun forEach(action: (Long, Int) -> Unit) = rawMap.forEachKeyValue { key, value -> action(key, value) }
            override fun iterate(action: (Long, Int) -> Unit) = throw UnsupportedOperationException()
            override fun get(key: Long): Int = rawMap.getIfAbsent(key, 0)
            override fun put(key: Long, value: Int) { rawMap.put(key, value) }
            override fun remove(key: Long) { rawMap.remove(key) }
            override fun putAll(otherMap: BenchmarkableLongMap<LongIntHashMap>) = rawMap.putAll(otherMap.rawMap)
            override fun clear() = rawMap.clear()
        }
    }

    private class HPPCMap : BenchmarkableLongMap<com.carrotsearch.hppc.LongIntHashMap> {

        override val rawMap: com.carrotsearch.hppc.LongIntHashMap = com.carrotsearch.hppc.LongIntHashMap()

        override val size: Int get() = rawMap.size()

        override fun newInstance(): BenchmarkableLongMap<com.carrotsearch.hppc.LongIntHashMap> = HPPCMap()
        override fun ensureCapacity(capacity: Int) { rawMap.ensureCapacity(capacity) }
        override fun forEach(action: (Long, Int) -> Unit) { rawMap.forEach (LongIntProcedure { key, value -> action(key, value) }) }
        override fun iterate(action: (Long, Int) -> Unit) { for (entry in rawMap) action(entry.key, entry.value) }
        override fun get(key: Long): Int = rawMap.getOrDefault(key, 0)
        override fun put(key: Long, value: Int) { rawMap.put(key, value) }
        override fun remove(key: Long) { rawMap.remove(key) }
        override fun putAll(otherMap: BenchmarkableLongMap<com.carrotsearch.hppc.LongIntHashMap>) { rawMap.putAll(otherMap.rawMap) }
        override fun clear() = rawMap.clear()
    }

    private class AgronaMap : BenchmarkableLongMap<org.agrona.collections.Long2LongHashMap> {

        override val rawMap: org.agrona.collections.Long2LongHashMap = org.agrona.collections.Long2LongHashMap(8, .75f, Long.MAX_VALUE)

        override val size: Int get() = rawMap.size

        override fun newInstance(): BenchmarkableLongMap<org.agrona.collections.Long2LongHashMap> = AgronaMap()
        override fun forEach(action: (Long, Int) -> Unit) = rawMap.forEachLong { key, value -> action(key, value.toInt()) }
        override fun iterate(action: (Long, Int) -> Unit) { for (entry in rawMap) action(entry.key, entry.value.toInt()) }
        override fun get(key: Long): Int = rawMap.getOrDefault(key, 0).toInt()
        override fun put(key: Long, value: Int) { rawMap.put(key, value.toLong()) }
        override fun remove(key: Long) { rawMap.remove(key) }
        override fun putAll(otherMap: BenchmarkableLongMap<org.agrona.collections.Long2LongHashMap>) { rawMap.putAll(otherMap.rawMap) }
        override fun clear() = rawMap.clear()
    }
}
