package io.github.sooniln.jvmcollectionsbenchmark

import androidx.collection.MutableIntIntMap
import com.carrotsearch.hppc.procedures.IntIntProcedure
import com.koloboke.collect.hash.HashConfig
import com.koloboke.collect.map.hash.HashIntIntMap
import com.koloboke.collect.map.hash.HashIntIntMaps
import com.koloboke.function.IntIntConsumer
import gnu.trove.impl.Constants
import gnu.trove.map.hash.TIntIntHashMap
import io.github.sooniln.fastcollect.ints.Int2IntHashMap
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import org.eclipse.collections.impl.map.mutable.primitive.IntIntHashMap

interface BenchmarkableIntMap<T> {
    val rawMap: T

    val size: Int

    fun newInstance(): BenchmarkableIntMap<T>
    fun copyInstance(): BenchmarkableIntMap<T> {
        val copy = newInstance()
        copy.putAll(this)
        return copy
    }

    fun ensureCapacity(capacity: Int) {}
    fun forEach(action: (Int, Int) -> Unit) = iterate(action)
    fun iterate(action: (Int, Int) -> Unit)
    fun get(key: Int): Int
    fun put(key: Int, value: Int)
    fun remove(key: Int)
    fun putAll(otherMap: BenchmarkableIntMap<T>)
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

        fun from(type: String): BenchmarkableIntMap<*> = map.getOrElse(type) { throw IllegalArgumentException("Unknown type: $type") }.invoke()

        fun all(): Sequence<Pair<String, BenchmarkableIntMap<*>>> = map.entries.asSequence().map { it.key to it.value.invoke() }

        private class JreMap : BenchmarkableIntMap<HashMap<Int, Int>> {

            override val rawMap: HashMap<Int, Int> = HashMap()

            override val size: Int get() = rawMap.size

            override fun newInstance(): BenchmarkableIntMap<HashMap<Int, Int>> = JreMap()
            override fun iterate(action: (Int, Int) -> Unit) { for ((key, value) in rawMap) action(key, value) }
            override fun get(key: Int): Int = rawMap.getOrDefault(key, 0)
            override fun put(key: Int, value: Int) { rawMap[key] = value }
            override fun remove(key: Int) { rawMap.remove(key) }
            override fun putAll(otherMap: BenchmarkableIntMap<HashMap<Int, Int>>) = rawMap.putAll(otherMap.rawMap)
            override fun clear() = rawMap.clear()
        }

        private class FastCollectMap : BenchmarkableIntMap<Int2IntHashMap> {

            override val rawMap: Int2IntHashMap = Int2IntHashMap()

            override val size: Int get() = rawMap.size

            override fun newInstance(): BenchmarkableIntMap<Int2IntHashMap> = FastCollectMap()
            override fun ensureCapacity(capacity: Int) = rawMap.ensureCapacity(capacity)
            override fun forEach(action: (Int, Int) -> Unit) = rawMap.forEach { key, value -> action(key, value) }
            override fun iterate(action: (Int, Int) -> Unit) { for ((key, value) in rawMap) action(key, value) }
            override fun get(key: Int): Int = rawMap.getOrDefault(key, 0)
            override fun put(key: Int, value: Int) { rawMap[key] = value }
            override fun remove(key: Int) { rawMap.remove(key) }
            override fun putAll(otherMap: BenchmarkableIntMap<Int2IntHashMap>) = rawMap.putAll(otherMap.rawMap)
            override fun clear() = rawMap.clear()
        }

        private class FastutilMap : BenchmarkableIntMap<Int2IntOpenHashMap> {

            override val rawMap: Int2IntOpenHashMap = Int2IntOpenHashMap()

            override val size: Int get() = rawMap.size

            override fun newInstance(): BenchmarkableIntMap<Int2IntOpenHashMap> = FastutilMap()
            override fun ensureCapacity(capacity: Int) = rawMap.ensureCapacity(capacity)
            @Suppress("JavaMapForEach")
            override fun forEach(action: (Int, Int) -> Unit) = rawMap.forEach { key, value -> action(key, value) }
            override fun iterate(action: (Int, Int) -> Unit) { for ((key, value) in rawMap) action(key, value) }
            override fun get(key: Int): Int = rawMap.getOrDefault(key, 0)
            override fun put(key: Int, value: Int) { rawMap.put(key, value) }
            override fun remove(key: Int) { rawMap.remove(key) }
            override fun putAll(otherMap: BenchmarkableIntMap<Int2IntOpenHashMap>) = rawMap.putAll(otherMap.rawMap)
            override fun clear() = rawMap.clear()
        }

        private class AndroidXMap : BenchmarkableIntMap<MutableIntIntMap> {

            override val rawMap: MutableIntIntMap = MutableIntIntMap()

            override val size: Int get() = rawMap.size

            override fun newInstance(): BenchmarkableIntMap<MutableIntIntMap> = AndroidXMap()
            override fun forEach(action: (Int, Int) -> Unit) = rawMap.forEach { key, value -> action(key, value) }
            override fun iterate(action: (Int, Int) -> Unit) = throw UnsupportedOperationException()
            override fun get(key: Int): Int = rawMap.getOrDefault(key, 0)
            override fun put(key: Int, value: Int) { rawMap[key] = value }
            override fun remove(key: Int) { rawMap.remove(key) }
            override fun putAll(otherMap: BenchmarkableIntMap<MutableIntIntMap>) = rawMap.putAll(otherMap.rawMap)
            override fun clear() = rawMap.clear()
        }

        private class TroveMap : BenchmarkableIntMap<TIntIntHashMap> {

            override val rawMap: TIntIntHashMap = TIntIntHashMap(Constants.DEFAULT_CAPACITY, 0.75f, 0, 0)

            override val size: Int get() = rawMap.size()

            override fun newInstance(): BenchmarkableIntMap<TIntIntHashMap> = TroveMap()
            override fun ensureCapacity(capacity: Int) = rawMap.ensureCapacity(capacity)
            override fun forEach(action: (Int, Int) -> Unit) { rawMap.forEachEntry { key, value -> action(key, value); return@forEachEntry true } }
            override fun iterate(action: (Int, Int) -> Unit) { throw UnsupportedOperationException() }
            override fun get(key: Int): Int = rawMap.get(key)
            override fun put(key: Int, value: Int) { rawMap.put(key, value) }
            override fun remove(key: Int) { rawMap.remove(key) }
            override fun putAll(otherMap: BenchmarkableIntMap<TIntIntHashMap>) = rawMap.putAll(otherMap.rawMap)
            override fun clear() = rawMap.clear()
        }

        private class KolobokeMap : BenchmarkableIntMap<HashIntIntMap> {

            override val rawMap: HashIntIntMap = HashIntIntMaps.getDefaultFactory().withHashConfig(HashConfig.fromLoads(.75/2, .75, .75)).newMutableMap()

            override val size: Int get() = rawMap.size

            override fun newInstance(): BenchmarkableIntMap<HashIntIntMap> = KolobokeMap()
            override fun ensureCapacity(capacity: Int) { rawMap.ensureCapacity(capacity.toLong()) }
            override fun forEach(action: (Int, Int) -> Unit) = rawMap.forEach(IntIntConsumer { key, value -> action(key, value) } )
            override fun iterate(action: (Int, Int) -> Unit) { for ((key, value) in rawMap) action(key, value) }
            override fun get(key: Int): Int = rawMap.getOrDefault(key, 0)
            override fun put(key: Int, value: Int) { rawMap.put(key, value) }
            override fun remove(key: Int) { rawMap.remove(key) }
            override fun putAll(otherMap: BenchmarkableIntMap<HashIntIntMap>) = rawMap.putAll(otherMap.rawMap)
            override fun clear() = rawMap.clear()
        }

        private class EclipseMap : BenchmarkableIntMap<IntIntHashMap> {

            override val rawMap: IntIntHashMap = IntIntHashMap()

            override val size: Int get() = rawMap.size()

            override fun newInstance(): BenchmarkableIntMap<IntIntHashMap> = EclipseMap()
            override fun forEach(action: (Int, Int) -> Unit) = rawMap.forEachKeyValue { key, value -> action(key, value) }
            override fun iterate(action: (Int, Int) -> Unit) = throw UnsupportedOperationException()
            override fun get(key: Int): Int = rawMap.getIfAbsent(key, 0)
            override fun put(key: Int, value: Int) { rawMap.put(key, value) }
            override fun remove(key: Int) { rawMap.remove(key) }
            override fun putAll(otherMap: BenchmarkableIntMap<IntIntHashMap>) = rawMap.putAll(otherMap.rawMap)
            override fun clear() = rawMap.clear()
        }

        private class HPPCMap : BenchmarkableIntMap<com.carrotsearch.hppc.IntIntHashMap> {

            override val rawMap: com.carrotsearch.hppc.IntIntHashMap = com.carrotsearch.hppc.IntIntHashMap()

            override val size: Int get() = rawMap.size()

            override fun newInstance(): BenchmarkableIntMap<com.carrotsearch.hppc.IntIntHashMap> = HPPCMap()
            override fun ensureCapacity(capacity: Int) { rawMap.ensureCapacity(capacity) }
            override fun forEach(action: (Int, Int) -> Unit) { rawMap.forEach (IntIntProcedure { key, value -> action(key, value) }) }
            override fun iterate(action: (Int, Int) -> Unit) { for (entry in rawMap) action(entry.key, entry.value) }
            override fun get(key: Int): Int = rawMap.getOrDefault(key, 0)
            override fun put(key: Int, value: Int) { rawMap.put(key, value) }
            override fun remove(key: Int) { rawMap.remove(key) }
            override fun putAll(otherMap: BenchmarkableIntMap<com.carrotsearch.hppc.IntIntHashMap>) { rawMap.putAll(otherMap.rawMap) }
            override fun clear() = rawMap.clear()
        }

        private class AgronaMap : BenchmarkableIntMap<org.agrona.collections.Int2IntHashMap> {

            override val rawMap: org.agrona.collections.Int2IntHashMap = org.agrona.collections.Int2IntHashMap(8, .75f, Int.MAX_VALUE)

            override val size: Int get() = rawMap.size

            override fun newInstance(): BenchmarkableIntMap<org.agrona.collections.Int2IntHashMap> = AgronaMap()
            override fun forEach(action: (Int, Int) -> Unit) = rawMap.forEachInt { key, value -> action(key, value) }
            override fun iterate(action: (Int, Int) -> Unit) { for (entry in rawMap) action(entry.key, entry.value) }
            override fun get(key: Int): Int = rawMap.getOrDefault(key, 0)
            override fun put(key: Int, value: Int) { rawMap.put(key, value) }
            override fun remove(key: Int) { rawMap.remove(key) }
            override fun putAll(otherMap: BenchmarkableIntMap<org.agrona.collections.Int2IntHashMap>) { rawMap.putAll(otherMap.rawMap) }
            override fun clear() = rawMap.clear()
        }
    }
}
