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
package jetbrains.jetpad.projectional.util;

import jetbrains.jetpad.base.Registration;
import jetbrains.jetpad.base.Value;
import jetbrains.jetpad.cell.Cell;
import jetbrains.jetpad.cell.CellContainer;
import jetbrains.jetpad.cell.CellPropertySpec;
import jetbrains.jetpad.cell.TextCell;
import jetbrains.jetpad.cell.position.PositionHandler;
import jetbrains.jetpad.cell.trait.CellTrait;
import jetbrains.jetpad.cell.util.Cells;
import jetbrains.jetpad.event.Key;
import jetbrains.jetpad.event.KeyEvent;
import jetbrains.jetpad.event.KeyStrokeSpecs;
import jetbrains.jetpad.event.MouseEvent;
import jetbrains.jetpad.geometry.Rectangle;
import jetbrains.jetpad.geometry.Vector;
import jetbrains.jetpad.model.composite.Composites;
import jetbrains.jetpad.model.composite.CompositesWithBounds;
import jetbrains.jetpad.model.event.CompositeRegistration;
import jetbrains.jetpad.model.event.EventHandler;
import jetbrains.jetpad.model.property.*;

import java.util.Stack;

import static jetbrains.jetpad.model.composite.Composites.nextFocusable;
import static jetbrains.jetpad.model.composite.Composites.prevFocusable;

public class CellNavigationController {
  public static final CellPropertySpec<Cell> PAIR_CELL = new CellPropertySpec<>("pairCell");


  private CompositesWithBounds ourWithBounds = new CompositesWithBounds(2);

  static Registration install(final CellContainer container) {
    final CellNavigationController controller = new CellNavigationController(container);
    return controller.install();
  }

  private CellContainer myContainer;
  private Value<Integer> myPrevXOffset = new Value<>(null);
  private Value<Boolean> myStackResetEnabled = new Value<>(true);
  private Stack<Cell> mySelectionStack = new Stack<>();


  private CellNavigationController(final CellContainer container) {
    myContainer = container;
  }

  public CompositeRegistration install() {
    CompositeRegistration result = new CompositeRegistration();
    result.add(
      selectedCaretOffset().addHandler(new EventHandler<PropertyChangeEvent<Integer>>() {
        @Override
        public void onEvent(PropertyChangeEvent<Integer> event) {
          myPrevXOffset.set(null);
        }
      }),
      focusedCell().addHandler(new EventHandler<PropertyChangeEvent<Cell>>() {
        @Override
        public void onEvent(PropertyChangeEvent<Cell> event) {
          //todo we should reset the stuff on every view structure change
          if (myStackResetEnabled.get()) {
            mySelectionStack.clear();
          }
        }
      }));
    result.add(myContainer.root.addTrait(new CellTrait() {
      @Override
      public void onKeyPressed(Cell cell, KeyEvent event) {
        handleKeyPress(cell, event);
        if (event.isConsumed()) return;
        super.onKeyPressed(cell, event);
      }

      @Override
      public void onMousePressed(Cell cell, MouseEvent event) {
        handleMousePress(event);
        if (event.isConsumed()) return;
        super.onMousePressed(cell, event);
      }
    }));
    result.add(myContainer.focusedCell.addHandler(new EventHandler<PropertyChangeEvent<Cell>>() {
      Cell myPair;

      @Override
      public void onEvent(PropertyChangeEvent<Cell> event) {
        Cell oldCell = event.getOldValue();
        if (oldCell != null) {
          oldCell.focusHighlighted().set(false);
          if (myPair != null) {
            oldCell.pairHighlighted().set(false);
            myPair.pairHighlighted().set(false);
            myPair.focusHighlighted().set(false);
            myPair = null;
          }
        }

        Cell newCell = event.getNewValue();
        if (newCell != null) {
          newCell.focusHighlighted().set(true);
          Cell pair = newCell.get(PAIR_CELL);
          if (pair != null) {
            newCell.pairHighlighted().set(true);
            pair.pairHighlighted().set(true);
            pair.focusHighlighted().set(true);
            myPair = pair;
          }
        }
      }
    }));

    return result;
  }

  private Property<Cell> focusedCell() {
    return myContainer.focusedCell;
  }

  private void scrollTo(Cell cell) {
    if (cell.getParent() != null) {
      Cell parent = cell.getParent();

      Rectangle parentBounds = parent.getBounds();
      Rectangle cellBounds = cell.getBounds();

      Vector delta = new Vector(10, 10);

      Rectangle bounds = new Rectangle(
        cellBounds.origin.sub(parentBounds.origin.add(delta)),
        cellBounds.dimension.add(delta.mul(2))
      );

      parent.scrollTo(bounds.intersect(new Rectangle(Vector.ZERO, parent.getBounds().dimension)));
    } else {
      cell.scrollTo();
    }
  }

  private Cell root() {
    return myContainer.root;
  }

  private void moveCaretTo(Cell cell, int offset) {
    cell.get(PositionHandler.PROPERTY).caretOffset().set(offset);
  }

  private Selector<Cell, ReadableProperty<Integer>> caretPositionSelector() {
    return new Selector<Cell, ReadableProperty<Integer>>() {
      @Override
      public ReadableProperty<Integer> select(Cell source) {
        return source.get(PositionHandler.PROPERTY).caretOffset();
      }
    };
  }

  private void moveToHome(Cell next) {
    if (next != null) {
      next.get(PositionHandler.PROPERTY).home();
    }
  }

  private void moveToEnd(Cell next) {
    if (next != null) {
      next.get(PositionHandler.PROPERTY).end();
    }
  }

  private int selectedXOffset() {
    return selectedCaretOffset().get() + focusedCell().get().getBounds().origin.x;
  }

  private ReadableProperty<Integer> selectedCaretOffset() {
    return Properties.select(focusedCell(), caretPositionSelector());
  }

  protected void handleKeyPress(Cell cell, KeyEvent event) {
    Cell current = focusedCell().get();
    Integer currentOffset = null;

    if (event.is(Key.UP) || event.is(Key.DOWN) || event.is(Key.PAGE_UP) || event.is(Key.PAGE_DOWN)) {
      currentOffset = myPrevXOffset.get();
      if (currentOffset == null) {
        currentOffset = selectedXOffset();
      }
    }

    Rectangle visibleRect = focusedCell().get().getContainer().visibleRect();

    Cell next = null;
    boolean restoreOffset = false;

    if (event.is(Key.RIGHT)) {
      next = nextFocusable(current);
      moveToHome(next);
    } else if (event.is(KeyStrokeSpecs.NEXT_WORD)) {
      next = nextFocusable(current);
      if (next != null) {
        moveToHome(next);
      } else if (!current.get(PositionHandler.PROPERTY).isEnd()) {
        next = current;
        moveToEnd(next);
      }
    } else if (event.is(Key.LEFT)) {
      next = prevFocusable(current);
      moveToEnd(next);
    } else if (event.is(KeyStrokeSpecs.PREV_WORD)) {
      next = prevFocusable(current);
      moveToHome(next);
    } else if (event.is(Key.UP)) {
      next = ourWithBounds.upperFocusable(current, currentOffset);
      restoreOffset = true;
    } else if (event.is(Key.DOWN)) {
      next = ourWithBounds.lowerFocusable(current, currentOffset);
      restoreOffset = true;
    } else if (event.is(Key.PAGE_UP)) {
      next = new PageUpDown(current, currentOffset, visibleRect.dimension.y) {
        @Override
        protected Cell next(Cell current, int offset) {
          return ourWithBounds.upperFocusable(current, offset);
        }
      }.execute();
      restoreOffset = true;
    } else if (event.is(Key.PAGE_DOWN)) {
      next = new PageUpDown(current, currentOffset, visibleRect.dimension.y) {
        @Override
        protected Cell next(Cell current, int offset) {
          return ourWithBounds.lowerFocusable(current, offset);
        }
      }.execute();
      restoreOffset = true;
    } else if (event.is(KeyStrokeSpecs.HOME)) {
      next = ourWithBounds.homeElement(current);
      moveToHome(next);
    } else if (event.is(KeyStrokeSpecs.END)) {
      next = ourWithBounds.endElement(current);
      moveToEnd(next);
    } else if (event.is(KeyStrokeSpecs.FILE_HOME)) {
      next = Composites.firstFocusable(cell, true);
      moveToHome(next);
    } else if (event.is(KeyStrokeSpecs.FILE_END)) {
      next = Composites.lastFocusable(cell, true);
      moveToEnd(next);
    } else if (event.is(KeyStrokeSpecs.SELECT_UP)) {
      Cell focusableParent = Composites.focusableParent(current);
      if (focusableParent != null) {
        mySelectionStack.push(current);
        next = focusableParent;
        myStackResetEnabled.set(false);
      }
    } else if (event.is(KeyStrokeSpecs.SELECT_DOWN)) {
      if (mySelectionStack.isEmpty()) {
        next = Composites.firstFocusable(current, false);
      } else {
        next = mySelectionStack.pop();
      }
      myStackResetEnabled.set(false);
    } else if (event.is(KeyStrokeSpecs.MATCHING_CONSTRUCTS) && current.get(PAIR_CELL) != null) {
      next = current.get(PAIR_CELL);
      if (Composites.isBefore(next, current)) {
        moveToHome(next);
      } else {
        moveToEnd(next);
      }
      restoreOffset = false;
    }
    if (next != null) {
      focusedCell().set(next);

      if (restoreOffset) {
        moveCaretTo(next, currentOffset - next.getBounds().origin.x);
        myPrevXOffset.set(currentOffset);
      }

      if (next instanceof TextCell) {
        ((TextCell) next).scrollToCaret();
      } else {
        scrollTo(next);
      }

      event.consume();
    }
    myStackResetEnabled.set(true);
  }

  protected void handleMousePress(MouseEvent event) {
    Cell closest = findFocusableAt(event.getLocation());
    if (closest == null) {
      closest = Cells.findClosestFocusableToSide(root(), event.getLocation());
    }
    if (closest != null) {
      focusedCell().set(closest);
      if (event.getX() < closest.getBounds().origin.x) {
        moveToHome(closest);
      } else {
        moveToEnd(closest);
      }
      event.consume();
    }
  }

  private Cell findFocusableAt(Vector loc) {
    Cell cell = myContainer.findCell(root(), loc);
    if (isTrivial(cell) || !cell.focusable().get()) {
      return null;
    }
    return cell;
  }

  private boolean isTrivial(Cell cell) {
    return cell == null || cell == root();
  }

  private abstract class PageUpDown {
    private Cell myCell;
    private int myOffset;
    private int myPageHeight;

    PageUpDown(Cell cell, int offset, int pageHeight) {
      myCell = cell;
      myOffset = offset;
      myPageHeight = pageHeight;
    }

    protected abstract Cell next(Cell current, int offset);

    Cell execute() {
      Cell current = myCell;
      while (true) {
        Cell next = next(current, myOffset);

        if (next == null) {
          if (next != myCell) {
            return current;
          } else {
            return null;
          }
        }

        if (myCell.getBounds().distance(next.origin()) >= myPageHeight) {
          return next;
        }

        current = next;
      }
    }
  }
}