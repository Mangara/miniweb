/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package miniweb;

import java.util.Map;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

/**
 *
 * @author Sander Verdonschot <sander.verdonschot at gmail.com>
 */
public class ClassCleaner implements NodeVisitor {

    private final Map<Element, Set<String>> referencedByClass;

    public static void removeUnreferencedClasses(Document doc, Map<Element, Set<String>> referencedByClass) {
        new NodeTraversor(new ClassCleaner(referencedByClass)).traverse(doc);
    }

    private ClassCleaner(Map<Element, Set<String>> referencedByClass) {
        this.referencedByClass = referencedByClass;
    }

    @Override
    public void head(Node node, int depth) {
        if (node instanceof Element) {
            Element e = (Element) node;

            // Fix classes
            Set<String> classes = referencedByClass.get(e);

            if (classes == null) {
                e.removeAttr("class");
            } else {
                e.classNames(classes);
            }
        }
    }

    @Override
    public void tail(Node node, int depth) {
    }
}
