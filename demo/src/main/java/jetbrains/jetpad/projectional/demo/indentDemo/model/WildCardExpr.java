package jetbrains.jetpad.projectional.demo.indentDemo.model;

public class WildCardExpr extends Expr {
  @Override
  public Expr copy() {
    return new WildCardExpr();
  }

  @Override
  public String toString() {
    return "?";
  }
}
