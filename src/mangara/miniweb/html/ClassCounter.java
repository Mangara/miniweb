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

import java.util.Map;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

public class ClassCounter implements NodeVisitor {

    private final Map<String, Integer> classCount;

    public static Map<String, Integer> countClasses(Map<String, Integer> classCount, Document doc) {
        ClassCounter counter = new ClassCounter(classCount);
        NodeTraversor countTraversor = new NodeTraversor(counter);

        for (Node childNode : doc.childNodes()) {
            countTraversor.traverse(childNode);
        }

        return counter.classCount;
    }
    
    private ClassCounter(Map<String, Integer> classCount) {
        this.classCount = classCount;
    }

    @Override
    public void head(Node node, int depth) {
        if (node instanceof Element) {
            Element e = (Element) node;
            
            for (String className : e.classNames()) {
                classCount.merge(className, 1, Integer::sum);
            }
        }
    }

    @Override
    public void tail(Node node, int depth) {
    }

}
