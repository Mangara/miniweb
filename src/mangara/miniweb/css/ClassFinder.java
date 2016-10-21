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
package mangara.miniweb.css;

import cz.vutbr.web.css.CombinedSelector;
import cz.vutbr.web.css.RuleSet;
import cz.vutbr.web.css.Selector;
import cz.vutbr.web.css.StyleSheet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClassFinder extends RuleSetVisitor {

    private final Set<String> classes;

    public static Set<String> getAllClasses(List<StyleSheet> stylesheets) {
        ClassFinder finder = new ClassFinder();
        finder.processStyleSheets(stylesheets);
        return finder.classes;
    }

    private ClassFinder() {
        classes = new HashSet<>();
    }

    @Override
    void processRuleSet(RuleSet ruleSet) {
        for (CombinedSelector selectorList : ruleSet.getSelectors()) {
            for (Selector selector : selectorList) {
                for (Selector.SelectorPart part : selector) {
                    if (part instanceof Selector.ElementClass) {
                        classes.add(((Selector.ElementClass) part).getClassName());
                    }
                }
            }
        }
    }

}
