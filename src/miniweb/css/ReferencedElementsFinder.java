package miniweb.css;

import cz.vutbr.web.css.CombinedSelector;
import cz.vutbr.web.css.RuleSet;
import cz.vutbr.web.css.Selector;
import cz.vutbr.web.css.StyleSheet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class ReferencedElementsFinder extends RuleSetVisitor {

    private final Map<Element, Set<String>> elementsReferencedByClass;
    private final Document doc;

    public static Map<Element, Set<String>> getReferencedElements(Document doc, List<StyleSheet> stylesheets) {
        ReferencedElementsFinder finder = new ReferencedElementsFinder(doc);
        finder.processStyleSheets(stylesheets);
        return finder.getElementsReferencedByClass();
    }

    public Map<Element, Set<String>> getElementsReferencedByClass() {
        return elementsReferencedByClass;
    }

    public ReferencedElementsFinder(Document doc) {
        this.doc = doc;
        elementsReferencedByClass = new HashMap<>();
    }

    @Override
    void processRuleSet(RuleSet ruleSet) {
        for (CombinedSelector selectorList : ruleSet.getSelectors()) {
            StringBuilder select = new StringBuilder();

            for (Selector selector : selectorList) {
                select.append(selector.getCombinator() == null ? "" : selector.getCombinator().value());

                for (Selector.SelectorPart part : selector) {
                    select.append(part);

                    if (part instanceof Selector.ElementClass) {
                        String className = ((Selector.ElementClass) part).getClassName();

                        for (Element e : doc.select(select.toString())) {
                            Set<String> classes = elementsReferencedByClass.get(e);

                            if (classes == null) {
                                /**
                                 * clone() is necessary here, because an
                                 * Element's hash and equals are based on the
                                 * content. Two divs with the same text will be
                                 * equal() and have the same hascode, thus will
                                 * only be stored once in the map. But when we
                                 * are changing the classes afterwards, we
                                 * change the first element first, which means
                                 * it is no longer equal() to the second
                                 * element, which will then not be found in the
                                 * map. By using clone() we do not change the
                                 * reference element, ensuring that all elements
                                 * are changed properly.
                                 */
                                elementsReferencedByClass.put(e.clone(), new HashSet<>(Collections.singleton(className)));
                            } else {
                                classes.add(className);
                            }
                        }
                    }
                }
            }
        }
    }

}
