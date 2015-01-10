public class RandoOne implements Randomizer {
  static {
    System.out.println("Loading IncrementingRandomizer");
  }
    
  @Override
  public long randomize(long seed) {
    seed ^= seed << 11;
    seed ^= seed >>> 19;
    seed ^= (seed << 8);
    return seed;
  }
}
