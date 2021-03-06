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
package jetbrains.jetpad.cell;

import jetbrains.jetpad.geometry.Rectangle;
import jetbrains.jetpad.geometry.Vector;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.values.Color;
import jetbrains.jetpad.values.Font;
import jetbrains.jetpad.values.FontFamily;

public class TextCell extends Cell {
  public static final Font DEFAULT_FONT = new Font(FontFamily.MONOSPACED, 15);

  public static final CellPropertySpec<String> TEXT = new CellPropertySpec<>("text", "");
  public static final CellPropertySpec<Color> TEXT_COLOR = new CellPropertySpec<>("textColor", Color.BLACK);
  public static final CellPropertySpec<Boolean> CARET_VISIBLE = new CellPropertySpec<>("caretVisible", false);
  public static final CellPropertySpec<Integer> CARET_POSITION = new CellPropertySpec<>("caretPosition", 0);

  public static final CellPropertySpec<FontFamily> FONT_FAMILY = new CellPropertySpec<>("fontFamily", DEFAULT_FONT.getFamily());
  public static final CellPropertySpec<Boolean> BOLD = new CellPropertySpec<>("bold", DEFAULT_FONT.isBold());
  public static final CellPropertySpec<Integer> FONT_SIZE = new CellPropertySpec<>("fontSize", DEFAULT_FONT.getSize());

  public static final CellPropertySpec<Boolean> SELECTION_VISIBLE = new CellPropertySpec<>("selectionVisible", false);
  public static final CellPropertySpec<Integer> SELECTION_START = new CellPropertySpec<>("selectionStart", 0);

  public TextCell() {
  }

  public TextCell(String text) {
    text().set(text);
  }

  public Property<String> text() {
    return getProp(TEXT);
  }

  public Property<Color> textColor() {
    return getProp(TEXT_COLOR);
  }

  public Property<Boolean> caretVisible() {
    return getProp(CARET_VISIBLE);
  }

  public Property<Boolean> bold() {
    return getProp(BOLD);
  }

  public Property<FontFamily> fontFamily() {
    return getProp(FONT_FAMILY);
  }

  public Property<Integer> fontSize() {
    return getProp(FONT_SIZE);
  }

  public Property<Integer> caretPosition() {
    return getProp(CARET_POSITION);
  }

  public Property<Boolean> selectionVisible() {
    return getProp(SELECTION_VISIBLE);
  }

  public Property<Integer> selectionStart() {
    return getProp(SELECTION_START);
  }

  public boolean isEnd() {
    return caretPosition().get() == lastPosition();
  }

  public boolean isHome() {
    return caretPosition().get() == 0;
  }

  public int lastPosition() {
    return text().get() == null ? 0 : text().get().length();
  }

  public int getCaretAt(int x) {
    return getPeer().getCaretAt(this, x);
  }

  public int getCaretOffset(int caret) {
    return getPeer().getCaretOffset(this, caret);
  }

  public void scrollToCaret() {
    int delta = 50;
    int offset = getCaretOffset(caretPosition().get());
    Rectangle bounds = getBounds();
    scrollTo(new Rectangle(offset - delta, 0, 2 * delta, bounds.dimension.y).intersect(new Rectangle(Vector.ZERO, bounds.dimension)));
  }

  @Override
  public String toString() {
    return "TextCell('" + text().get() + "')@" + Integer.toHexString(hashCode());
  }
}