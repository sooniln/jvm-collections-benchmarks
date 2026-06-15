import me.champeau.jmh.JMHTask
import org.gradle.kotlin.dsl.kotlin
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    kotlin("jvm") version "2.4.0"
    id("me.champeau.jmh") version "0.7.3"
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("org.openjdk.jol:jol-core:0.17")

    implementation("io.github.sooniln:fastcollect-kotlin-jvm:2.0.0")
    implementation("it.unimi.dsi:fastutil:8.5.18")
    implementation("org.eclipse.collections:eclipse-collections:13.0.0")
    implementation("androidx.collection:collection-jvm:1.6.0")
    implementation("net.sf.trove4j:core:3.1.0")
    implementation("com.koloboke:koloboke-impl-jdk8:1.0.0")
    implementation("com.carrotsearch:hppc:0.10.0")
    implementation("org.agrona:agrona:2.4.1")
    implementation("io.github.speiger:Primitive-Collections:1.0.0")
}

jmh {
    includeTests = false
    verbosity = "EXTRA"
    failOnError = true
    resultFormat = "JSON"

    // if a jmhIncludes property is set, forward it to JMH
    findProperty("jmhIncludes")?.also { includes.set(listOf(it as String)) }
}

registerMemoryMeasurementTask("memory") {
    mainClass = "io.github.sooniln.jvmcollectionsbenchmark.memory.MemoryMeasurementKt"
}

registerMemoryMeasurementTask("memorySplits") {
    mainClass = "io.github.sooniln.jvmcollectionsbenchmark.memory.SplitMemoryMeasurementKt"
}

registerMemoryMeasurementTask("naiveCopySim") {
    mainClass = "io.github.sooniln.jvmcollectionsbenchmark.NaiveCopySimKt"
}

registerJMHTask("IntList") {
    includes.set(listOf("IntListBenchmark\\."))
}

registerJMHTask("LongList") {
    includes.set(listOf("LongListBenchmark\\."))
}

registerJMHTask("IntSet") {
    includes.set(listOf("IntSetBenchmark\\."))
}

registerJMHTask("LongSet") {
    includes.set(listOf("LongSetBenchmark\\."))
}

registerJMHTask("IntMap") {
    includes.set(listOf("IntMapBenchmark\\."))
}

registerJMHTask("LongMap") {
    includes.set(listOf("LongMapBenchmark\\."))
}

registerJitAsm("Int2IntHashMapLookup") {
    mainClass = "io.github.sooniln.jvmcollectionsbenchmark.jitasm.Int2IntHashMapLookupAsmProbe"
    jvmArgs(
        "-XX:CompileCommand=quiet",
        "-XX:CompileCommand=compileonly,io.github.sooniln.fastcollect.ints.Int2IntHashMap::get",
        "-XX:CompileCommand=print,io.github.sooniln.fastcollect.ints.Int2IntHashMap::get",
        "-XX:CompileCommand=compileonly,com.koloboke.collect.impl.hash.MutableLHashParallelKVIntIntMapGO::get",
        "-XX:CompileCommand=print,com.koloboke.collect.impl.hash.MutableLHashParallelKVIntIntMapGO::get",
    )
}

registerJitAsm("Int2IntHashMapIterate") {
    mainClass = "io.github.sooniln.jvmcollectionsbenchmark.jitasm.Int2IntHashMapIterateAsmProbe"
    jvmArgs(
        "-XX:CompileCommand=quiet",
        "-XX:CompileCommand=compileonly,io.github.sooniln.jvmcollectionsbenchmark.jitasm.Int2IntHashMapIterateAsmProbe::iterate1",
        "-XX:CompileCommand=print,io.github.sooniln.jvmcollectionsbenchmark.jitasm.Int2IntHashMapIterateAsmProbe::iterate1",
        "-XX:CompileCommand=compileonly,io.github.sooniln.jvmcollectionsbenchmark.jitasm.Int2IntHashMapIterateAsmProbe::iterate2",
        "-XX:CompileCommand=print,io.github.sooniln.jvmcollectionsbenchmark.jitasm.Int2IntHashMapIterateAsmProbe::iterate2",
    )
}

registerJitAsm("IntHashSetContains") {
    mainClass = "io.github.sooniln.jvmcollectionsbenchmark.jitasm.IntHashSetContainsAsmProbe"
    jvmArgs(
        "-XX:CompileCommand=quiet",
        "-XX:CompileCommand=compileonly,io.github.sooniln.fastcollect.ints.IntHashSet::contains",
        "-XX:CompileCommand=print,io.github.sooniln.fastcollect.ints.IntHashSet::contains",
        "-XX:CompileCommand=compileonly,org.eclipse.collections.impl.set.mutable.primitive.IntHashSet::contains",
        "-XX:CompileCommand=print,org.eclipse.collections.impl.set.mutable.primitive.IntHashSet::contains",
    )
}

registerJitAsm("IntHashSetGrow") {
    mainClass = "io.github.sooniln.jvmcollectionsbenchmark.jitasm.IntHashSetGrowAsmProbe"
    jvmArgs(
        "-XX:CompileCommand=quiet",
        "-XX:CompileCommand=compileonly,io.github.sooniln.fastcollect.ints.IntHashSet::rehash",
        "-XX:CompileCommand=print,io.github.sooniln.fastcollect.ints.IntHashSet::rehash",
        "-XX:CompileCommand=compileonly,it.unimi.dsi.fastutil.ints.IntOpenHashSet::rehash",
        "-XX:CompileCommand=print,it.unimi.dsi.fastutil.ints.IntOpenHashSet::rehash",
    )
}

private fun registerMemoryMeasurementTask(name: String, configuration: JavaExec.() -> Unit) = tasks.register<JavaExec>(name) {
    group = "benchmark"
    description = "Measure memory of collections"
    classpath = sourceSets.main.get().runtimeClasspath
    jvmArgs("-Djdk.attach.allowAttachSelf=true")

    configuration()
}

private fun registerJMHTask(name: String, configuration: JMHTask.()->Unit): TaskProvider<JMHTask> = tasks.register<JMHTask>("jmh$name") {
    group = "benchmark"
    description = "Run JMH benchmarks for $name"

    includeTests = false
    verbosity = "EXTRA"
    failOnError = true
    resultFormat = "JSON"

    val baseTask = tasks.named<JMHTask>("jmh")

    jmhClasspath = baseTask.get().jmhClasspath
    testRuntimeClasspath = baseTask.get().testRuntimeClasspath
    jarArchive = baseTask.get().jarArchive
    javaLauncher = baseTask.get().javaLauncher
    resultsFile = baseTask.get().resultsFile

    configuration()
}

private val copyTask = tasks.register<Copy>("CopyJmhResults") {
    description = "Copy last JMH results into benchmark-results directory."

    from("build/results/jmh/results.json")
    into("benchmark-results")
    rename { "${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))}.json" }
}

private abstract class ExclusiveTaskService : BuildService<BuildServiceParameters.None>
private val exclusiveServiceProvider = gradle.sharedServices.registerIfAbsent("exclusiveTask", ExclusiveTaskService::class.java) {
    maxParallelUsages.set(1)
}

private val jmhPow2: Provider<String> = providers.gradleProperty("jmhPow2")
private val jmhLoadFactor: Provider<String> = providers.gradleProperty("jmhLoadFactor")
private val jmhType: Provider<String> = providers.gradleProperty("jmhType")
private val jmhOrder: Provider<String> = providers.gradleProperty("jmhOrder")

tasks.withType<JMHTask> {
    // ensure JMH tasks are never cached
    outputs.cacheIf { false }
    outputs.upToDateWhen { false }

    // ensure jmh tasks cannot run in parallel
    usesService(exclusiveServiceProvider)

    // save all benchmark data
    finalizedBy(copyTask)

    // forward various parameters to JMH
    if (jmhPow2.isPresent) benchmarkParameters.put("pow2", decodeArgs(jmhPow2.get()))
    if (jmhLoadFactor.isPresent) benchmarkParameters.put("loadFactor", decodeArgs(jmhLoadFactor.get()))
    if (jmhType.isPresent) benchmarkParameters.put("type", decodeArgs(jmhType.get()))
    if (jmhOrder.isPresent) benchmarkParameters.put("order", decodeArgs(jmhOrder.get()))
}

private fun decodeArgs(args: String): ListProperty<String> {
    return objects.listProperty(String::class.java).apply { addAll(args.split(",")) }
}

private fun registerJitAsm(name: String, configuration: JavaExec.() -> Unit): TaskProvider<JavaExec> = tasks.register<JavaExec>("jitAsm$name") {
    group = "investigate"
    description = "Run a small harness that heats up and prints the generated machine code (requires hsdis to be present on path)."
    classpath = sourceSets["main"].runtimeClasspath
    jvmArgs(
        "-Xms256m",
        "-Xmx256m",
        "-Xbatch",
        // Force C2 (server compiler) so the assembly reflects final hot-path optimizations.
        "-XX:-TieredCompilation",
        "-XX:CICompilerCount=2",
        "-XX:CompileThreshold=1000",
        // Assembly
        "-XX:+UnlockDiagnosticVMOptions",
        "-XX:+PrintAssembly",
        "-XX:PrintAssemblyOptions=intel",
    )
    configuration()
}

