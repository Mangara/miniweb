/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package miniweb;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.MinifyVisitor;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeTraversor;

/**
 *
 * @author Sander Verdonschot <sander.verdonschot at gmail.com>
 */
public class MiniWeb {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        File test = new File("testInputs/CG-Publy/CG-Lab.html");
        Document doc = Jsoup.parse(test, "UTF-8");

        System.out.println(minify(doc));
    }

    private static List<Path> getExternalStyleSheets(Document doc) {
        return doc.select("link[rel=stylesheet]").stream()
                .map(e -> Paths.get(e.attr("href")))
                .collect(Collectors.toList());
    }

    private static List<Path> getExternalJavascript(Document doc) {
        return doc.select("script[src]").stream()
                .filter(e -> !e.attr("src").startsWith("http")) // Only keep local JS files
                .map(e -> Paths.get(e.attr("src")))
                .collect(Collectors.toList());
    }

    private static String minify(Document doc) {
        StringBuilder minified = new StringBuilder();
        NodeTraversor minifyTraversor = new NodeTraversor(new MinifyVisitor(minified));

        for (Node childNode : doc.childNodes()) {
            minifyTraversor.traverse(childNode);
        }

        return minified.toString();
    }
}
