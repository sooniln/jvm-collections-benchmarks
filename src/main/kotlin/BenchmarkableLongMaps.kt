package io.github.sooniln.jvmcollectionsbenchmark

import androidx.collection.MutableLongLongMap
import com.koloboke.collect.hash.HashConfig
import com.koloboke.collect.map.hash.HashLongLongMap
import com.koloboke.collect.map.hash.HashLongLongMaps
import com.koloboke.function.LongLongConsumer
import gnu.trove.impl.Constants
import gnu.trove.map.hash.TLongLongHashMap
import io.github.sooniln.fastcollect.longs.Long2LongHashMap
import io.github.sooniln.fastcollect.longs.getOrDefault
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap
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
    fun forEach(action: (Long, Long) -> Unit) = iterate(action)
    fun iterate(action: (Long, Long) -> Unit)
    fun get(key: Long): Long
    fun put(key: Long, value: Long)
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
        )

        fun from(type: String): BenchmarkableLongMap<*> = map.getOrElse(type) { throw IllegalArgumentException("Unknown type: $type") }.invoke()

        fun all(): Sequence<Pair<String, BenchmarkableLongMap<*>>> = map.entries.asSequence().map { it.key to it.value.invoke() }

        private class JreMap : BenchmarkableLongMap<HashMap<Long, Long>> {

            override val rawMap: HashMap<Long, Long> = HashMap()

            override val size: Int get() = rawMap.size

            override fun newInstance(): BenchmarkableLongMap<HashMap<Long, Long>> = JreMap()
            override fun iterate(action: (Long, Long) -> Unit) { for ((key, value) in rawMap) action(key, value) }
            override fun get(key: Long): Long = rawMap.getOrDefault(key, 0)
            override fun put(key: Long, value: Long) { rawMap[key] = value }
            override fun remove(key: Long) { rawMap.remove(key) }
            override fun putAll(otherMap: BenchmarkableLongMap<HashMap<Long, Long>>) = rawMap.putAll(otherMap.rawMap)
            override fun clear() = rawMap.clear()
        }

        private class FastCollectMap : BenchmarkableLongMap<Long2LongHashMap> {

            override val rawMap: Long2LongHashMap = Long2LongHashMap()

            override val size: Int get() = rawMap.size

            override fun newInstance(): BenchmarkableLongMap<Long2LongHashMap> = FastCollectMap()
            override fun ensureCapacity(capacity: Int) = rawMap.ensureCapacity(capacity)
            override fun iterate(action: (Long, Long) -> Unit) { for ((key, value) in rawMap) action(key, value) }
            override fun get(key: Long): Long = rawMap.getOrDefault(key, 0)
            override fun put(key: Long, value: Long) { rawMap[key] = value }
            override fun remove(key: Long) { rawMap.remove(key) }
            override fun putAll(otherMap: BenchmarkableLongMap<Long2LongHashMap>) = rawMap.putAll(otherMap.rawMap)
            override fun clear() = rawMap.clear()
        }

        private class FastutilMap : BenchmarkableLongMap<Long2LongOpenHashMap> {

            override val rawMap: Long2LongOpenHashMap = Long2LongOpenHashMap()

            override val size: Int get() = rawMap.size

            override fun newInstance(): BenchmarkableLongMap<Long2LongOpenHashMap> = FastutilMap()
            override fun ensureCapacity(capacity: Int) = rawMap.ensureCapacity(capacity)
            @Suppress("JavaMapForEach")
            override fun forEach(action: (Long, Long) -> Unit) = rawMap.forEach { key, value -> action(key, value) }
            override fun iterate(action: (Long, Long) -> Unit) { for ((key, value) in rawMap) action(key, value) }
            override fun get(key: Long): Long = rawMap.getOrDefault(key, 0)
            override fun put(key: Long, value: Long) { rawMap[key] = value }
            override fun remove(key: Long) { rawMap.remove(key) }
            override fun putAll(otherMap: BenchmarkableLongMap<Long2LongOpenHashMap>) = rawMap.putAll(otherMap.rawMap)
            override fun clear() = rawMap.clear()
        }

        private class AndroidXMap : BenchmarkableLongMap<MutableLongLongMap> {

            override val rawMap: MutableLongLongMap = MutableLongLongMap()

            override val size: Int get() = rawMap.size

            override fun newInstance(): BenchmarkableLongMap<MutableLongLongMap> = AndroidXMap()
            override fun forEach(action: (Long, Long) -> Unit) = rawMap.forEach { key, value -> action(key, value) }
            override fun iterate(action: (Long, Long) -> Unit) = throw UnsupportedOperationException()
            override fun get(key: Long): Long = rawMap.getOrDefault(key, 0)
            override fun put(key: Long, value: Long) { rawMap[key] = value }
            override fun remove(key: Long) { rawMap.remove(key) }
            override fun putAll(otherMap: BenchmarkableLongMap<MutableLongLongMap>) = rawMap.putAll(otherMap.rawMap)
            override fun clear() = rawMap.clear()
        }

        private class TroveMap : BenchmarkableLongMap<TLongLongHashMap> {

            override val rawMap: TLongLongHashMap = TLongLongHashMap(Constants.DEFAULT_CAPACITY, 0.75f)

            override val size: Int get() = rawMap.size()

            override fun newInstance(): BenchmarkableLongMap<TLongLongHashMap> = TroveMap()
            override fun ensureCapacity(capacity: Int) = rawMap.ensureCapacity(capacity)
            override fun forEach(action: (Long, Long) -> Unit) { rawMap.forEachEntry { key, value -> action(key, value); return@forEachEntry true } }
            override fun iterate(action: (Long, Long) -> Unit) { throw UnsupportedOperationException() }
            override fun get(key: Long): Long = rawMap.get(key)
            override fun put(key: Long, value: Long) { rawMap.put(key, value) }
            override fun remove(key: Long) { rawMap.remove(key) }
            override fun putAll(otherMap: BenchmarkableLongMap<TLongLongHashMap>) = rawMap.putAll(otherMap.rawMap)
            override fun clear() = rawMap.clear()
        }

        private class KolobokeMap : BenchmarkableLongMap<HashLongLongMap> {

            override val rawMap: HashLongLongMap = HashLongLongMaps.getDefaultFactory().withHashConfig(HashConfig.fromLoads(.75/2, .75, .75)).newMutableMap()

            override val size: Int get() = rawMap.size

            override fun newInstance(): BenchmarkableLongMap<HashLongLongMap> = KolobokeMap()
            override fun ensureCapacity(capacity: Int) { rawMap.ensureCapacity(capacity.toLong()) }
            override fun forEach(action: (Long, Long) -> Unit) = rawMap.forEach(LongLongConsumer { key, value -> action(key, value) } )
            override fun iterate(action: (Long, Long) -> Unit) { for ((key, value) in rawMap) action(key, value) }
            override fun get(key: Long): Long = rawMap.getOrDefault(key, 0)
            override fun put(key: Long, value: Long) { rawMap.put(key, value)}
            override fun remove(key: Long) { rawMap.remove(key) }
            override fun putAll(otherMap: BenchmarkableLongMap<HashLongLongMap>) = rawMap.putAll(otherMap.rawMap)
            override fun clear() = rawMap.clear()
        }
    }
}
