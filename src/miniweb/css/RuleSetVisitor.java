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

import cz.vutbr.web.css.RuleBlock;
import cz.vutbr.web.css.RuleMedia;
import cz.vutbr.web.css.RuleSet;
import cz.vutbr.web.css.StyleSheet;
import java.util.List;

public abstract class RuleSetVisitor {
    abstract void processRuleSet(RuleSet ruleSet);
    
    public void processStyleSheets(List<StyleSheet> stylesheets) {
        for (StyleSheet stylesheet : stylesheets) {
            for (RuleBlock<?> rules : stylesheet) {
                if (rules instanceof RuleSet) {
                    processRuleSet((RuleSet) rules);
                } else if (rules instanceof RuleMedia) {
                    for (RuleSet set : (RuleMedia) rules) {
                        processRuleSet(set);
                    }
                } else {
                    System.err.println("Unexpected RuleBlock type: " + rules);
                }
            }
        }
    }
}
