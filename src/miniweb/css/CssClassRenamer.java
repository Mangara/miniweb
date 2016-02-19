/*
 */
package miniweb.css;

import cz.vutbr.web.css.CombinedSelector;
import cz.vutbr.web.css.RuleSet;
import cz.vutbr.web.css.Selector;
import cz.vutbr.web.css.StyleSheet;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Sander Verdonschot <sander.verdonschot at gmail.com>
 */
public class CssClassRenamer extends RuleSetVisitor {

    private final Map<String, String> newNames;

    public static void renameCssClasses(Map<String, String> newNames, List<StyleSheet> stylesheets) {
        (new CssClassRenamer(newNames)).processStyleSheets(stylesheets);
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
                        String newName = newNames.get(name);

                        if (newName == null) {
                            if (newNames.values().contains(name)) {
                                System.err.println("The following classname occurs only in stylesheets (not in the HTML) and conflicts with one of the compressed names:" + name);
                            }
                        } else {
                            classSelector.setClassName(newNames.get(classSelector.getClassName()));
                        }
                    }
                }
            }
        }
    }
    
}
