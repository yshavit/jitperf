public class RandoThree implements Randomizer {

  static {
    System.out.println("Loading JcipDifferent");
  }
  
  @Override
  public long randomize(long seed) {
    seed ^= seed << 13;
    seed ^= seed >>> 17;
    seed ^= (seed << 5);
    return seed;
  }
}
