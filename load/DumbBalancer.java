package load;

public class DumbBalancer extends LoadBalancer {

  @Override
  public boolean checkLoad() {
    return true;
  }
}
