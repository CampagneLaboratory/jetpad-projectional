package jetbrains.jetpad.projectional.svg.toAwt;

import jetbrains.jetpad.mapper.Synchronizers;
import jetbrains.jetpad.model.property.WritableProperty;
import jetbrains.jetpad.projectional.svg.SvgRect;
import jetbrains.jetpad.values.Color;
import org.apache.batik.dom.AbstractDocument;
import org.apache.batik.dom.svg.SVGOMRectElement;

public class SvgRectMapper extends SvgElementMapper<SvgRect, SVGOMRectElement> {
  public SvgRectMapper(SvgRect source, SVGOMRectElement target, AbstractDocument doc) {
    super(source, target, doc);
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);

    conf.add(Synchronizers.forPropsOneWay(getSource().x, new WritableProperty<Double>() {
      @Override
      public void set(Double value) {
        getTarget().setAttribute("x", Double.toString(value));
      }
    }));
    conf.add(Synchronizers.forPropsOneWay(getSource().y, new WritableProperty<Double>() {
      @Override
      public void set(Double value) {
        getTarget().setAttribute("y", Double.toString(value));
      }
    }));
    conf.add(Synchronizers.forPropsOneWay(getSource().height, new WritableProperty<Double>() {
      @Override
      public void set(Double value) {
        getTarget().setAttribute("height", Double.toString(value));
      }
    }));
    conf.add(Synchronizers.forPropsOneWay(getSource().width, new WritableProperty<Double>() {
      @Override
      public void set(Double value) {
        getTarget().setAttribute("width", Double.toString(value));
      }
    }));
    conf.add(Synchronizers.forPropsOneWay(getSource().fill, new WritableProperty<Color>() {
      @Override
      public void set(Color value) {
        getTarget().setAttribute("fill", value.toCssColor());
      }
    }));
  }
}