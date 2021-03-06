/*
 * Copyright 2016-2017 Sander Verdonschot <sander.verdonschot at gmail.com>.
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
package mangara.miniweb.html;

import cz.vutbr.web.css.StyleSheet;
import java.util.Map;
import java.util.stream.Collectors;
import mangara.miniweb.css.CSSPrinter;
import mangara.miniweb.css.CssClassRenamer;
import mangara.miniweb.css.StylesheetExtractor;
import mangara.miniweb.js.JSClassRenamer;
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
                                .map(s -> newNames.getOrDefault(s, s))
                                .collect(Collectors.toSet())
                );
            }
        } else if (node instanceof DataNode
                && node.parent() instanceof Element
                && "style".equals(((Element) node.parent()).tagName())) { // Inline CSS
            DataNode dataNode = (DataNode) node;
            StyleSheet style = StylesheetExtractor.parseString(dataNode.getWholeData());

            if (style != null) {
                CssClassRenamer.renameCssClasses(newNames, style);
                dataNode.setWholeData(CSSPrinter.toString(style));
            }
        } else if (node instanceof DataNode
                && node.parent() instanceof Element
                && "script".equals(((Element) node.parent()).tagName())) { // Inline JS
            DataNode dataNode = (DataNode) node;
            dataNode.setWholeData(JSClassRenamer.renameHTMLClasses(dataNode.getWholeData(), newNames));
        }
    }

    @Override
    public void tail(Node node, int depth) {
        // No action needed
    }
}
