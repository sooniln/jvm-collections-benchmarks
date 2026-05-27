package io.github.sooniln.fastcollect

import androidx.collection.MutableLongLongMap
import com.koloboke.collect.hash.HashConfig
import com.koloboke.collect.map.hash.HashLongLongMap
import com.koloboke.collect.map.hash.HashLongLongMaps
import com.koloboke.function.LongLongConsumer
import gnu.trove.impl.Constants
import gnu.trove.map.hash.TLongLongHashMap
import io.github.sooniln.fastcollect.longs.Long2LongHashMap
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

interface BenchmarkableLongMap<T> {
    val map: T

    fun newInstance(): BenchmarkableLongMap<T>
    fun copyInstance(): BenchmarkableLongMap<T> {
        val copy = newInstance()
        copy.putAll(this)
        return copy
    }

    fun forEach(action: (Long, Long) -> Unit)
    fun get(key: Long): Long
    fun put(key: Long, value: Long)
    fun remove(key: Long)
    fun putAll(otherMap: BenchmarkableLongMap<T>)
    fun clear()

    companion object {
        fun from(type: String): BenchmarkableLongMap<*> {
            return when (type) {
                "JRE" -> JreMap()
                "FastCollect" -> FastCollectMap()
                "Fastutil" -> FastutilMap()
                "AndroidX" -> AndroidXMap()
                "Trove" -> TroveMap()
                "Koloboke" -> KolobokeMap()
                else -> throw IllegalArgumentException("Unknown type: $type")
            }
        }

        private class JreMap : BenchmarkableLongMap<HashMap<Long, Long>> {

            override val map: HashMap<Long, Long> = HashMap()

            override fun newInstance(): BenchmarkableLongMap<HashMap<Long, Long>> = JreMap()

            override fun forEach(action: (Long, Long) -> Unit) {
                for ((key, value) in map) {
                    action(key, value)
                }
            }

            override fun get(key: Long): Long = map.getOrDefault(key, 0)

            override fun put(key: Long, value: Long) {
                map[key] = value
            }

            override fun remove(key: Long) {
                map.remove(key)
            }

            override fun putAll(otherMap: BenchmarkableLongMap<HashMap<Long, Long>>) = map.putAll(otherMap.map)
            override fun clear() = map.clear()
        }

        private class FastCollectMap : BenchmarkableLongMap<Long2LongHashMap> {

            override val map: Long2LongHashMap = Long2LongHashMap(defaultValue = 0)

            override fun newInstance(): BenchmarkableLongMap<Long2LongHashMap> = FastCollectMap()

            override fun forEach(action: (Long, Long) -> Unit) {
                for (entry in map.fastIterator()) {
                    action(entry.key(), entry.value())
                }
            }

            override fun get(key: Long): Long = map.lookup(key)

            override fun put(key: Long, value: Long) {
                map[key] = value
            }

            override fun remove(key: Long) {
                map.removeKey(key)
            }

            override fun putAll(otherMap: BenchmarkableLongMap<Long2LongHashMap>) = map.putAll(otherMap.map)
            override fun clear() = map.clear()
        }

        private class FastutilMap : BenchmarkableLongMap<Long2LongOpenHashMap> {

            override val map: Long2LongOpenHashMap = Long2LongOpenHashMap()

            init {
                map.defaultReturnValue(0)
            }

            override fun newInstance(): BenchmarkableLongMap<Long2LongOpenHashMap> = FastutilMap()

            override fun forEach(action: (Long, Long) -> Unit) {
                for (entry in map.long2LongEntrySet().fastIterator()) {
                    action(entry.longKey, entry.longValue)
                }
            }

            override fun get(key: Long): Long = map.get(key)

            override fun put(key: Long, value: Long) {
                map[key] = value
            }

            override fun remove(key: Long) {
                map.remove(key)
            }

            override fun putAll(otherMap: BenchmarkableLongMap<Long2LongOpenHashMap>) = map.putAll(otherMap.map)
            override fun clear() = map.clear()
        }

        private class AndroidXMap : BenchmarkableLongMap<MutableLongLongMap> {

            override val map: MutableLongLongMap = MutableLongLongMap()

            override fun newInstance(): BenchmarkableLongMap<MutableLongLongMap> = AndroidXMap()

            override fun forEach(action: (Long, Long) -> Unit) {
                map.forEach { key, value -> action(key, value) }
            }

            override fun get(key: Long): Long = map.getOrDefault(key, 0)

            override fun put(key: Long, value: Long) {
                map[key] = value
            }

            override fun remove(key: Long) {
                map.remove(key)
            }

            override fun putAll(otherMap: BenchmarkableLongMap<MutableLongLongMap>) = map.putAll(otherMap.map)
            override fun clear() = map.clear()
        }

        private class TroveMap : BenchmarkableLongMap<TLongLongHashMap> {

            override val map: TLongLongHashMap = TLongLongHashMap(Constants.DEFAULT_CAPACITY, 0.75f)

            override fun newInstance(): BenchmarkableLongMap<TLongLongHashMap> = TroveMap()

            override fun forEach(action: (Long, Long) -> Unit) {
                val it = map.iterator()
                while (it.hasNext()) {
                    it.advance()
                    action(it.key(), it.value())
                }
            }

            override fun get(key: Long): Long = map.get(key)

            override fun put(key: Long, value: Long) {
                map.put(key, value)
            }

            override fun remove(key: Long) {
                map.remove(key)
            }

            override fun putAll(otherMap: BenchmarkableLongMap<TLongLongHashMap>) = map.putAll(otherMap.map)
            override fun clear() = map.clear()
        }

        private class KolobokeMap : BenchmarkableLongMap<HashLongLongMap> {

            override val map: HashLongLongMap = HashLongLongMaps.getDefaultFactory().withHashConfig(HashConfig.fromLoads(0.0, .75, .75)).newMutableMap()

            override fun newInstance(): BenchmarkableLongMap<HashLongLongMap> = KolobokeMap()

            override fun forEach(action: (Long, Long) -> Unit) {
                map.forEach(LongLongConsumer { key: Long, value: Long -> action(key, value) })
            }

            override fun get(key: Long): Long = map.get(key)

            override fun put(key: Long, value: Long) {
                map.put(key, value)
            }

            override fun remove(key: Long) {
                map.remove(key)
            }

            override fun putAll(otherMap: BenchmarkableLongMap<HashLongLongMap>) = map.putAll(otherMap.map)
            override fun clear() = map.clear()
        }
    }
}
