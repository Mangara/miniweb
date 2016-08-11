package miniweb;

import com.yahoo.platform.yui.compressor.CssCompressor;
import miniweb.html.ClassRenamer;
import miniweb.html.ClassCounter;
import miniweb.html.ClassCleaner;
import cz.vutbr.web.css.CSSException;
import cz.vutbr.web.css.RuleBlock;
import cz.vutbr.web.css.StyleSheet;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
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

    private static final String testFolder;
    private static final String testFile;
    
    static {
        testFolder = "CG-Publy"; testFile = "CG-Lab.html";
        //testFolder = "PersonalWebsite"; testFile = "index.html";
        //testFolder = "ColorZebra"; testFile = "index.html";
    }
    
    private static final Path inputDir = Paths.get("testInputs/" + testFolder);
    private static final Path outputDir = Paths.get("testOutputs");
    private static final Path input = inputDir.resolve(testFile);

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
        // TODO: Add classes in the CSS that aren't used in the HTML with frequency 0
        Map<String, Integer> htmlClassOccurrences = ClassCounter.countClasses(doc);
        Map<String, String> compressedClassNames = ClassnameCompressor.compressClassNames(htmlClassOccurrences);
        ClassRenamer.renameHtmlClasses(doc, compressedClassNames);
        CssClassRenamer.renameCssClasses(compressedClassNames, styles);
        // TODO: rename classes in inline CSS in the HTML files

        // Compress the HTML
        String html = MinifyVisitor.minify(doc);

        // Write the HTML
        Path outputFile = outputDir.resolve(inputDir.relativize(input));
        Files.createDirectories(outputFile.getParent());

        try (BufferedWriter out = Files.newBufferedWriter(outputFile)) {
            out.write(html);
        }

        // Write the stylesheets
        // TODO: Preserve @import statements
        for (Pair<Path, StyleSheet> stylesheet : stylesheets) {
            if (stylesheet.getKey() != null) {
                Path file = outputDir.resolve(stylesheet.getKey());

                try (BufferedWriter out = Files.newBufferedWriter(file)) {
                    for (RuleBlock<?> rules : stylesheet.getValue()) {
                        String css = rules.toString();
                        css = css.replaceAll(".0%", "%"); // This is an artifact introduced by the CSS parser
                        CssCompressor compressor = new CssCompressor(new StringReader(css));
                        compressor.compress(out, -1);
                    }
                }
            }
        }
    }
}
