package miniweb;

import miniweb.html.ClassRenamer;
import miniweb.html.ClassCounter;
import miniweb.html.ClassCleaner;
import cz.vutbr.web.css.CSSException;
import cz.vutbr.web.css.StyleSheet;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import miniweb.css.CssClassRenamer;
import miniweb.css.ReferencedElementsFinder;
import miniweb.css.StylesheetExtractor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

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
        List<StyleSheet> stylesheets = StylesheetExtractor.getStylesheets(doc, inputDir, input);

        // Remove unnecessary classes from elements
        Map<Element, Set<String>> referencedByClassFromCSS = ReferencedElementsFinder.getReferencedElements(doc, stylesheets);
        ClassCleaner.removeUnreferencedClasses(doc, referencedByClassFromCSS);

        // Optimally compress the classnames, based on their frequencies in the HTML
        Map<String, Integer> htmlClassOccurrences = ClassCounter.countClasses(doc);
        Map<String, String> compressedClassNames = ClassnameCompressor.compressClassNames(htmlClassOccurrences);
        ClassRenamer.renameHtmlClasses(doc, compressedClassNames);
        CssClassRenamer.renameCssClasses(compressedClassNames, stylesheets);

        System.out.println(doc);
        System.out.println(stylesheets);
    }
}
