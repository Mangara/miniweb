package miniweb.html;

import java.util.Map;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

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
