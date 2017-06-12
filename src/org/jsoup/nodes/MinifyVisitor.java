/*
 * Copyright 2016 Sander Verdonschot <sander.verdonschot at gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jsoup.nodes;

import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import mangara.miniweb.js.BasicErrorReporter;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import org.mozilla.javascript.EvaluatorException;

public class MinifyVisitor implements NodeVisitor {

    private final StringBuilder sb;
    private final Document.OutputSettings out;
    private boolean textEndsWithWhitespace = true;
    private int lastTextWhitespacePos = -1;
    private final static Set<String> DISPLAYED_INLINE_ELEMENTS
            = new HashSet<>(Arrays.asList("img", "textarea", "input", "select",
                    "button", "object", "q", "iframe", "keygen",
                    "progress", "meter"
            ));

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
        out.charset("UTF-8");
    }

    @Override
    public void head(Node node, int depth) {
        if (node instanceof Comment) {
            // Print nothing
        } else if (node instanceof DataNode) {
            String data = ((DataNode) node).getWholeData().trim();
            boolean handled = false;

            // Compress inline CSS or JS
            if (node.parent() instanceof Element) {
                Element parent = (Element) node.parent();

                if (parent.tagName().equals("style")) { // CSS
                    try {
                        // Remove obsolete HTML comments
                        if (data.startsWith("<!--") && data.endsWith("-->")) {
                            data = data.substring("<!--".length(), data.length() - "-->".length()).trim();
                        }

                        if (!data.isEmpty()) {
                            CssCompressor compressor = new CssCompressor(new StringReader(data));
                            StringWriter writer = new StringWriter(data.length());
                            compressor.compress(writer, -1);
                            sb.append(writer.getBuffer());
                        }

                        handled = true;
                    } catch (IOException ex) {
                        // This should never happen - it is from the compressor reading the input, which is a StringReader.
                        throw new InternalError(ex);
                    }
                } else if (parent.tagName().equals("script") && (parent.attr("type").isEmpty() || parent.attr("type").contains("javascript") || parent.attr("type").contains("ecmascript"))) { // JS
                    try {
                        // Remove obsolete HTML comments
                        if (data.startsWith("<!--") && data.endsWith("-->")) {
                            data = data.substring("<!--".length(), data.length() - "-->".length()).trim();
                        }

                        if (!data.isEmpty()) {
                            JavaScriptCompressor compressor = new JavaScriptCompressor(new StringReader(data), new BasicErrorReporter("Embedded <script> tag"));
                            StringWriter writer = new StringWriter(data.length());

                            compressor.compress(writer,
                                    -1, //linebreakpos
                                    true, //munge
                                    false, //verbose
                                    false, //preserveAllSemiColons
                                    false //disableOptimizations
                            );

                            String compressedJS = writer.getBuffer().toString();

                            if (compressedJS.endsWith(";")) {
                                compressedJS = compressedJS.substring(0, compressedJS.length() - 1);
                            }

                            sb.append(compressedJS);
                        }

                        handled = true;
                    } catch (IOException ex) {
                        // This should never happen - it is from the compressor reading the input, which is a StringReader.
                        throw new InternalError(ex);
                    } catch (EvaluatorException ex) {
                        // JS could not be parsed - just keep it as-is
                    }
                }
            }

            if (!handled) {
                sb.append(data);
            }
        } else if (node instanceof DocumentType) {
            // No special printing necessary
            node.outerHtmlHead(sb, 0, out);
        } else if (node instanceof Element) {
            Element e = (Element) node;

            sb.append("<").append(e.tagName());

            minifyAttributes(e, e.attributes());

            // If the last attribute was unquoted and ended with '/', we need a space before the end '>'
            if (sb.charAt(sb.length() - 1) == '/') {
                sb.append(' ');
            }

            sb.append(">");

            if (e.tag().isBlock() && !e.tagName().equals("s")) { // <s> is misclassified by JSoup as block
                textEndsWithWhitespace = true;
                lastTextWhitespacePos = -1;
            }
        } else if (node instanceof TextNode) {
            String text = ((TextNode) node).getWholeText();
            boolean normaliseWhite = node.parent() instanceof Element && !Element.preserveWhitespace(node.parent());

            if (normaliseWhite) {
                if (text.matches("\\s+")) {
                    if (textEndsWithWhitespace || !inBody(node)) {
                        return; // Don't add anything and don't change textEndsWithWhitespace
                    } else {
                        sb.append(' ');
                    }
                } else {
                    boolean trim = !inBody(node) || (node.parent() instanceof Element && DISPLAYED_INLINE_ELEMENTS.contains(((Element) node.parent()).tagName()) && !"q".equals(((Element) node.parent()).tagName()));
                    boolean stripLeadingWhite = trim || (textEndsWithWhitespace && !(node.parent() instanceof Element && "q".equals(((Element) node.parent()).tagName())));
                    boolean stripTrailingWhite = trim;

                    Entities.escape(sb, text, out, false, normaliseWhite, stripLeadingWhite);

                    while (stripTrailingWhite && Character.isWhitespace(sb.charAt(sb.length() - 1))) {
                        sb.deleteCharAt(sb.length() - 1);
                    }
                }
            } else {
                Entities.escape(sb, text, out, false, false, false);
            }

            if (Character.isWhitespace(sb.charAt(sb.length() - 1))) {
                textEndsWithWhitespace = true;
                lastTextWhitespacePos = sb.length() - 1;
            } else {
                textEndsWithWhitespace = false;
                lastTextWhitespacePos = -1;
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

            if (e.tag().isBlock() && !e.tagName().equals("s")) { // <s> is misclassified by JSoup as block
                if (!Element.preserveWhitespace(e) && lastTextWhitespacePos > 0) {
                    sb.deleteCharAt(lastTextWhitespacePos);
                }

                textEndsWithWhitespace = false;
                lastTextWhitespacePos = -1;
            } else { // inline
                if (DISPLAYED_INLINE_ELEMENTS.contains(e.tagName())) {
                    textEndsWithWhitespace = false;
                    lastTextWhitespacePos = -1;
                }

                if (Element.preserveWhitespace(e)) {
                    lastTextWhitespacePos = -1;
                }
            }

            if (!(e.childNodes().isEmpty() && e.tag().isSelfClosing())) {
                sb.append("</").append(e.tagName()).append(">");
            }
        }
        // Otherwise nothing to print (unexpected node types are handled at the head)
    }

    private static final Set<String> untrimmable = Arrays.asList(
            "value", "title"
    ).stream().collect(Collectors.toSet());

    private void minifyAttributes(Element e, Attributes attr) {
        if (attr == null) {
            System.err.println("Null");
            return;
        }

        for (Attribute a : attr) {
            String val = (untrimmable.contains(a.getKey()) ? a.getValue() : a.getValue().trim());

            if (omitAttribute(e, a, val)) {
                continue;
            }

            sb.append(" ").append(a.getKey());

            if (!omitValue(e, a, val)) {
                StringBuilder value = new StringBuilder();
                Entities.escape(value, val, out, true, false, false);

                val = cleanAttributeValue(e, a, value.toString());

                if (!val.contains("'") && val.contains("&quot;")) {
                    sb.append("=\'").append(val.replaceAll("&quot;", "\"")).append('\'');
                } else if (noQuotesRequired.matcher(val).matches()) {
                    sb.append("=").append(val);
                } else {
                    sb.append("=\"").append(val).append('"');
                }
            }
        }
    }

    private static final Set<String> removeIfEmpty = Arrays.asList(
            "id", "class", "style", "lang", "dir", "value"
    ).stream().collect(Collectors.toSet());

    private boolean omitAttribute(Element e, Attribute a, String value) {
        if (value.isEmpty() && (a.getKey().startsWith("on") || removeIfEmpty.contains(a.getKey()))) {
            return true;
        }

        if (a.getKey().startsWith("on") && value.equalsIgnoreCase("javascript:")) {
            return true;
        }

        if ("draggable".equals(a.getKey()) && !("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value))) {
            return true;
        }

        if ("form".equals(e.tagName()) && "method".equals(a.getKey()) && "get".equalsIgnoreCase(value)) {
            return true;
        }

        if ("input".equals(e.tagName()) && "type".equals(a.getKey()) && "text".equalsIgnoreCase(value)) {
            return true;
        }

        if ("a".equals(e.tagName()) && "name".equals(a.getKey()) && e.attributes().hasKey("id")) {
            String id = e.attributes().get("id");

            if (id.trim().equalsIgnoreCase(value)) {
                return true;
            }
        }

        if ("script".equals(e.tagName()) && "charset".equals(a.getKey()) && !e.attributes().hasKey("src")) {
            return true;
        }

        if ("script".equals(e.tagName()) && "language".equals(a.getKey())) {
            return true;
        }

        if ("script".equals(e.tagName()) && "type".equals(a.getKey()) && "text/javascript".equalsIgnoreCase(value)) {
            return true;
        }

        if ("style".equals(e.tagName()) && "type".equals(a.getKey()) && "text/css".equalsIgnoreCase(value)) {
            return true;
        }

        if ("link".equals(e.tagName()) && "type".equals(a.getKey()) && "text/css".equalsIgnoreCase(value)) {
            return true;
        }

        if ("area".equals(e.tagName()) && "shape".equals(a.getKey())
                && ("rect".equalsIgnoreCase(value) || "rectangle".equalsIgnoreCase(value) || "default".equalsIgnoreCase(value))) {
            return true;
        }

        // Replace <meta http-equiv=Content-Type content="text/html;charset=utf-8">
        // With <meta charset=UTF-8>
        // As per https://developer.mozilla.org/en-US/docs/Web/Guide/HTML/Obsolete_things_to_avoid
        String charsetRegex = "text/html;charset=[0-9a-zA-Z-_]+";

        if ("meta".equals(e.tagName()) && "http-equiv".equals(a.getKey()) && "Content-Type".equalsIgnoreCase(value)
                && e.attributes().size() == 2 && e.attributes().hasKey("content")) {
            String content = e.attributes().get("content").replaceAll("\\s+", "");

            if (content.toLowerCase().matches(charsetRegex)) {
                sb.append(' ').append(content.substring("text/html;".length()));
                return true;
            }
        }

        if ("meta".equals(e.tagName()) && "content".equals(a.getKey()) && value.replaceAll("\\s+", "").toLowerCase().matches(charsetRegex)
                && e.attributes().size() == 2 && e.attributes().hasKey("http-equiv")) {
            return true;
        }

        return false;
    }

    private static final Set<String> booleanAttributes = Arrays.asList(
            // JSoup boolean attributes
            "allowfullscreen", "async", "autofocus", "checked", "compact", "declare", "default", "defer", "disabled",
            "formnovalidate", "hidden", "inert", "ismap", "itemscope", "multiple", "muted", "nohref", "noresize",
            "noshade", "novalidate", "nowrap", "open", "readonly", "required", "reversed", "seamless", "selected",
            "sortable", "truespeed", "typemustmatch",
            // Extra ones
            "autoplay", "controls", "loop", "muted", "defaultchecked", "defaultselected", "defaultmuted", "enabled",
            "indeterminate", "pauseonexit", "scoped", "spellcheck", "visible"
    ).stream().collect(Collectors.toSet());

    private boolean omitValue(Element e, Attribute a, String val) {
        if (val.isEmpty()) {
            return true;
        }

        if (booleanAttributes.contains(a.getKey())) {
            return true;
        }

        return false;
    }

    private static final Pattern unambiguousAmpersand = Pattern.compile("&amp;([0-9a-zA-Z]*[^0-9a-zA-Z;])");

    private String cleanAttributeValue(Element e, Attribute a, String value) {
        String val = value;

        // Remove trailing semicolons in certain cases
        if (a.getKey().startsWith("on") || "style".equals(a.getKey())) {
            if (val.endsWith(";")) {
                val = val.substring(0, val.length() - 1).trim();
            }
        }

        // Clean javascript attributes
        if (a.getKey().startsWith("on")) {
            if (val.toLowerCase().startsWith("javascript:")) {
                val = val.substring("javascript:".length()).trim();
            }
        }

        // Re-expand unambiguous ampersands
        StringBuffer newVal = new StringBuffer();
        Matcher matcher = unambiguousAmpersand.matcher(val);

        while (matcher.find()) {
            matcher.appendReplacement(newVal, "&$1");
        }

        matcher.appendTail(newVal);
        val = newVal.toString();

        // Specific cases
        if ("class".equals(a.getKey())) {
            val = val.replaceAll("\\s+", " ");
        } else if ("meta".equals(e.nodeName()) && "content".equals(a.getKey()) && "viewport".equals(e.attributes().get("name"))) {
            val = val.replaceAll("\\s", "");
            val = val.replaceAll("\\.0+(\\D)", "$1"); // Remove needless zeroes (1.0 => 1)
            val = val.replaceAll("\\.0+$", "");
        }

        return val;
    }

    private boolean inBody(Node node) {
        Node n = node;
        while (n != null) {
            if (n instanceof Element && ((Element) n).tagName().equals("body")) {
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

}
