package io.github.sooniln.fastcollect

import androidx.collection.MutableIntIntMap
import com.koloboke.collect.hash.HashConfig
import com.koloboke.collect.map.hash.HashIntIntMap
import com.koloboke.collect.map.hash.HashIntIntMaps
import com.koloboke.function.IntIntConsumer
import gnu.trove.impl.Constants
import gnu.trove.map.hash.TIntIntHashMap
import io.github.sooniln.fastcollect.ints.Int2IntHashMap
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap

interface BenchmarkableIntMap<T> {
    val map: T

    fun newInstance(): BenchmarkableIntMap<T>
    fun copyInstance(): BenchmarkableIntMap<T> {
        val copy = newInstance()
        copy.putAll(this)
        return copy
    }

    fun forEach(action: (Int, Int) -> Unit)
    fun get(key: Int): Int
    fun put(key: Int, value: Int)
    fun remove(key: Int)
    fun putAll(otherMap: BenchmarkableIntMap<T>)
    fun clear()

    companion object {
        fun from(type: String): BenchmarkableIntMap<*> {
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

        private class JreMap : BenchmarkableIntMap<HashMap<Int, Int>> {

            override val map: HashMap<Int, Int> = HashMap()

            override fun newInstance(): BenchmarkableIntMap<HashMap<Int, Int>> = JreMap()

            override fun forEach(action: (Int, Int) -> Unit) {
                for ((key, value) in map) {
                    action(key, value)
                }
            }

            override fun get(key: Int): Int = map.getOrDefault(key, 0)

            override fun put(key: Int, value: Int) {
                map[key] = value
            }

            override fun remove(key: Int) {
                map.remove(key)
            }

            override fun putAll(otherMap: BenchmarkableIntMap<HashMap<Int, Int>>) = map.putAll(otherMap.map)
            override fun clear() = map.clear()
        }

        private class FastCollectMap : BenchmarkableIntMap<Int2IntHashMap> {

            override val map: Int2IntHashMap = Int2IntHashMap(defaultValue = 0)

            override fun newInstance(): BenchmarkableIntMap<Int2IntHashMap> = FastCollectMap()

            override fun forEach(action: (Int, Int) -> Unit) {
                for (entry in map.fastIterator()) {
                    action(entry.key(), entry.value())
                }
            }

            override fun get(key: Int): Int = map.lookup(key)

            override fun put(key: Int, value: Int) {
                map[key] = value
            }

            override fun remove(key: Int) {
                map.removeKey(key)
            }

            override fun putAll(otherMap: BenchmarkableIntMap<Int2IntHashMap>) = map.putAll(otherMap.map)
            override fun clear() = map.clear()
        }

        private class FastutilMap : BenchmarkableIntMap<Int2IntOpenHashMap> {

            override val map: Int2IntOpenHashMap = Int2IntOpenHashMap()

            init {
                map.defaultReturnValue(0)
            }

            override fun newInstance(): BenchmarkableIntMap<Int2IntOpenHashMap> = FastutilMap()

            override fun forEach(action: (Int, Int) -> Unit) {
                for (entry in map.int2IntEntrySet().fastIterator()) {
                    action(entry.intKey, entry.intValue)
                }
            }

            override fun get(key: Int): Int = map.get(key)

            override fun put(key: Int, value: Int) {
                map[key] = value
            }

            override fun remove(key: Int) {
                map.remove(key)
            }

            override fun putAll(otherMap: BenchmarkableIntMap<Int2IntOpenHashMap>) = map.putAll(otherMap.map)
            override fun clear() = map.clear()
        }

        private class AndroidXMap : BenchmarkableIntMap<MutableIntIntMap> {

            override val map: MutableIntIntMap = MutableIntIntMap()

            override fun newInstance(): BenchmarkableIntMap<MutableIntIntMap> = AndroidXMap()

            override fun forEach(action: (Int, Int) -> Unit) {
                map.forEach { key, value -> action(key, value) }
            }

            override fun get(key: Int): Int = map.getOrDefault(key, 0)

            override fun put(key: Int, value: Int) {
                map[key] = value
            }

            override fun remove(key: Int) {
                map.remove(key)
            }

            override fun putAll(otherMap: BenchmarkableIntMap<MutableIntIntMap>) = map.putAll(otherMap.map)
            override fun clear() = map.clear()
        }

        private class TroveMap : BenchmarkableIntMap<TIntIntHashMap> {

            override val map: TIntIntHashMap = TIntIntHashMap(Constants.DEFAULT_CAPACITY, 0.75f)

            override fun newInstance(): BenchmarkableIntMap<TIntIntHashMap> = TroveMap()

            override fun forEach(action: (Int, Int) -> Unit) {
                val it = map.iterator()
                while (it.hasNext()) {
                    it.advance()
                    action(it.key(), it.value())
                }
            }

            override fun get(key: Int): Int = map.get(key)

            override fun put(key: Int, value: Int) {
                map.put(key, value)
            }

            override fun remove(key: Int) {
                map.remove(key)
            }

            override fun putAll(otherMap: BenchmarkableIntMap<TIntIntHashMap>) = map.putAll(otherMap.map)
            override fun clear() = map.clear()
        }

        private class KolobokeMap : BenchmarkableIntMap<HashIntIntMap> {

            override val map: HashIntIntMap = HashIntIntMaps.getDefaultFactory().withHashConfig(HashConfig.fromLoads(0.0, .75, .75)).newMutableMap()

            override fun newInstance(): BenchmarkableIntMap<HashIntIntMap> = KolobokeMap()

            override fun forEach(action: (Int, Int) -> Unit) {
                map.forEach(IntIntConsumer { key: Int, value: Int -> action(key, value) })
            }

            override fun get(key: Int): Int = map.get(key)

            override fun put(key: Int, value: Int) {
                map.put(key, value)
            }

            override fun remove(key: Int) {
                map.remove(key)
            }

            override fun putAll(otherMap: BenchmarkableIntMap<HashIntIntMap>) = map.putAll(otherMap.map)
            override fun clear() = map.clear()
        }
    }
}
