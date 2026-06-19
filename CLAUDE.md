This project's purpose is to benchmark the CPU and memory characteristics of various collection implementations in the
JVM ecosystem.

## JMH Arguments

Various flags may be used to set JMH parameters for JMH gradle tasks:
* -P-i=<number> to set the number of iterations
* -P-wi=<number> to set the number of warmup iterations
* -PjmhIncludes="<regex>" to only run benchmarks matching the given regular expression
* -PjmhSize="<number>(,<number>...)" to set the size parameter
* -PjmhType="<type>(,<type>...)" to set the type parameter
* -PjmhOrder="<order>(,<order>...)" to set the order parameter

## JIT ASM

The jitAsm sub-directory holds harnesses that can be run to output JIT ASM for interesting methods to allow for detailed
analysis of the assembly code and a deeper understanding of real performance.