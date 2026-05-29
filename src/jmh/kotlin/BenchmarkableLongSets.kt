package io.github.sooniln.fastcollect

import androidx.collection.MutableLongSet
import com.koloboke.collect.hash.HashConfig
import com.koloboke.collect.set.hash.HashLongSet
import com.koloboke.collect.set.hash.HashLongSets
import gnu.trove.impl.Constants
import gnu.trove.set.hash.TLongHashSet
import io.github.sooniln.fastcollect.longs.LongHashSet
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import java.util.function.LongConsumer

interface BenchmarkableLongSet<T> {
    val set: T

    fun newInstance(): BenchmarkableLongSet<T>
    fun copyInstance(): BenchmarkableLongSet<T> {
        val copy = newInstance()
        copy.addAll(this)
        return copy
    }

    fun forEach(action: (Long) -> Unit)
    fun contains(key: Long): Boolean
    fun add(key: Long): Boolean
    fun remove(key: Long): Boolean
    fun addAll(otherSet: BenchmarkableLongSet<T>): Boolean
    fun clear()

    companion object {
        fun from(type: String): BenchmarkableLongSet<*> {
            return when (type) {
                "JRE" -> JreSet()
                "FastCollect" -> FastCollectSet()
                "Fastutil" -> FastutilSet()
                "AndroidX" -> AndroidXSet()
                "Trove" -> TroveSet()
                "Koloboke" -> KolobokeSet()
                else -> throw IllegalArgumentException("Unknown type: $type")
            }
        }

        private class JreSet : BenchmarkableLongSet<HashSet<Long>> {

            override val set: HashSet<Long> = HashSet()

            override fun newInstance(): BenchmarkableLongSet<HashSet<Long>> = JreSet()

            override fun forEach(action: (Long) -> Unit) {
                for (key in set) {
                    action(key)
                }
            }

            override fun contains(key: Long) = set.contains(key)
            override fun add(key: Long) = set.add(key)
            override fun remove(key: Long) = set.remove(key)
            override fun addAll(otherSet: BenchmarkableLongSet<HashSet<Long>>) = set.addAll(otherSet.set)
            override fun clear() = set.clear()
        }

        private class FastCollectSet : BenchmarkableLongSet<LongHashSet> {

            override val set: LongHashSet = LongHashSet()

            override fun newInstance(): BenchmarkableLongSet<LongHashSet> = FastCollectSet()

            override fun forEach(action: (Long) -> Unit) {
                for (key in set) {
                    action(key)
                }
            }

            override fun contains(key: Long) = set.contains(key)
            override fun add(key: Long) = set.add(key)
            override fun remove(key: Long) = set.remove(key)
            override fun addAll(otherSet: BenchmarkableLongSet<LongHashSet>) = set.addAll(otherSet.set)
            override fun clear() = set.clear()
        }

        private class FastutilSet : BenchmarkableLongSet<LongOpenHashSet> {

            override val set: LongOpenHashSet = LongOpenHashSet()

            override fun newInstance(): BenchmarkableLongSet<LongOpenHashSet> = FastutilSet()

            override fun forEach(action: (Long) -> Unit) {
                val it = set.iterator()
                while (it.hasNext()) {
                    action(it.nextLong())
                }
            }

            override fun contains(key: Long) = set.contains(key)
            override fun add(key: Long) = set.add(key)
            override fun remove(key: Long) = set.remove(key)
            override fun addAll(otherSet: BenchmarkableLongSet<LongOpenHashSet>) = set.addAll(otherSet.set)
            override fun clear() = set.clear()
        }

        private class AndroidXSet : BenchmarkableLongSet<MutableLongSet> {

            override val set: MutableLongSet = MutableLongSet()

            override fun newInstance(): BenchmarkableLongSet<MutableLongSet> = AndroidXSet()

            override fun forEach(action: (Long) -> Unit) {
                set.forEach { action(it) }
            }

            override fun contains(key: Long) = set.contains(key)
            override fun add(key: Long) = set.add(key)
            override fun remove(key: Long) = set.remove(key)
            override fun addAll(otherSet: BenchmarkableLongSet<MutableLongSet>) = set.addAll(otherSet.set)
            override fun clear() = set.clear()
        }

        private class TroveSet : BenchmarkableLongSet<TLongHashSet> {

            override val set: TLongHashSet = TLongHashSet(Constants.DEFAULT_CAPACITY, 0.75f)

            override fun newInstance(): BenchmarkableLongSet<TLongHashSet> = TroveSet()

            override fun forEach(action: (Long) -> Unit) {
                set.forEach { action(it); return@forEach true }
            }

            override fun contains(key: Long) = set.contains(key)
            override fun add(key: Long) = set.add(key)
            override fun remove(key: Long) = set.remove(key)
            override fun addAll(otherSet: BenchmarkableLongSet<TLongHashSet>) = set.addAll(otherSet.set)
            override fun clear() = set.clear()
        }

        private class KolobokeSet : BenchmarkableLongSet<HashLongSet> {

            override val set: HashLongSet = HashLongSets.getDefaultFactory().withHashConfig(HashConfig.fromLoads(0.0, .75, .75)).newMutableSet()

            override fun newInstance(): BenchmarkableLongSet<HashLongSet> = KolobokeSet()

            override fun forEach(action: (Long) -> Unit) {
                set.forEach(LongConsumer { action(it) })
            }

            override fun contains(key: Long) = set.contains(key)
            override fun add(key: Long): Boolean = set.add(key)
            override fun remove(key: Long) = set.removeLong(key)
            override fun addAll(otherSet: BenchmarkableLongSet<HashLongSet>) = set.addAll(otherSet.set)
            override fun clear() = set.clear()
        }
    }
}
