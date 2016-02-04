/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package miniweb;

import java.util.Collections;
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

            String before = printElement(e);

            // Fix classes
            Set<String> classes = referencedByClass.get(e);

            if (classes == null) {
                e.removeAttr("class");
            } else {
                e.classNames(classes);
            }

            String after = printElement(e);

            if (!before.equals(after)) {
                System.out.println("Before: " + before);
                System.out.println("After:  " + after);
            }
        }
    }

    @Override
    public void tail(Node node, int depth) {
    }

    private String printElement(Element e) {
        String text = e.toString();
        int eol = text.indexOf('\n');

        if (eol >= 0) {
            return text.substring(0, eol);
        } else {
            return text;
        }
    }
}
