package io.github.sooniln.jvmcollectionsbenchmark

import androidx.collection.MutableIntList
import gnu.trove.list.array.TIntArrayList
import io.github.sooniln.fastcollect.*
import java.util.function.IntConsumer

interface BenchmarkableIntList<T> {
    val rawList: T

    val size: Int

    fun newInstance(): BenchmarkableIntList<T>
    fun copyInstance(): BenchmarkableIntList<T> {
        val copy = newInstance()
        copy.addAll(this)
        return copy
    }

    fun ensureCapacity(capacity: Int)
    fun forEach(action: IntConsumer) = iterate(action)
    fun iterate(action: IntConsumer)
    fun indexOf(key: Int): Int
    fun add(key: Int): Boolean
    fun removeAt(index: Int): Int
    fun addAll(otherList: BenchmarkableIntList<T>): Boolean
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

        fun from(type: String): BenchmarkableIntList<*> = map.getOrElse(type) { throw IllegalArgumentException("Unknown type: $type") }.invoke()

        fun all(): Sequence<Pair<String, BenchmarkableIntList<*>>> = map.entries.asSequence().map { it.key to it.value.invoke() }

        private class JreList : BenchmarkableIntList<ArrayList<Int>> {

            override val rawList: ArrayList<Int> = ArrayList()

            override val size: Int get() = rawList.size

            override fun newInstance(): BenchmarkableIntList<ArrayList<Int>> = JreList()
            override fun ensureCapacity(capacity: Int) = rawList.ensureCapacity(capacity)
            override fun iterate(action: IntConsumer) { for (key in rawList) action.accept(key) }
            override fun indexOf(key: Int) = rawList.indexOf(key)
            override fun add(key: Int) = rawList.add(key)
            override fun removeAt(index: Int) = rawList.removeAt(index)
            override fun addAll(otherList: BenchmarkableIntList<ArrayList<Int>>) = rawList.addAll(otherList.rawList)
            override fun clear() = rawList.clear()
        }

        private class FastCollectList : BenchmarkableIntList<IntArrayList> {

            override val rawList: IntArrayList = IntArrayList()

            override val size: Int get() = rawList.size

            override fun newInstance(): BenchmarkableIntList<IntArrayList> = FastCollectList()
            override fun ensureCapacity(capacity: Int) = rawList.ensureCapacity(capacity)
            override fun forEach(action: IntConsumer) { rawList.foreach { action.accept(it) } }
            override fun iterate(action: IntConsumer) { for (key in rawList) action.accept(key) }
            override fun indexOf(key: Int) = rawList.indexOf(key)
            override fun add(key: Int) = rawList.add(key)
            override fun removeAt(index: Int) = rawList.removeAt(index)
            override fun addAll(otherList: BenchmarkableIntList<IntArrayList>) = rawList.addAll(otherList.rawList)
            override fun clear() = rawList.clear()
        }

        private class FastutilList : BenchmarkableIntList<it.unimi.dsi.fastutil.ints.IntArrayList> {

            override val rawList: it.unimi.dsi.fastutil.ints.IntArrayList = it.unimi.dsi.fastutil.ints.IntArrayList()

            override val size: Int get() = rawList.size

            override fun newInstance(): BenchmarkableIntList<it.unimi.dsi.fastutil.ints.IntArrayList> = FastutilList()
            override fun ensureCapacity(capacity: Int) = rawList.ensureCapacity(capacity)
            override fun forEach(action: IntConsumer) = rawList.forEach(IntConsumer { key -> action.accept(key) })
            @Suppress("DEPRECATION")
            override fun iterate(action: IntConsumer) { for (key in rawList) action.accept(key) }
            override fun indexOf(key: Int) = rawList.indexOf(key)
            override fun add(key: Int) = rawList.add(key)
            override fun removeAt(index: Int) = rawList.removeInt(index)
            override fun addAll(otherList: BenchmarkableIntList<it.unimi.dsi.fastutil.ints.IntArrayList>) = rawList.addAll(otherList.rawList)
            override fun clear() = rawList.clear()
        }

        private class AndroidXList : BenchmarkableIntList<MutableIntList> {

            override val rawList: MutableIntList = MutableIntList()

            override val size: Int get() = rawList.size

            override fun newInstance(): BenchmarkableIntList<MutableIntList> = AndroidXList()
            override fun ensureCapacity(capacity: Int) = rawList.ensureCapacity(capacity)
            override fun forEach(action: IntConsumer) = rawList.forEach { action.accept(it) }
            override fun iterate(action: IntConsumer) = throw UnsupportedOperationException()
            override fun indexOf(key: Int) = rawList.indexOf(key)
            override fun add(key: Int) = rawList.add(key)
            override fun removeAt(index: Int) = rawList.removeAt(index)
            override fun addAll(otherList: BenchmarkableIntList<MutableIntList>) = rawList.addAll(otherList.rawList)
            override fun clear() = rawList.clear()
        }

        private class TroveList : BenchmarkableIntList<TIntArrayList> {

            override val rawList: TIntArrayList = TIntArrayList()

            override val size: Int get() = rawList.size()

            override fun newInstance(): BenchmarkableIntList<TIntArrayList> = TroveList()
            override fun ensureCapacity(capacity: Int) = rawList.ensureCapacity(capacity)
            override fun forEach(action: IntConsumer) { rawList.forEach { action.accept(it); return@forEach true } }
            override fun iterate(action: IntConsumer) { for (key in rawList) action.accept(key) }
            override fun indexOf(key: Int) = rawList.indexOf(key)
            override fun add(key: Int) = rawList.add(key)
            override fun removeAt(index: Int) = rawList.removeAt(index)
            override fun addAll(otherList: BenchmarkableIntList<TIntArrayList>) = rawList.addAll(otherList.rawList)
            override fun clear() = rawList.clear()
        }

        private class EclipseList : BenchmarkableIntList<org.eclipse.collections.impl.list.mutable.primitive.IntArrayList> {

            override val rawList: org.eclipse.collections.impl.list.mutable.primitive.IntArrayList = org.eclipse.collections.impl.list.mutable.primitive.IntArrayList()

            override val size: Int get() = rawList.size()

            override fun newInstance(): BenchmarkableIntList<org.eclipse.collections.impl.list.mutable.primitive.IntArrayList> = EclipseList()
            override fun ensureCapacity(capacity: Int) = rawList.ensureCapacity(capacity)
            override fun forEach(action: IntConsumer) = rawList.forEach { key -> action.accept(key) }
            override fun iterate(action: IntConsumer) { throw UnsupportedOperationException() }
            override fun indexOf(key: Int) = rawList.indexOf(key)
            override fun add(key: Int) = rawList.add(key)
            override fun removeAt(index: Int) = rawList.removeAtIndex(index)
            override fun addAll(otherList: BenchmarkableIntList<org.eclipse.collections.impl.list.mutable.primitive.IntArrayList>) = rawList.addAll(otherList.rawList)
            override fun clear() = rawList.clear()
        }

        private class HPPCList : BenchmarkableIntList<com.carrotsearch.hppc.IntArrayList> {

            override val rawList: com.carrotsearch.hppc.IntArrayList = com.carrotsearch.hppc.IntArrayList()

            override val size: Int get() = rawList.size()

            override fun newInstance(): BenchmarkableIntList<com.carrotsearch.hppc.IntArrayList> = HPPCList()
            override fun ensureCapacity(capacity: Int) = rawList.ensureCapacity(capacity)
            override fun forEach(action: IntConsumer) = rawList.forEach { key -> action.accept(key.value) }
            override fun iterate(action: IntConsumer) { for (element in rawList) action.accept(element.value) }
            override fun indexOf(key: Int) = rawList.indexOf(key)
            override fun add(key: Int): Boolean { rawList.add(key); return true }
            override fun removeAt(index: Int) = rawList.removeAt(index)
            override fun addAll(otherList: BenchmarkableIntList<com.carrotsearch.hppc.IntArrayList>) = rawList.addAll(otherList.rawList) > 0
            override fun clear() = rawList.clear()
        }

        private class AgronaList : BenchmarkableIntList<org.agrona.collections.IntArrayList> {

            override val rawList: org.agrona.collections.IntArrayList = org.agrona.collections.IntArrayList()

            override val size: Int get() = rawList.size

            override fun newInstance(): BenchmarkableIntList<org.agrona.collections.IntArrayList> = AgronaList()
            override fun ensureCapacity(capacity: Int) = rawList.ensureCapacity(capacity)
            override fun forEach(action: IntConsumer) = rawList.forEach { key -> action.accept(key) }
            override fun iterate(action: IntConsumer) { for (element in rawList) action.accept(element) }
            override fun indexOf(key: Int) = rawList.indexOf(key)
            override fun add(key: Int) = rawList.add(key)
            override fun removeAt(index: Int) = rawList.removeAt(index)
            override fun addAll(otherList: BenchmarkableIntList<org.agrona.collections.IntArrayList>) = rawList.addAll(otherList.rawList)
            override fun clear() = rawList.clear()
        }

        private class LibGdxList : BenchmarkableIntList<com.badlogic.gdx.utils.IntArray> {

            override val rawList: com.badlogic.gdx.utils.IntArray = com.badlogic.gdx.utils.IntArray()

            override val size: Int get() = rawList.size

            override fun newInstance(): BenchmarkableIntList<com.badlogic.gdx.utils.IntArray> = LibGdxList()
            override fun ensureCapacity(capacity: Int) { rawList.ensureCapacity(capacity - rawList.size) }
            override fun iterate(action: IntConsumer) { for (i in 0 until rawList.size) action.accept(rawList[i]) }
            override fun indexOf(key: Int) = rawList.indexOf(key)
            override fun add(key: Int): Boolean { rawList.add(key); return true }
            override fun removeAt(index: Int) = rawList.removeIndex(index)
            override fun addAll(otherList: BenchmarkableIntList<com.badlogic.gdx.utils.IntArray>): Boolean { rawList.addAll(otherList.rawList); return true }
            override fun clear() = rawList.clear()
        }
    }
}
