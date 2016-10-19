/*
 * Copyright 2016 Sander Verdonschot <sander.verdonschot at gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package miniweb.css;

import cz.vutbr.web.css.CombinedSelector;
import cz.vutbr.web.css.RuleSet;
import cz.vutbr.web.css.Selector;
import cz.vutbr.web.css.StyleSheet;
import java.util.Collections;
import java.util.Map;

public class CssClassRenamer extends RuleSetVisitor {

    private final Map<String, String> newNames;

    public static void renameCssClasses(Map<String, String> newNames, StyleSheet stylesheet) {
        (new CssClassRenamer(newNames)).processStyleSheets(Collections.singletonList(stylesheet));
    }

    private CssClassRenamer(Map<String, String> newNames) {
        this.newNames = newNames;
    }

    @Override
    void processRuleSet(RuleSet ruleSet) {
        for (CombinedSelector selectorList : ruleSet.getSelectors()) {
            for (Selector selector : selectorList) {
                for (Selector.SelectorPart part : selector) {
                    if (part instanceof Selector.ElementClass) {
                        Selector.ElementClass classSelector = (Selector.ElementClass) part;

                        String name = classSelector.getClassName();
                        classSelector.setClassName(newNames.getOrDefault(name, name));
                    }
                }
            }
        }
    }

}
