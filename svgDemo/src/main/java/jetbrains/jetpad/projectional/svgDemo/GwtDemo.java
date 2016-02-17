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
package jetbrains.jetpad.projectional.svgDemo;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Element;
import elemental.client.Browser;
import elemental.dom.Document;
import jetbrains.jetpad.projectional.view.toGwt.ViewToDom;

public class GwtDemo implements EntryPoint {
  public void onModuleLoad() {
    Document doc = Browser.getDocument();
    ViewToDom.map(DemoModel.demoViewContainer(), (Element) doc.getElementById("svg"));
  }
}