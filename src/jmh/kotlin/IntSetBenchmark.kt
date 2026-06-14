package io.github.sooniln.jvmcollectionsbenchmark

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

/**
 * A JVM specific benchmark which measures the performance of various set libraries.
 */
@Fork(1)
@Warmup(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 250, timeUnit = TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
open class IntSetBenchmark {

    private companion object {
        private val seed = System.currentTimeMillis()
        private val timeout = 30.seconds
    }

    @State(Scope.Benchmark)
    open class BaseState {
        @Param("JRE", "FastCollect", "Fastutil", "AndroidX", "Trove", "Eclipse", "HPPC", "Agrona", "PrimitiveCollections")
        var type: String = ""

        @Param("12", "14", "16", "18", "20", "22", "24")
        var pow2: Int = 12

        @Param(".50", ".75")
        var loadFactor: Float = .75f

        var size: Int = 0
        lateinit var set: BenchmarkableIntSet<*>

        @Setup(Level.Trial)
        open fun setup() {
            if (loadFactor > .5 && type == "Eclipse") {
                throw UnsupportedOperationException("eclipse does not support load factors over .5")
            }

            size = ((1 shl pow2) * loadFactor).toInt() - 2
            set = BenchmarkableIntSet.from(type)
        }
    }

    @State(Scope.Benchmark)
    open class RandomState : BaseState() {

        lateinit var keys: IntArray

        @Setup(Level.Trial)
        override fun setup() {
            super.setup()

            keys = IntArray(size)
            KeyGenerators.generateRandomKeys(keys, seed = seed)

            val startTime = System.nanoTime()

            set.ensureCapacity(keys.size)
            for (key in keys) {
                if ((System.nanoTime() - startTime).nanoseconds > timeout) throw TimeoutException()
                set.add(key)
            }
        }
    }

    @State(Scope.Benchmark)
    open class FullState : BaseState() {

        @Param("random", "sequential", "even", "partition", "highBits")
        var order: String = "random"

        var idx = 0
        lateinit var inKeys: IntArray
        lateinit var outKeys: IntArray

        @Setup(Level.Trial)
        override fun setup() {
            super.setup()

            inKeys = IntArray(size)
            outKeys = IntArray(size)
            KeyGenerators.generateKeys(order, inKeys, outKeys, seed = seed)

            val startTime = System.nanoTime()

            set.ensureCapacity(inKeys.size)
            for (key in inKeys) {
                if ((System.nanoTime() - startTime).nanoseconds > timeout) throw TimeoutException()
                set.add(key)
            }
        }

        inline fun <T> nextInKey(crossinline action: FullState.(Int) -> T): T {
            val t = action(inKeys[idx])
            if (++idx == inKeys.size) {
                idx = 0
            }
            return t
        }

        inline fun <T> nextOutKey(crossinline action: FullState.(Int) -> T): T {
            val t = action(outKeys[idx])
            if (++idx == outKeys.size) {
                idx = 0
            }
            return t
        }

        inline fun <T> nextInOutKeys(action: FullState.(Int, Int) -> T): T {
            val t = action(inKeys[idx], outKeys[idx])
            if (++idx == inKeys.size) {
                idx = 0
            }
            return t
        }

        @Suppress("NOTHING_TO_INLINE")
        inline fun swapInOut() {
            val t = inKeys[idx]
            inKeys[idx] = outKeys[idx]
            outKeys[idx] = t
        }
    }

    @State(Scope.Benchmark)
    open class EmptyState : FullState() {
        @Setup(Level.Trial)
        override fun setup() {
            super.setup()
            set.clear()
        }

        inline fun <T> nextMissInKey(crossinline action: FullState.(Int) -> T): T {
            val t = action(inKeys[idx])
            if (++idx == inKeys.size) {
                idx = 0
                set.clear()
            }
            return t
        }
    }

    @Benchmark
    fun naiveCopy(state: RandomState): BenchmarkableIntSet<*> {
        val copy = state.set.newInstance()
        state.set.forEach { key -> copy.add(key) }
        return copy
    }

    @Benchmark
    fun preAllocatedCopy(state: RandomState) = state.set.copyInstance()

    @Benchmark
    fun getHit(state: FullState) = state.nextInKey { key -> set.contains(key) }

    @Benchmark
    fun getMiss(state: FullState) = state.nextOutKey { key -> set.contains(key) }

    @Benchmark
    fun putHit(state: FullState) = state.nextInKey { key -> set.add(key) }

    @Benchmark
    fun putMiss(state: EmptyState) = state.nextMissInKey { key -> set.add(key) }

    @Benchmark
    fun removeAndPutMiss(state: FullState) = state.nextInOutKeys { inKey, outKey ->
        set.remove(inKey)
        set.add(outKey)
        swapInOut()
    }

    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Benchmark
    fun iterate(state: FullState, bh: Blackhole) = state.set.iterate { key -> bh.consume(key) }

    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Benchmark
    fun forEach(state: FullState, bh: Blackhole) = state.set.forEach { key -> bh.consume(key) }
}
