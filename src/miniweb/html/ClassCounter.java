package miniweb.html;

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
