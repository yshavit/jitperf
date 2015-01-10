public class RandoThree extends AbstractRandomizer {

  static {
    System.out.println("Loading RandoThree");
  }
  
  @Override
  public long randomize(long seed) {
    seed ^= seed << 13;
    seed ^= seed >>> 17;
    seed ^= (seed << 5);
    return seed;
  }
}
