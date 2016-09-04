package miniweb.html;

import cz.vutbr.web.css.CombinedSelector;
import cz.vutbr.web.css.RuleBlock;
import cz.vutbr.web.css.RuleMedia;
import cz.vutbr.web.css.RuleSet;
import cz.vutbr.web.css.Selector;
import cz.vutbr.web.css.StyleSheet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

public class ClassRenamer implements NodeVisitor {

    private final Map<String, String> newNames;

    public static void renameHtmlClasses(Document doc, Map<String, String> newNames) {
        new NodeTraversor(new ClassRenamer(newNames)).traverse(doc);
    }

    private ClassRenamer(Map<String, String> newNames) {
        this.newNames = newNames;
    }

    @Override
    public void head(Node node, int depth) {
        // TODO: fix classes in inline CSS
        if (node instanceof Element) {
            Element e = (Element) node;

            // Fix classes
            if (e.hasAttr("class")) {
                e.classNames(
                        e.classNames().stream()
                        .map(s -> newNames.get(s))
                        .collect(Collectors.toSet())
                );
            }
        }
    }

    @Override
    public void tail(Node node, int depth) {
        // No action needed
    }

    static void renameCssClasses(List<StyleSheet> stylesheets, Map<String, String> newNames) {
        for (StyleSheet stylesheet : stylesheets) {
            for (RuleBlock<?> rules : stylesheet) {
                if (rules instanceof RuleSet) {
                    processRuleSet((RuleSet) rules, newNames);
                } else if (rules instanceof RuleMedia) {
                    for (RuleSet set : (RuleMedia) rules) {
                        processRuleSet(set, newNames);
                    }
                } else {
                    System.err.println("Unexpected RuleBlock type: " + rules);
                }
            }
        }
    }

    private static void processRuleSet(RuleSet ruleSet, Map<String, String> newNames) {
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
