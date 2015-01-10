import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import sun.misc.Signal;
import sun.misc.SignalHandler;

public class JitPerfMain {
  public static final long LOOPS_PER_EXPERIMENT = 1000000000;
  
  public static void main(String[] args) {
    AtomicInteger sync = new AtomicInteger(); 
    Randomizer[] randoms = new Randomizer[3];
    for (int i = 0; i < randoms.length; ++i) {
      randoms[i] = new RandoOne();
    }
    
    setUpHandler(sync, randoms);
    
    long seed = System.nanoTime();    
    while (true) {
      seed = runExperiment(seed, randoms);
      
    }
  }

  private static long runExperiment(long seed, Randomizer[] randomizers) {
    Randomizer[] allRandos = new Randomizer[randomizers.length * 100];
    for (int i = 0; i < allRandos.length; ++i) {
      allRandos[i] = randomizers[i % randomizers.length];
    }
    Collections.shuffle(Arrays.asList(allRandos));
    long start = System.nanoTime();
    for (long i = 0; i < LOOPS_PER_EXPERIMENT; ++i) {
      int idx = ((int)i) % allRandos.length;
      Randomizer randomizer = allRandos[idx];
      seed = randomizer.randomize(seed);
    }
    long end = System.nanoTime();
    System.out.printf("%s ms (0x%x)%n", TimeUnit.NANOSECONDS.toMillis(end - start), seed);
    return seed;
  }

  private static void setUpHandler(final AtomicInteger sync, final Randomizer[] randoms) {
    final String sigName = "USR2";
    SignalHandler handler = new SignalHandler() {
      @Override
      public void handle(Signal sig) {
        if (sigName.equals(sig.getName())) {
          final Randomizer rand;
          int step = sync.get();
          switch (step % 3) {
            case 0:
              rand = new RandoTwo();
              break;
            case 1:
              rand = new RandoThree();
              break;
            default:
              rand = null;
              break;
          }
          if (step >= 2) {
            if (rand == null) {
              for (int i = 0; i < randoms.length; ++i) {
                randoms[i] = new RandoOne();
              }
              System.out.println("setting all to RandoOne");
            } else {
              int idx = step % randoms.length;
              randoms[idx] = rand;
              System.out.printf("setting randoms[%d] = %s%n", idx, rand.getClass().getSimpleName());
            }
          }
          sync.incrementAndGet();
        }
      }
    };
    Signal.handle(new Signal(sigName), handler);
  }
}
