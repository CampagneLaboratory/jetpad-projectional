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
package jetbrains.jetpad.projectional.testApp.model;

import jetbrains.jetpad.model.children.ChildProperty;

public abstract class BinExpr extends Expr {
  public final ChildProperty<ExprNode, Expr> left = new ChildProperty<ExprNode, Expr>(this);
  public final ChildProperty<ExprNode, Expr> right = new ChildProperty<ExprNode, Expr>(this);

  protected abstract String getSign();

  @Override
  public String toString() {
    return "(" + left.get() + " '" + getSign() + "' " + right.get() + ")";
  }
}