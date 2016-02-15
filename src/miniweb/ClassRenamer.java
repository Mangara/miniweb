/*
 */
package miniweb;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

/**
 *
 * @author Sander Verdonschot <sander.verdonschot at gmail.com>
 */
public class ClassRenamer implements NodeVisitor {

    private final Map<String, String> newNames;

    public static void renameClasses(Document doc, Map<String, String> newNames) {
        new NodeTraversor(new ClassRenamer(newNames)).traverse(doc);
    }

    private ClassRenamer(Map<String, String> newNames) {
        this.newNames = newNames;
    }

    @Override
    public void head(Node node, int depth) {
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

}
