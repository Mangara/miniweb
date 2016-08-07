package miniweb;

import miniweb.html.ClassRenamer;
import miniweb.html.ClassCounter;
import miniweb.html.ClassCleaner;
import cz.vutbr.web.css.CSSException;
import cz.vutbr.web.css.StyleSheet;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.util.Pair;
import miniweb.css.CssClassRenamer;
import miniweb.css.ReferencedElementsFinder;
import miniweb.css.StylesheetExtractor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.MinifyVisitor;

public class MiniWeb {

    private static final Path inputDir = Paths.get("testInputs/ColorZebra");
    private static final Path outputDir = Paths.get("testOutputs/ColorZebra");
    private static final Path input = inputDir.resolve("index.html");

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, CSSException {
        // Parse the HTML and all inline and local external stylesheets
        Document doc = Jsoup.parse(input.toFile(), "UTF-8");
        List<Pair<Path, StyleSheet>> stylesheets = StylesheetExtractor.getStylesheets(doc, inputDir, input);
        List<StyleSheet> styles = stylesheets.stream().map(p -> p.getValue()).collect(Collectors.toList());

        // Remove unnecessary classes from elements
        Map<Element, Set<String>> referencedByClassFromCSS = ReferencedElementsFinder.getReferencedElements(doc, styles);
        ClassCleaner.removeUnreferencedClasses(doc, referencedByClassFromCSS);

        // Optimally compress the classnames, based on their frequencies in the HTML
        Map<String, Integer> htmlClassOccurrences = ClassCounter.countClasses(doc);
        Map<String, String> compressedClassNames = ClassnameCompressor.compressClassNames(htmlClassOccurrences);
        ClassRenamer.renameHtmlClasses(doc, compressedClassNames);
        CssClassRenamer.renameCssClasses(compressedClassNames, styles);

        // Compress the HTML
        String html = MinifyVisitor.minify(doc);

        // Write the HTML
        Path outputFile = outputDir.resolve(inputDir.relativize(input));
        Files.createDirectories(outputFile.getParent());

        try (BufferedWriter out = Files.newBufferedWriter(outputFile)) {
            out.write(html);
        }

        // Write the stylesheets
        for (Pair<Path, StyleSheet> stylesheet : stylesheets) {
            if (stylesheet.getKey() != null) {
                Path file = outputDir.resolve(stylesheet.getKey());

                try (BufferedWriter out = Files.newBufferedWriter(file)) {
                    StyleSheet css = stylesheet.getValue();
                    
                    // TODO: good printing
                    
                    out.write(stylesheet.getValue().toString());
                }
            }
        }
    }
}
