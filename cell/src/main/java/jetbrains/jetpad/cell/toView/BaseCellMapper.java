/*
 * Copyright 2012-2016 JetBrains s.r.o
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrains.jetpad.cell.toView;

import jetbrains.jetpad.base.Registration;
import jetbrains.jetpad.cell.Cell;
import jetbrains.jetpad.cell.CellPropertySpec;
import jetbrains.jetpad.cell.mappersUtil.*;
import jetbrains.jetpad.geometry.Rectangle;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.mapper.MappingContext;
import jetbrains.jetpad.model.collections.list.ObservableList;
import jetbrains.jetpad.model.event.EventHandler;
import jetbrains.jetpad.model.property.PropertyChangeEvent;
import jetbrains.jetpad.projectional.view.View;
import jetbrains.jetpad.values.Color;

import java.util.Collection;
import java.util.List;

class BaseCellMapper<SourceT extends Cell, TargetT extends View> extends Mapper<SourceT, TargetT> implements HasCounters, EventHandler<PropertyChangeEvent<Cell>> {
  private CellToViewContext myContext;
  private Counters myCounters;
  private PopupManager myPopupManager;
  private List<Mapper<?, ?>> myChildMappers = null;
  private Color myAncestorBackground;

  BaseCellMapper(SourceT source, TargetT target, CellToViewContext ctx) {
    super(source, target);
    if (ctx == null) {
      throw new IllegalArgumentException();
    }
    myContext = ctx;
  }

  protected CellToViewContext getContext() {
    return myContext;
  }

  @Override
  protected void onAttach(MappingContext ctx) {
    super.onAttach(ctx);

    myContext.register(this);

    if (isAutoChildManagement()) {
      myChildMappers = createChildList();
      ObservableList<Cell> children = getSource().children();
      for (int i = 0; i < children.size(); i++) {
        childAdded(i, children.get(i));
      }
    }

    myPopupManager = createPopupManager();
    myPopupManager.attach(getSource());

    refreshProperties();
  }

  @Override
  protected void onDetach() {
    getTarget().children().clear();
    myPopupManager.dispose();
    myContext.unregister(this);
    super.onDetach();
  }

  protected boolean isLeaf() {
    return false;
  }

  protected boolean isAutoChildManagement() {
    return true;
  }

  protected boolean isAutoPopupManagement() {
    return true;
  }

  @Override
  public final int getCounter(CounterSpec spec) {
    if (myCounters == null) return 0;
    return myCounters.getCounter(spec);
  }

  @Override
  public final void changeCounter(CounterSpec spec, int delta) {
    if (myCounters == null) {
      myCounters = new Counters();
    }
    myCounters.changeCounter(spec, delta);
    if (myCounters.isEmpty()) {
      myCounters = null;
    }
  }

  void childAdded(int index, Cell child) {
    if (!isAutoChildManagement()) return;
    BaseCellMapper<? extends Cell, ? extends View> mapper = myContext.apply(child);
    getTarget().children().add(index, mapper.getTarget());
    myChildMappers.add(index, mapper);
  }

  void childRemoved(int index, Cell child) {
    if (!isAutoChildManagement()) return;
    myChildMappers.remove(index);
    getTarget().children().remove(index);
  }

  @Override
  public final void onEvent(PropertyChangeEvent<Cell> event) {
    myPopupManager.onEvent(event);
  }

  void onPopupPropertyChanged(CellPropertySpec<?> prop, PropertyChangeEvent<?> change) {
    if (getParent() == null) return;
    ((BaseCellMapper<?, ?>) getParent()).myPopupManager.onPopupPropertyChanged(getSource(), prop, change);
  }

  void setAncestorBackground(Color background) {
    myAncestorBackground = background;
  }

  void refreshProperties() {
    boolean selected = getSource().get(Cell.SELECTED) || getCounter(Counters.SELECT_COUNT) > 0;
    boolean focusHighlighted = getSource().get(Cell.FOCUS_HIGHLIGHTED) || getCounter(Counters.HIGHLIGHT_COUNT) > 0;
    Color background = getSource().get(Cell.BACKGROUND);
    applyStyle(selected, focusHighlighted, (background == null ? myAncestorBackground : background));
  }

  private void applyStyle(boolean selected, boolean focusHighlighted, Color background) {
    if (selected) {
      background = CellContainerToViewMapper.SELECTION_COLOR;
    } else if (focusHighlighted) {
      background = isLeaf() ? CellContainerToViewMapper.FOCUS_HIGHLIGHT_COLOR : CellContainerToViewMapper.SELECTION_COLOR;
    } else if (getSource().get(Cell.PAIR_HIGHLIGHTED)) {
      background = CellContainerToViewMapper.PAIR_HIGHLIGHT_COLOR;
    }
    getTarget().background().set(background);

    if (getSource().get(Cell.RED_UNDERLINE)) {
      getTarget().border().set(Color.RED);
    } else if (getSource().get(Cell.YELLOW_UNDERLINE)) {
      getTarget().border().set(Color.YELLOW);
    } else {
      getTarget().border().set(getSource().get(Cell.BORDER_COLOR));
    }

    getTarget().visible().set(getSource().get(Cell.VISIBLE));
    getTarget().hasShadow().set(getSource().get(Cell.HAS_SHADOW));
  }

  protected PopupManager createPopupManager() {
    return new BasePopupManager<View>() {
      @Override
      protected Mapper<? extends Cell, ? extends View> attachPopup(Cell popup) {
        Mapper<? extends Cell, ? extends View> mapper = myContext.apply(popup);
        if (mapper == null) {
          throw new IllegalStateException("Can't create a mapper for " + popup);
        }
        getContext().popupView.children().add(mapper.getTarget());
        return mapper;
      }

      @Override
      protected void detachPopup(Mapper<? extends Cell, ? extends View> popupMapper) {
        getContext().popupView.children().remove(popupMapper.getTarget());
      }

      @Override
      protected Collection<Mapper<? extends Cell, ? extends View>> createContainer() {
        return createChildSet();
      }

      @Override
      protected Registration setPopupUpdate() {
        return getTarget().bounds().addHandler(new EventHandler<PropertyChangeEvent<Rectangle>>() {
          @Override
          public void onEvent(PropertyChangeEvent<Rectangle> event) {
            updatePopupPositions();
          }
        });
      }

      @Override
      protected PopupPositionUpdater<View> getPositionUpdater(Mapper<? extends Cell, ? extends View> popupMapper) {
        return new PopupPositioner(getTarget());
      }
    };
  }
}