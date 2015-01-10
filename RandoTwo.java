public class RandoTwo implements Randomizer {

  static {
    System.out.println("Loading RandoTwo");
  }
  
  @Override
  public long randomize(long seed) {
    seed ^= seed << 21;
    seed ^= seed >>> 35;
    seed ^= (seed << 4);
    return seed;
  }
}
