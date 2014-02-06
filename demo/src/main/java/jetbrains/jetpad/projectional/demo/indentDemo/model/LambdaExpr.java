/*
 * Copyright 2012-2014 JetBrains s.r.o
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
package jetbrains.jetpad.projectional.demo.indentDemo.model;

import jetbrains.jetpad.model.children.ChildProperty;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.ValueProperty;

public class LambdaExpr extends Expr {
  public final Property<String> varName = new ValueProperty<String>();
  public final Property<Expr> body = new ChildProperty<LambdaExpr, Expr>(this);

  @Override
  public Expr copy() {
    LambdaExpr result = new LambdaExpr();
    result.varName.set(varName.get());
    result.body.set(copy(body.get()));
    return result;
  }

  @Override
  public String toString() {
    return "(lambda " + varName.get() + " -> " + body.get() + ")";
  }
}