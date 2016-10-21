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
package mangara.miniweb.html;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

public class ClassCleaner implements NodeVisitor {

    private final Map<Element, Set<String>> referencedByClass;
    private final Set<String> dontRemove;

    public static void removeUnreferencedClasses(Document doc, Map<Element, Set<String>> referencedByClass, Set<String> dontRemove) {
        new NodeTraversor(new ClassCleaner(referencedByClass, dontRemove)).traverse(doc);
    }

    private ClassCleaner(Map<Element, Set<String>> referencedByClass, Set<String> dontRemove) {
        this.referencedByClass = referencedByClass;
        this.dontRemove = dontRemove;
    }

    @Override
    public void head(Node node, int depth) {
        if (node instanceof Element) {
            Element e = (Element) node;

            // Fix classes
            Set<String> classes = e.classNames();
            classes.retainAll(dontRemove);
            classes.addAll(referencedByClass.getOrDefault(e, Collections.emptySet()));

            if (classes.isEmpty()) {
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
