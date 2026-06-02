package io.github.sooniln.jvmcollectionsbenchmark

import androidx.collection.MutableLongList
import gnu.trove.list.array.TLongArrayList
import io.github.sooniln.fastcollect.longs.LongArrayList
import java.util.function.LongConsumer

interface BenchmarkableLongList<T> {
    val rawList: T

    val size: Int

    fun newInstance(): BenchmarkableLongList<T>
    fun copyInstance(): BenchmarkableLongList<T> {
        val copy = newInstance()
        copy.addAll(this)
        return copy
    }

    fun ensureCapacity(capacity: Int)
    fun forEach(action: (Long) -> Unit) = iterate(action)
    fun iterate(action: (Long) -> Unit)
    fun indexOf(key: Long): Int
    fun add(key: Long): Boolean
    fun removeAt(index: Int): Long
    fun addAll(otherList: BenchmarkableLongList<T>): Boolean
    fun clear()

    companion object {
        private val map = mapOf(
            "JRE" to { JreList() },
            "FastCollect" to { FastCollectList() },
            "Fastutil" to { FastutilList() },
            "AndroidX" to { AndroidXList() },
            "Trove" to { TroveList() },
            "Eclipse" to { EclipseList() },
        )

        fun from(type: String): BenchmarkableLongList<*> = map.getOrElse(type) { throw IllegalArgumentException("Unknown type: $type") }.invoke()

        fun all(): Sequence<Pair<String, BenchmarkableLongList<*>>> = map.entries.asSequence().map { it.key to it.value.invoke() }

        private class JreList : BenchmarkableLongList<ArrayList<Long>> {

            override val rawList: ArrayList<Long> = ArrayList()

            override val size: Int get() = rawList.size

            override fun newInstance(): BenchmarkableLongList<ArrayList<Long>> = JreList()
            override fun ensureCapacity(capacity: Int) = rawList.ensureCapacity(capacity)
            override fun iterate(action: (Long) -> Unit) { for (key in rawList) action(key) }
            override fun indexOf(key: Long) = rawList.indexOf(key)
            override fun add(key: Long) = rawList.add(key)
            override fun removeAt(index: Int) = rawList.removeAt(index)
            override fun addAll(otherList: BenchmarkableLongList<ArrayList<Long>>) = rawList.addAll(otherList.rawList)
            override fun clear() = rawList.clear()
        }

        private class FastCollectList : BenchmarkableLongList<LongArrayList> {

            override val rawList: LongArrayList = LongArrayList()

            override val size: Int get() = rawList.size

            override fun newInstance(): BenchmarkableLongList<LongArrayList> = FastCollectList()
            override fun ensureCapacity(capacity: Int) = rawList.ensureCapacity(capacity)
            override fun iterate(action: (Long) -> Unit) { for (key in rawList) action(key) }
            override fun indexOf(key: Long) = rawList.indexOf(key)
            override fun add(key: Long) = rawList.add(key)
            override fun removeAt(index: Int) = rawList.removeAt(index)
            override fun addAll(otherList: BenchmarkableLongList<LongArrayList>) = rawList.addAll(otherList.rawList)
            override fun clear() = rawList.clear()
        }

        private class FastutilList : BenchmarkableLongList<it.unimi.dsi.fastutil.longs.LongArrayList> {

            override val rawList: it.unimi.dsi.fastutil.longs.LongArrayList = it.unimi.dsi.fastutil.longs.LongArrayList()

            override val size: Int get() = rawList.size

            override fun newInstance(): BenchmarkableLongList<it.unimi.dsi.fastutil.longs.LongArrayList> = FastutilList()
            override fun ensureCapacity(capacity: Int) = rawList.ensureCapacity(capacity)
            override fun forEach(action: (Long) -> Unit) = rawList.forEach(LongConsumer { key -> action(key) })
            @Suppress("DEPRECATION")
            override fun iterate(action: (Long) -> Unit) { for (key in rawList) action(key) }
            override fun indexOf(key: Long) = rawList.indexOf(key)
            override fun add(key: Long) = rawList.add(key)
            override fun removeAt(index: Int) = rawList.removeLong(index)
            override fun addAll(otherList: BenchmarkableLongList<it.unimi.dsi.fastutil.longs.LongArrayList>) = rawList.addAll(otherList.rawList)
            override fun clear() = rawList.clear()
        }

        private class AndroidXList : BenchmarkableLongList<MutableLongList> {

            override val rawList: MutableLongList = MutableLongList()

            override val size: Int get() = rawList.size

            override fun newInstance(): BenchmarkableLongList<MutableLongList> = AndroidXList()
            override fun ensureCapacity(capacity: Int) = rawList.ensureCapacity(capacity)
            override fun forEach(action: (Long) -> Unit) = rawList.forEach { action(it) }
            override fun iterate(action: (Long) -> Unit) = throw UnsupportedOperationException()
            override fun indexOf(key: Long) = rawList.indexOf(key)
            override fun add(key: Long) = rawList.add(key)
            override fun removeAt(index: Int) = rawList.removeAt(index)
            override fun addAll(otherList: BenchmarkableLongList<MutableLongList>) = rawList.addAll(otherList.rawList)
            override fun clear() = rawList.clear()
        }

        private class TroveList : BenchmarkableLongList<TLongArrayList> {

            override val rawList: TLongArrayList = TLongArrayList()

            override val size: Int get() = rawList.size()

            override fun newInstance(): BenchmarkableLongList<TLongArrayList> = TroveList()
            override fun ensureCapacity(capacity: Int) = rawList.ensureCapacity(capacity)
            override fun forEach(action: (Long) -> Unit) { rawList.forEach { action(it); return@forEach true } }
            override fun iterate(action: (Long) -> Unit) { for (key in rawList) action(key) }
            override fun indexOf(key: Long) = rawList.indexOf(key)
            override fun add(key: Long) = rawList.add(key)
            override fun removeAt(index: Int) = rawList.removeAt(index)
            override fun addAll(otherList: BenchmarkableLongList<TLongArrayList>) = rawList.addAll(otherList.rawList)
            override fun clear() = rawList.clear()
        }

        private class EclipseList : BenchmarkableLongList<org.eclipse.collections.impl.list.mutable.primitive.LongArrayList> {

            override val rawList: org.eclipse.collections.impl.list.mutable.primitive.LongArrayList = org.eclipse.collections.impl.list.mutable.primitive.LongArrayList()

            override val size: Int get() = rawList.size()

            override fun newInstance(): BenchmarkableLongList<org.eclipse.collections.impl.list.mutable.primitive.LongArrayList> = EclipseList()
            override fun ensureCapacity(capacity: Int) = rawList.ensureCapacity(capacity)
            override fun forEach(action: (Long) -> Unit) = rawList.forEach { key -> action(key) }
            override fun iterate(action: (Long) -> Unit) { throw UnsupportedOperationException() }
            override fun indexOf(key: Long) = rawList.indexOf(key)
            override fun add(key: Long) = rawList.add(key)
            override fun removeAt(index: Int) = rawList.removeAtIndex(index)
            override fun addAll(otherList: BenchmarkableLongList<org.eclipse.collections.impl.list.mutable.primitive.LongArrayList>) = rawList.addAll(otherList.rawList)
            override fun clear() = rawList.clear()
        }
    }
}
