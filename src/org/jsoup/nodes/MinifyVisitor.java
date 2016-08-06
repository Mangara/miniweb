package org.jsoup.nodes;

import java.util.regex.Pattern;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

/**
 *
 * @author Sander Verdonschot <sander.verdonschot at gmail.com>
 */
public class MinifyVisitor implements NodeVisitor {

    private final StringBuilder sb;
    private final Document.OutputSettings out;

    public static String minify(Document doc) {
        StringBuilder minified = new StringBuilder();
        NodeTraversor minifyTraversor = new NodeTraversor(new MinifyVisitor(minified));

        for (Node childNode : doc.childNodes()) {
            minifyTraversor.traverse(childNode);
        }

        return minified.toString();
    }

    private MinifyVisitor(StringBuilder sb) {
        this.sb = sb;
        out = new Document.OutputSettings();
        out.prettyPrint(false);
    }

    @Override
    public void head(Node node, int depth) {
        if (node instanceof Comment) {
            // Print nothing
        } else if (node instanceof DataNode) {
            // This could be inline CSS or JavaScript
            // We should use other minifiers for this
            // For now, just print the contents as is
            sb.append(((DataNode) node).getWholeData());
        } else if (node instanceof DocumentType) {
            // No special printing necessary
            node.outerHtmlHead(sb, 0, out);
        } else if (node instanceof Element) {
            Element e = (Element) node;

            sb.append("<").append(e.tagName());

            minifyAttributes(e.attributes());

            // If the last attribute was unquoted and ended with '/', we need a space before the end '>'
            if (sb.charAt(sb.length() - 1) == '/') {
                sb.append(' ');
            }

            sb.append(">");
        } else if (node instanceof TextNode) {
            boolean normaliseWhite = node.parent() instanceof Element && !Element.preserveWhitespace(node.parent());

            boolean inHead = inHead(node);
            boolean stripLeadingWhite = normaliseWhite && (inHead || node.previousSibling() == null);
            boolean stripTrailingWhite = normaliseWhite && (inHead || node.nextSibling() == null);

            Entities.escape(sb, ((TextNode) node).getWholeText(), out, false, normaliseWhite, stripLeadingWhite);

            while (stripTrailingWhite && sb.charAt(sb.length() - 1) == ' ') {
                sb.deleteCharAt(sb.length() - 1);
            }
        } else if (node instanceof XmlDeclaration) {
            // No special printing necessary
            node.outerHtmlHead(sb, 0, out);
        } else {
            throw new AssertionError("Unexpected node type: " + node);
        }
    }

    @Override
    public void tail(Node node, int depth) {
        if (node instanceof Element) {
            Element e = (Element) node;

            if (!(e.childNodes().isEmpty() && e.tag().isSelfClosing())) {
                sb.append("</").append(e.tagName()).append(">");
            }
        }
        // Otherwise nothing to print (unexpected node types are handled at the head)
    }

    private void minifyAttributes(Attributes attr) {
        if (attr == null) {
            System.err.println("Null");
            return;
        }

        for (Attribute a : attr) {
            if (omitAttribute(a)) {
                continue;
            }

            sb.append(" ").append(a.getKey());

            if (!(a.getValue().trim().isEmpty() || a.isBooleanAttribute())) {
                StringBuilder value = new StringBuilder();
                Entities.escape(value, a.getValue(), out, true, false, false);
                
                if (trimAttribute(a)) {
                    value = new StringBuilder(value.toString().trim());
                }

                if (noQuotesRequired.matcher(value).matches()) {
                    sb.append("=").append(value);
                } else {
                    sb.append("=\"").append(value).append('"');
                }
            }
        }
    }

    private boolean omitAttribute(Attribute a) {
        // <link type="text/css">
        // <style type="text/css">
        // <script type="text/javascript">
        return false;
    }
    
    private boolean trimAttribute(Attribute a) {
        switch (a.getKey()) {
            case "tabindex":
            case "maxlength":
            case "size":
            case "rows":
            case "cols":
            case "span":
            case "rowspan":
            case "colspan":
                return a.getValue().trim().matches("\\d+");
            default:
                return false;
        }
    }

    private boolean inHead(Node node) {
        Node n = node;
        while (n != null) {
            if (n instanceof Element && ((Element) n).tagName().equals("head")) {
                return true;
            }

            n = n.parentNode();
        }
        return false;
    }

    /*
     * From the HTML 4.0.1 spec (https://www.w3.org/TR/html401/intro/sgmltut.html#h-3.2.2 ):
     *
     * In certain cases, authors may specify the value of an attribute
     * without any quotation marks. The attribute value may only contain
     * letters (a-z and A-Z), digits (0-9), hyphens (ASCII decimal 45),
     * periods (ASCII decimal 46), underscores (ASCII decimal 95), and
     * colons (ASCII decimal 58).
     *
     * The HTML 5 spec is more liberal.
     */
    private static final Pattern noQuotesRequired = Pattern.compile("[^ \t\n\r\f\"'`=<>]+");
    //Pattern.compile("[a-zA-Z0-9-._:]+");

}
