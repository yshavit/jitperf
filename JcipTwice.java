public class JcipTwice implements Randomizer {

  static {
    System.out.println("Loading JcipTwice");
  }
  
  @Override
  public long randomize(long seed) {
    seed ^= seed << 21;
    seed ^= seed >>> 35;
    seed ^= (seed << 4);
    seed ^= seed << 21;
    seed ^= seed >>> 35;
    seed ^= (seed << 4);
    return seed;
  }
}
