/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package miniweb.html;

import java.util.HashMap;
import java.util.Map;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

/**
 *
 * @author Sander Verdonschot <sander.verdonschot at gmail.com>
 */
public class ClassCounter implements NodeVisitor {

    private final Map<String, Integer> classCount;

    public static Map<String, Integer> countClasses(Document doc) {
        ClassCounter classCount = new ClassCounter();
        NodeTraversor countTraversor = new NodeTraversor(classCount);

        for (Node childNode : doc.childNodes()) {
            countTraversor.traverse(childNode);
        }

        return classCount.getClassCount();
    }
    
    private ClassCounter() {
        classCount = new HashMap<>();
    }

    public Map<String, Integer> getClassCount() {
        return classCount;
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
