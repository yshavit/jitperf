import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import sun.misc.Signal;
import sun.misc.SignalHandler;

public class JitPerfMain {
  public static final long LOOPS_PER_EXPERIMENT = 1000000000;
  
  public static void main(String[] args) {
    Randomizer[] randoms = new Randomizer[3];
    for (int i = 0; i < randoms.length; ++i) {
      randoms[i] = new RandoOne();
    }
    
    AtomicReference<Randomizer[]> randomsRef = new AtomicReference<Randomizer[]>(randoms);
    setUpHandler(randomsRef);
    
    long seed = System.nanoTime();    
    while (true) {
      seed = runExperiment(seed, randomsRef);
      
    }
  }

  private static long runExperiment(long seed, AtomicReference<Randomizer[]> randomizersRef) {
    Randomizer[] randomizers = randomizersRef.get();
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

  private static void setUpHandler(final AtomicReference<Randomizer[]> randoms) {
    final String sigName = "USR2";
    SignalHandler handler = new SignalHandler() {
      @Override
      public void handle(Signal sig) {
        if (!sigName.equals(sig.getName())) {
          return;
        }
        FileReader reader = null;
        try {
          reader = new FileReader("__jpm_message.txt");
          BufferedReader bf = new BufferedReader(reader);
          String command = bf.readLine();
          List<Randomizer> instances = new ArrayList<Randomizer>();
          for (String line = bf.readLine(); line != null; line = bf.readLine()) {
            Class<?> clazz = Class.forName(line);
            Randomizer r = (Randomizer) clazz.newInstance();
            instances.add(r);
          }
          if ("install".equalsIgnoreCase(command)) {
            Randomizer[] randos = instances.toArray(new Randomizer[instances.size()]);
            randoms.set(randos);
            System.out.println("installing " + instances);
          } else {
            System.out.println("loading " + instances);
          }
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          try {
            if (reader != null) {
              reader.close();
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    };
    Signal.handle(new Signal(sigName), handler);
  }
}
