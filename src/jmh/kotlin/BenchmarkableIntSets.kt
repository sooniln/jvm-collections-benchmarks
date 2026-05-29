package io.github.sooniln.fastcollect

import androidx.collection.MutableIntSet
import com.koloboke.collect.hash.HashConfig
import com.koloboke.collect.set.hash.HashIntSet
import com.koloboke.collect.set.hash.HashIntSets
import gnu.trove.impl.Constants
import gnu.trove.set.hash.TIntHashSet
import io.github.sooniln.fastcollect.ints.IntHashSet
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import java.util.function.IntConsumer

interface BenchmarkableIntSet<T> {
    val set: T

    fun newInstance(): BenchmarkableIntSet<T>
    fun copyInstance(): BenchmarkableIntSet<T> {
        val copy = newInstance()
        copy.addAll(this)
        return copy
    }

    fun forEach(action: (Int) -> Unit)
    fun contains(key: Int): Boolean
    fun add(key: Int): Boolean
    fun remove(key: Int): Boolean
    fun addAll(otherSet: BenchmarkableIntSet<T>): Boolean
    fun clear()

    companion object {
        fun from(type: String): BenchmarkableIntSet<*> {
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

        private class JreSet : BenchmarkableIntSet<HashSet<Int>> {

            override val set: HashSet<Int> = HashSet()

            override fun newInstance(): BenchmarkableIntSet<HashSet<Int>> = JreSet()

            override fun forEach(action: (Int) -> Unit) {
                for (key in set) {
                    action(key)
                }
            }

            override fun contains(key: Int) = set.contains(key)
            override fun add(key: Int) = set.add(key)
            override fun remove(key: Int) = set.remove(key)
            override fun addAll(otherSet: BenchmarkableIntSet<HashSet<Int>>) = set.addAll(otherSet.set)
            override fun clear() = set.clear()
        }

        private class FastCollectSet : BenchmarkableIntSet<IntHashSet> {

            override val set: IntHashSet = IntHashSet()

            override fun newInstance(): BenchmarkableIntSet<IntHashSet> = FastCollectSet()

            override fun forEach(action: (Int) -> Unit) {
                for (key in set) {
                    action(key)
                }
            }

            override fun contains(key: Int) = set.contains(key)
            override fun add(key: Int) = set.add(key)
            override fun remove(key: Int) = set.remove(key)
            override fun addAll(otherSet: BenchmarkableIntSet<IntHashSet>) = set.addAll(otherSet.set)
            override fun clear() = set.clear()
        }

        private class FastutilSet : BenchmarkableIntSet<IntOpenHashSet> {

            override val set: IntOpenHashSet = IntOpenHashSet()

            override fun newInstance(): BenchmarkableIntSet<IntOpenHashSet> = FastutilSet()

            override fun forEach(action: (Int) -> Unit) {
                val it = set.iterator()
                while (it.hasNext()) {
                    action(it.nextInt())
                }
            }

            override fun contains(key: Int) = set.contains(key)
            override fun add(key: Int) = set.add(key)
            override fun remove(key: Int) = set.remove(key)
            override fun addAll(otherSet: BenchmarkableIntSet<IntOpenHashSet>) = set.addAll(otherSet.set)
            override fun clear() = set.clear()
        }

        private class AndroidXSet : BenchmarkableIntSet<MutableIntSet> {

            override val set: MutableIntSet = MutableIntSet()

            override fun newInstance(): BenchmarkableIntSet<MutableIntSet> = AndroidXSet()

            override fun forEach(action: (Int) -> Unit) {
                set.forEach { action(it) }
            }

            override fun contains(key: Int) = set.contains(key)
            override fun add(key: Int) = set.add(key)
            override fun remove(key: Int) = set.remove(key)
            override fun addAll(otherSet: BenchmarkableIntSet<MutableIntSet>) = set.addAll(otherSet.set)
            override fun clear() = set.clear()
        }

        private class TroveSet : BenchmarkableIntSet<TIntHashSet> {

            override val set: TIntHashSet = TIntHashSet(Constants.DEFAULT_CAPACITY, 0.75f)

            override fun newInstance(): BenchmarkableIntSet<TIntHashSet> = TroveSet()

            override fun forEach(action: (Int) -> Unit) {
                set.forEach { action(it); return@forEach true }
            }

            override fun contains(key: Int) = set.contains(key)
            override fun add(key: Int) = set.add(key)
            override fun remove(key: Int) = set.remove(key)
            override fun addAll(otherSet: BenchmarkableIntSet<TIntHashSet>) = set.addAll(otherSet.set)
            override fun clear() = set.clear()
        }

        private class KolobokeSet : BenchmarkableIntSet<HashIntSet> {

            override val set: HashIntSet = HashIntSets.getDefaultFactory().withHashConfig(HashConfig.fromLoads(0.0, .75, .75)).newMutableSet()

            override fun newInstance(): BenchmarkableIntSet<HashIntSet> = KolobokeSet()

            override fun forEach(action: (Int) -> Unit) {
                set.forEach(IntConsumer { action(it) })
            }

            override fun contains(key: Int) = set.contains(key)
            override fun add(key: Int): Boolean = set.add(key)
            override fun remove(key: Int) = set.removeInt(key)
            override fun addAll(otherSet: BenchmarkableIntSet<HashIntSet>) = set.addAll(otherSet.set)
            override fun clear() = set.clear()
        }
    }
}
