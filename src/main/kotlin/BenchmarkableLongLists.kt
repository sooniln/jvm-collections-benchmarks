package io.github.sooniln.jvmcollectionsbenchmark

import androidx.collection.MutableLongList
import gnu.trove.list.array.TLongArrayList
import io.github.sooniln.fastcollect.*
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
    fun forEach(action: LongConsumer) = iterate(action)
    fun iterate(action: LongConsumer)
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
            "HPPC" to { HPPCList() },
            "Agrona" to { AgronaList() },
            "LibGDX" to { LibGdxList() },
        )

        fun from(type: String): BenchmarkableLongList<*> = map.getOrElse(type) { throw IllegalArgumentException("Unknown type: $type") }.invoke()

        fun all(): Sequence<Pair<String, BenchmarkableLongList<*>>> = map.entries.asSequence().map { it.key to it.value.invoke() }

        private class JreList : BenchmarkableLongList<ArrayList<Long>> {

            override val rawList: ArrayList<Long> = ArrayList()

            override val size: Int get() = rawList.size

            override fun newInstance(): BenchmarkableLongList<ArrayList<Long>> = JreList()
            override fun ensureCapacity(capacity: Int) = rawList.ensureCapacity(capacity)
            override fun iterate(action: LongConsumer) { for (key in rawList) action.accept(key) }
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
            override fun forEach(action: LongConsumer) { rawList.foreach { action.accept(it) } }
            override fun iterate(action: LongConsumer) { for (key in rawList) action.accept(key) }
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
            override fun forEach(action: LongConsumer) = rawList.forEach(LongConsumer { key -> action.accept(key) })
            @Suppress("DEPRECATION")
            override fun iterate(action: LongConsumer) { for (key in rawList) action.accept(key) }
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
            override fun forEach(action: LongConsumer) = rawList.forEach { action.accept(it) }
            override fun iterate(action: LongConsumer) = throw UnsupportedOperationException()
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
            override fun forEach(action: LongConsumer) { rawList.forEach { action.accept(it); return@forEach true } }
            override fun iterate(action: LongConsumer) { for (key in rawList) action.accept(key) }
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
            override fun forEach(action: LongConsumer) = rawList.forEach { key -> action.accept(key) }
            override fun iterate(action: LongConsumer) { throw UnsupportedOperationException() }
            override fun indexOf(key: Long) = rawList.indexOf(key)
            override fun add(key: Long) = rawList.add(key)
            override fun removeAt(index: Int) = rawList.removeAtIndex(index)
            override fun addAll(otherList: BenchmarkableLongList<org.eclipse.collections.impl.list.mutable.primitive.LongArrayList>) = rawList.addAll(otherList.rawList)
            override fun clear() = rawList.clear()
        }

        private class HPPCList : BenchmarkableLongList<com.carrotsearch.hppc.LongArrayList> {

            override val rawList: com.carrotsearch.hppc.LongArrayList = com.carrotsearch.hppc.LongArrayList()

            override val size: Int get() = rawList.size()

            override fun newInstance(): BenchmarkableLongList<com.carrotsearch.hppc.LongArrayList> = HPPCList()
            override fun ensureCapacity(capacity: Int) = rawList.ensureCapacity(capacity)
            override fun forEach(action: LongConsumer) = rawList.forEach { key -> action.accept(key.value) }
            override fun iterate(action: LongConsumer) { throw UnsupportedOperationException() }
            override fun indexOf(key: Long) = rawList.indexOf(key)
            override fun add(key: Long): Boolean { rawList.add(key); return true }
            override fun removeAt(index: Int) = rawList.removeAt(index)
            override fun addAll(otherList: BenchmarkableLongList<com.carrotsearch.hppc.LongArrayList>) = rawList.addAll(otherList.rawList) > 0
            override fun clear() = rawList.clear()
        }

        private class AgronaList : BenchmarkableLongList<org.agrona.collections.LongArrayList> {

            override val rawList: org.agrona.collections.LongArrayList = org.agrona.collections.LongArrayList()

            override val size: Int get() = rawList.size

            override fun newInstance(): BenchmarkableLongList<org.agrona.collections.LongArrayList> = AgronaList()
            override fun ensureCapacity(capacity: Int) = rawList.ensureCapacity(capacity)
            override fun forEach(action: LongConsumer) = rawList.forEach { key -> action.accept(key) }
            override fun iterate(action: LongConsumer) { throw UnsupportedOperationException() }
            override fun indexOf(key: Long) = rawList.indexOf(key)
            override fun add(key: Long): Boolean { rawList.add(key); return true }
            override fun removeAt(index: Int) = rawList.removeAt(index)
            override fun addAll(otherList: BenchmarkableLongList<org.agrona.collections.LongArrayList>) = rawList.addAll(otherList.rawList)
            override fun clear() = rawList.clear()
        }

        private class LibGdxList : BenchmarkableLongList<com.badlogic.gdx.utils.LongArray> {

            override val rawList: com.badlogic.gdx.utils.LongArray = com.badlogic.gdx.utils.LongArray()

            override val size: Int get() = rawList.size

            override fun newInstance(): BenchmarkableLongList<com.badlogic.gdx.utils.LongArray> = LibGdxList()
            override fun ensureCapacity(capacity: Int) { rawList.ensureCapacity(capacity - rawList.size) }
            override fun iterate(action: LongConsumer) { for (i in 0 until rawList.size) action.accept(rawList[i]) }
            override fun indexOf(key: Long) = rawList.indexOf(key)
            override fun add(key: Long): Boolean { rawList.add(key); return true }
            override fun removeAt(index: Int) = rawList.removeIndex(index)
            override fun addAll(otherList: BenchmarkableLongList<com.badlogic.gdx.utils.LongArray>): Boolean { rawList.addAll(otherList.rawList); return true }
            override fun clear() = rawList.clear()
        }
    }
}
