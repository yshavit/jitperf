Compile and run
===============

    javac -XDignore.symbol.file *.java && java -XX:+PrintCompilation JitPerfMain
    
Explanation of the experiment
=============================

The `Randomizer` interface is just a simple mapping from `long -> long`. It has three implementations, each of which provide a different flavor of an xor shift randomizer.

Each experiment consists of running a randomizer a lot of times, timing how long it takes and printing the result (so that the optimizer can't optimize the steps away). Inititally, all randomizers use the `RandoOne`.

In addition, sending the `SIGUSR2` signal has various effects (you can use the `send_signal.sh` script to do it easily).

- the first two times you do it, it'll load (but not use) `RandoTwo` and `RandoThree`, respectively
- the third time, it's a no-op
- subsequent times work in a pattern:
  - set 1/3 of the randomizers to `RandoTwo` (and keep the rest as `RandoOne`)
  - set 1/3 of the randomizers to each of the three implementations
  - set all implementations to `RandoOne`

Results
=======

(On my machine, a MacBook Pro running HotSpot build 1.7.0_67-b01.)

- initially (with only `RandoOne` loaded), each run takes about 3800 ms
- loading the two additional implementations has no noticeable effect
- using `RandoTwo` for the fist time slows things down, to about 6000 ms
- using `RandoThree` for the first time slows things further.
  - Intially, each experiment takes about 10000 ms
  - After it goes through the second round of compilation (in which profiling is removed, and the result is assumed to be as good as it'll get), it bumps even higher, to about 17000 ms
- setting the randoms all back to `RandoOne` slows things down to about 6500 ms

Interpretation
==============

This is my best guess at understanding what's going on. Feedback is welcome (if you're visiting from the outside world, maybe open an issue to start a discussion?).

- In the initial setup, the JIT is able to figure out pretty quickly that `runExperiment` is monomorphic; there's only one implementation of `Randomizer` in play. It's able to de-virtualize the call.
- I would think that loading the two other implementations would slow things down, as that assumption can no longer be proved; but they don't. Maybe it was never proved at all (that is, the JIT still double checked that the site is still monomorphic on each run). Incidentally, loading but not using the classes does _not_ result in recompliation of `runExperiment`, which suggests that the check is in fact being performed even before those classes are loaded.
- Using `RandoOne` for the first time triggers recompilation, as I'd expect. The end result is slower, as I'd expect, since there are now two possibilities for each virtual call. Neither the JIT nor the CPU's branch prediction can get around the pain of doing an actual check and lookup.
- Using `RandoThree` for the first time really slows things down. Not only does `runExperiment` take significantly longer, but it also takes much longer for `runExperiment` to go through both phases of compilation. I suspect that the jump from 2 to 3 implementation precludes certain optimizations. This causes the JIT to try even harder to optimize, which is why the second phase takes a while. In the end, its efforts are counterproductive, and the second phase is even _slower_ than before. This part is a mystery to me.
- Setting them all back to `RandoOne` helps the branch predictor a lot; all roads now lead to the same place. But the damage is already done in terms of the vtable lookup code (and whatever optimizations the JIT tried to do). Furthermore, since the second pass of the JIT takes away profiling information, the JIT isn't able to recognize that the site is now monomorphic. In the absence of anything to cause a recompilation (such as yet another implementation), the slow, polymorphic version of `runExperiment` keeps on going. Even though we're now effectively at the same place we were when all three classes were loaded but only `RandoOne` was in use (which was the fastest scenario, at about 3800 ms), the JVM is running code that doesn't know about that optimization and is running a much slower version of the code (which, happily, the CPU's branch predictor is able to ameliorate to some degree).
