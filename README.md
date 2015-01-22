Compile and run
===============

To compile and start the test:

    ./compile_and_run.sh

To load (but not install) classes:

    ./send_signal.sh load One Two

This will load `RandoOne` and `RandoTwo`, compiling them first if necessary.

To install (and compile/load if necessary) classes:

    ./send_signal.sh install One Two

Explanation of the experiment
=============================

The `Randomizer` interface is just a simple mapping from `long -> long`. This repo comes with three implementations, each of which provide a different flavor of an xor shift randomizer. You can provide more if you like; if their names start with `RandoX` (the `X` is literal, not a "something goes here" marker), they'll be ignored by git as a convenience.

Each experiment consists of running a randomizer a bunch of times, timing how long it takes and printing the result (so that the optimizer can't optimize the steps away). Inititally, the experiment only loads and uses `RandoOne`.

If you send the `SIGUSER2` message to the test, it'll read `__jpm_message.txt`. The first line of this file should be "load" or "install," and tells the test what to do with the subsequent lines. Those are each simple class names, which are loaded (using `Class.forName`), instantiated, and (if the command is "install") installed to be used in subsequent iterations of the experiment. Installations are _not_ cumulative; if you install `RandoOne` from one invocation and then `RandoTwo` from a second, then both will be loaded, but experiments will only use `RandoTwo`.

The `send_signal.sh` script is provided as a convenience for this signaling. Its first argument is the command ("load" or "install") and subsequent arguments are interpreted as `Rando*` class names. For instance, `./send_signal.sh load Foo` will load a class called `RandoFoo`. If the class's `.class` file is missing, the script will attempt to compile it; if that fails for any reason, the script will abort and not send a signal to the test.

Results
=======

(On my machine, a MacBook Pro running HotSpot build 1.7.0_67-b01.)

- initially (with only `RandoOne` loaded), each run takes about 3800 ms
- loading (but not installing) the two additional implementations has no noticeable effect
- installing `RandoOne` and `RandoTwo` slows things down, to about 6000 ms
- installing `RandoThree` in addition to the other two for the first time slows things further.
  - Intially, each experiment takes about 10000 ms
  - After it goes through the second round of compilation (in which profiling is removed, and the result is assumed to be as good as it'll get), it bumps even higher, to about 17000 ms
- installing `RandoXFour` (not provided in this code, but yet another variation on the xorshift theme) has no noticeable effect
- installing only `RandoOne` speeds things up, but only to about 6500 ms. The original 3800 ms speeds are gone forever (well, for this JVM instance).

Interpretation
==============

This is my best guess at understanding what's going on. Feedback is welcome (if you're visiting from the outside world, maybe open an issue to start a discussion?).

In the initial setup, the JIT is able to figure out pretty quickly that `runExperiment` is monomorphic; there's only one implementation of `Randomizer` in play. It's able to de-virtualize the call. I would think that loading the two other implementations would slow things down, as that assumption can no longer be proved; but they don't. Maybe it was never proved at all (that is, the JIT still double checked that the site is still monomorphic on each run). Loading but not installing the classes does _not_ result in recompliation of `runExperiment`, which suggests that the monomorphic check is in fact being performed even before those classes are loaded.

Installing `RandoTwo` for the first time triggers recompilation, as I'd expect. The end result is slower, which is also expected, since there are now two possibilities for each virtual call. Neither the JIT nor the CPU's branch prediction can get around the pain of doing an actual check and lookup.

Installing `RandoThree` for the first time really slows things down. Not only does `runExperiment` take significantly longer, but it also takes much longer for `runExperiment` to go through both phases of compilation. I suspect that the jump from 2 to 3 implementation precludes certain optimizations. This causes the JIT to try even harder to optimize, which is why the second phase takes a while. In the end, its efforts are counterproductive, and the second phase is even _slower_ than before. This is a mystery to me.

Installing `RandoXFour` has no effect. I think by the time the call site has seen three implementations, the damage is done; it just assumes full polymophism from now on.

Installing only `RandoOne` at this point doesn't trigger a recompilation; the damage is already done in terms of the JIT assuming it needs the vtable lookup code (and whatever optimizations the JIT tried to do). Even though we're now effectively at the same place we were when all three classes were loaded but only `RandoOne` was in use (which was the fastest scenario, at about 3800 ms), the JVM is running code that doesn't know about that optimization and is running a much slower version of the code (which, happily, the CPU's branch predictor is able to ameliorate to some degree). That said, the code _does_ run faster at this point with only `RandoOne` installed. I suspect this is not because of anything in the JIT, but rather that the monomorphic nature of the call makes it easy for the CPU's branch prediction to pipeline the instructions.

Incidentally, you don't need to wait for the three-implementation JIT code to go through both phases of compilation before the damage is permanent. I would have thought you would; that since the first phase includes profiling hooks for the JIT, re-installing only `RandoOne` would let the JIT rediscover the monomorphism. But it looks like as soon as the site has seen three implementations, it assumes the worst forever. In fact, just installing each implemenation alone, one at a time, is enough.

    ./send_signal.sh install One            # no-op; this is the starting state
    ./send_signal.sh install Two            # triggers recompilation, but not much slower
    ./send_signal.sh install Three          # triggers recompilation, significantly slower
                                            # (branch predictor is happy?)
    ./send_signal.sh install One Two Three  # does not trigger recompilation, but is now even slower
                                            # (branch predictor is helpless?)

