package miniweb.css;

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
