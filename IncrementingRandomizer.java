public class IncrementingRandomizer implements Randomizer {
  static {
    System.out.println("Loading IncrementingRandomizer");
  }
    
  @Override
  public long randomize(long seed) {
    return seed + 1;
  }
}
