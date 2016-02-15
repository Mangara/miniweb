/*
 */
package miniweb.css;

import cz.vutbr.web.css.RuleBlock;
import cz.vutbr.web.css.RuleMedia;
import cz.vutbr.web.css.RuleSet;
import cz.vutbr.web.css.StyleSheet;
import java.util.List;

/**
 *
 * @author Sander Verdonschot <sander.verdonschot at gmail.com>
 */
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
