/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package miniweb;

import cz.vutbr.web.css.CSSException;
import cz.vutbr.web.css.CSSFactory;
import cz.vutbr.web.css.MediaSpecNone;
import cz.vutbr.web.css.NetworkProcessor;
import cz.vutbr.web.css.StyleSheet;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
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
    public static void main(String[] args) throws IOException, CSSException {
        Path inputDir = Paths.get("testInputs/ColorZebra");
        Path outputDir = Paths.get("testOutputs/ColorZebra");
        Path input = inputDir.resolve("index.html");

        Document doc = Jsoup.parse(input.toFile(), "UTF-8");

        Map<String, Integer> htmlClassOccurrences = ClassCounter.countClasses(doc);

        System.out.println("Class occurrences: " + htmlClassOccurrences);

        List<Path> externalCSS = getExternalStyleSheets(doc);

        for (Path cssFile : externalCSS) {
            CSSFactory.setAutoImportMedia(new MediaSpecNone());
            StyleSheet css = CSSFactory.parse(inputDir.resolve(cssFile).toUri().toURL(), new NetworkProcessor() {

                @Override
                public InputStream fetch(URL url) throws IOException {
                    System.err.println("Asked to fetch " + url);
                    
                    if (url.toString().startsWith("file")) {
                        return url.openConnection().getInputStream();
                    } else {
                        return new InputStream() {

                            @Override
                            public int read() throws IOException {
                                return -1;
                            }
                        };
                    }
                }
            }, "UTF-8");
            System.out.println("CSS from " + cssFile + ":");
            System.out.println(css);
        }

        //System.out.println(minify(doc));
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
