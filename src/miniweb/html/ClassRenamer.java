package miniweb.html;

import cz.vutbr.web.css.CSSException;
import cz.vutbr.web.css.CSSFactory;
import cz.vutbr.web.css.StyleSheet;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import miniweb.css.CssClassRenamer;
import miniweb.css.StylesheetExtractor;
import org.jsoup.nodes.DataNode;
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
        } else if (node instanceof DataNode
                && node.parent() instanceof Element
                && "style".equals(((Element) node.parent()).tagName())) { // Inline CSS
            try {
                DataNode dataNode = (DataNode) node;
                StyleSheet style = CSSFactory.parseString(dataNode.getWholeData(), null, StylesheetExtractor.localNetworkProcessor);
                CssClassRenamer.renameCssClasses(newNames, Collections.singletonList(style));
                dataNode.setWholeData(style.toString());
            } catch (IOException | CSSException ex) {
                throw new InternalError(ex);
            }
        }
    }

    @Override
    public void tail(Node node, int depth) {
        // No action needed
    }
}
