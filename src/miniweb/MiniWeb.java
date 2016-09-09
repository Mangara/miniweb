package miniweb;

import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;
import miniweb.html.ClassRenamer;
import miniweb.html.ClassCounter;
import miniweb.html.ClassCleaner;
import cz.vutbr.web.css.CSSException;
import cz.vutbr.web.css.RuleBlock;
import cz.vutbr.web.css.StyleSheet;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javafx.util.Pair;
import miniweb.css.ClassFinder;
import miniweb.css.CssClassRenamer;
import miniweb.css.ReferencedElementsFinder;
import miniweb.css.StylesheetExtractor;
import miniweb.js.BasicErrorReporter;
import miniweb.js.JSClassRenamer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.MinifyVisitor;
import org.mozilla.javascript.EvaluatorException;

public class MiniWeb {

    private static final String testFolder;
    private static final List<String> testFiles;

    static {
        //testFolder = "CG-Publy"; testFiles = Arrays.asList("CG-Lab.html");
        testFolder = "PersonalWebsite"; testFiles = Arrays.asList("index.html", "misc.html", "oldaddress.html", "publications.html", "teaching.html");
        //testFolder = "ColorZebra"; testFiles = Arrays.asList("index.html");
    }

    private static final Path inputDir = Paths.get("testInputs/" + testFolder);
    private static final Path outputDir = Paths.get("testOutputs");

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, CSSException {
        List<Path> inputs = new ArrayList<>();

        for (String testFile : testFiles) {
            inputs.add(inputDir.resolve(testFile));
        }

        minify(inputs, inputDir, outputDir);

        /* Old driver code
         // Parse the HTML and all inline and local external stylesheets
         Document doc = Jsoup.parse(input.toFile(), "UTF-8");
         List<Pair<Path, StyleSheet>> stylesheets = StylesheetExtractor.getStylesheets(doc, inputDir, input);
         List<StyleSheet> styles = stylesheets.stream().map(p -> p.getValue()).collect(Collectors.toList());

         // Remove unnecessary classes from elements
         Map<Element, Set<String>> referencedByClassFromCSS = ReferencedElementsFinder.getReferencedElements(doc, styles);
         ClassCleaner.removeUnreferencedClasses(doc, referencedByClassFromCSS);

         // Optimally compress the classnames, based on their frequencies in the HTML
         // TODO: Add classes in the CSS that aren't used in the HTML with frequency 0
         Map<String, Integer> htmlClassOccurrences = new HashMap<>();
         ClassCounter.countClasses(htmlClassOccurrences, doc);
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
         css = css.replaceAll(".0%", "%"); // Work-around for a bug in YUI Compressor
         CssCompressor compressor = new CssCompressor(new StringReader(css));
         compressor.compress(out, -1);
         }
         }
         }
         }*/
    }

    /**
     * Minifies the given HTML files and all referenced local CSS and JS files,
     * then replaces the input files with the minified versions (if replaceFiles
     * is true), or writes the minified files to the same location, adding
     * ".min" before the file extension (if replaceFiles is false).
     *
     * @param htmlFiles
     * @param replaceFiles
     * @throws java.io.IOException
     */
    public static void minify(Iterable<Path> htmlFiles, boolean replaceFiles) throws IOException {
        Map<Path, Document> docs = parseAll(htmlFiles);
        Pair<Set<Path>, Set<Path>> externalFiles = findReferencedLocalCssAndJsFiles(docs);
        minify(docs, externalFiles.getKey(), externalFiles.getValue(), getTargets(htmlFiles, externalFiles.getKey(), externalFiles.getValue(), replaceFiles));
    }

    /**
     * Minifies the given HTML files and all referenced local CSS and JS files
     * and places the resulting files in the the same relative location with
     * respect to the output directory as they were to the base directory. In
     * other words, if the files were all in the base directory, the output
     * files will be in the output directory. If the HTML files were in a
     * subfolder called "html" of the base directory, they will also be in a
     * subfolder called "html" from the output directory, etc.
     *
     * @param htmlFiles
     * @param baseDir
     * @param outputDir
     * @throws java.io.IOException
     */
    public static void minify(Iterable<Path> htmlFiles, Path baseDir, Path outputDir) throws IOException {
        Map<Path, Document> docs = parseAll(htmlFiles);
        Pair<Set<Path>, Set<Path>> externalFiles = findReferencedLocalCssAndJsFiles(docs);
        minify(docs, externalFiles.getKey(), externalFiles.getValue(), getTargets(htmlFiles, externalFiles.getKey(), externalFiles.getValue(), baseDir, outputDir));
    }

    /**
     * Minifies the given files, then replaces the input files with the minified
     * versions (if replaceFiles is true), or writes the minified files to the
     * same location, adding ".min" before the file extension (if replaceFiles
     * is false).
     *
     * @param htmlFiles
     * @param cssFiles
     * @param jsFiles
     * @param replaceFiles
     * @throws java.io.IOException
     */
    public static void minify(Iterable<Path> htmlFiles, Iterable<Path> cssFiles, Iterable<Path> jsFiles, boolean replaceFiles) throws IOException {
        minify(htmlFiles, cssFiles, jsFiles, getTargets(htmlFiles, cssFiles, jsFiles, replaceFiles));
    }

    /**
     * Minifies the given files and places the resulting files in the the same
     * relative location with respect to the output directory as they were to
     * the base directory. In other words, if the files were all in the base
     * directory, the output files will be in the output directory. If the HTML
     * files were in a subfolder called "html" of the base directory, they will
     * also be in a subfolder called "html" from the output directory, etc.
     *
     * @param htmlFiles
     * @param cssFiles
     * @param jsFiles
     * @param baseDir
     * @param outputDir
     * @throws java.io.IOException
     */
    public static void minify(Iterable<Path> htmlFiles, Iterable<Path> cssFiles, Iterable<Path> jsFiles, Path baseDir, Path outputDir) throws IOException {
        minify(htmlFiles, cssFiles, jsFiles, getTargets(htmlFiles, cssFiles, jsFiles, baseDir, outputDir));
    }

    /**
     * Minifies the given files and places the resulting files at the specified
     * target locations.
     *
     * @param htmlFiles
     * @param cssFiles
     * @param jsFiles
     * @param targets
     * @throws java.io.IOException
     */
    public static void minify(Iterable<Path> htmlFiles, Iterable<Path> cssFiles, Iterable<Path> jsFiles, Map<Path, Path> targets) throws IOException {
        Map<Path, Document> docs = parseAll(htmlFiles);
        minify(docs, cssFiles, jsFiles, targets);
    }

    private static void minify(Map<Path, Document> htmlFiles, Iterable<Path> cssFiles, Iterable<Path> jsFiles, Map<Path, Path> targets) throws IOException {
        removeUnreferencedClasses(htmlFiles);

        Map<String, String> compressedClassNames = compressClassnames(htmlFiles, cssFiles);

        writeCompressedHTMLFiles(htmlFiles, compressedClassNames, targets);
        writeCompressedCSSFiles(cssFiles, compressedClassNames, targets);
        writeCompressedJSFiles(jsFiles, compressedClassNames, targets);
    }

    private static void removeUnreferencedClasses(Map<Path, Document> htmlFiles) throws IOException {
        // Remove HTML class attributes that are not referenced by the CSS
        for (Entry<Path, Document> htmlFile : htmlFiles.entrySet()) {
            try {
                Document doc = htmlFile.getValue();
                Path location = htmlFile.getKey();

                // Find all inline and referenced stylesheets
                List<Pair<Path, StyleSheet>> stylesheets = StylesheetExtractor.getStylesheets(doc, location.getParent(), location);
                List<StyleSheet> styles = stylesheets.stream().map(p -> p.getValue()).collect(Collectors.toList());

                // For each HTML element, find all classes that it is referenced by
                Map<Element, Set<String>> referencedByClassFromCSS = ReferencedElementsFinder.getReferencedElements(doc, styles);

                // For each HTML element, remove all class attributes that are not referenced from the CSS
                ClassCleaner.removeUnreferencedClasses(doc, referencedByClassFromCSS);
            } catch (CSSException ex) {
                throw new IOException(ex);
            }
        }
    }

    private static Map<String, String> compressClassnames(Map<Path, Document> htmlFiles, Iterable<Path> cssFiles) throws IOException {
        // Count how often each class occurs in the combined HTML files
        Map<String, Integer> htmlClassOccurrences = new HashMap<>();
        getAllCSSClasses(htmlFiles, cssFiles).forEach(c -> htmlClassOccurrences.put(c, 0));
        htmlFiles.forEach((path, doc) -> ClassCounter.countClasses(htmlClassOccurrences, doc));

        // Optimally compress the classnames, based on their frequencies in the HTML
        return ClassnameCompressor.compressClassNames(htmlClassOccurrences);
    }

    private static Set<String> getAllCSSClasses(Map<Path, Document> htmlFiles, Iterable<Path> cssFiles) throws IOException {
        List<StyleSheet> styles = new ArrayList<>();

        for (Entry<Path, Document> htmlFile : htmlFiles.entrySet()) {
            try {
                Document doc = htmlFile.getValue();
                Path location = htmlFile.getKey();

                // Find all inline and referenced stylesheets
                List<Pair<Path, StyleSheet>> stylesheets = StylesheetExtractor.getStylesheets(doc, location.getParent(), location);
                stylesheets.stream().map(p -> p.getValue()).forEach(styles::add);
            } catch (CSSException ex) {
                throw new IOException(ex);
            }
        }

        return ClassFinder.getAllClasses(styles);
    }

    private static void writeCompressedHTMLFiles(Map<Path, Document> htmlFiles, Map<String, String> compressedClassNames, Map<Path, Path> targets) throws IOException {
        for (Entry<Path, Document> htmlFile : htmlFiles.entrySet()) {
            Document doc = htmlFile.getValue();
            Path location = htmlFile.getKey();
            Path target = targets.get(location);

            // Rename classes
            ClassRenamer.renameHtmlClasses(doc, compressedClassNames);

            // Compress the HTML
            String html = MinifyVisitor.minify(doc);

            // Write the HTML
            Files.createDirectories(target.getParent());

            try (BufferedWriter out = Files.newBufferedWriter(target)) {
                out.write(html);
            }
        }
    }

    private static void writeCompressedCSSFiles(Iterable<Path> cssFiles, Map<String, String> compressedClassNames, Map<Path, Path> targets) throws IOException {
        for (Path cssFile : cssFiles) {
            // Collect @import statements
            List<String> imports = collectImportStatements(cssFile);

            // Parse 
            StyleSheet stylesheet;

            try {
                stylesheet = StylesheetExtractor.parseFile(cssFile);
            } catch (CSSException ex) {
                System.err.println("Exception while parsing CSS file " + cssFile + ": " + ex.getMessage());
                continue;
            }

            CssClassRenamer.renameCssClasses(compressedClassNames, stylesheet);
            
            try (BufferedWriter out = Files.newBufferedWriter(targets.get(cssFile))) {
                for (String aImport : imports) {
                    out.write(aImport);
                }

                for (RuleBlock<?> rules : stylesheet) {
                    String css = rules.toString();
                    css = css.replaceAll(".0%", "%"); // Work-around for a bug in YUI Compressor
                    CssCompressor compressor = new CssCompressor(new StringReader(css));
                    compressor.compress(out, -1);
                }
            }
        }
    }

    private static void writeCompressedJSFiles(Iterable<Path> jsFiles, Map<String, String> compressedClassNames, Map<Path, Path> targets) throws IOException {
        for (Path jsFile : jsFiles) {
            StringBuilder fileContents = new StringBuilder();

            try (BufferedReader in = Files.newBufferedReader(jsFile)) {
                for (String line = in.readLine(); line != null; line = in.readLine()) {
                    fileContents.append(line);
                    fileContents.append('\n');
                }
            }

            try (BufferedWriter out = Files.newBufferedWriter(targets.get(jsFile))) {
                try {
                    JavaScriptCompressor compressor = new JavaScriptCompressor(new StringReader(fileContents.toString()), new BasicErrorReporter(jsFile.toString()));

                    StringWriter writer = new StringWriter();
                    
                    compressor.compress(writer,
                            -1, //linebreakpos
                            true, //munge
                            false, //verbose
                            false, //preserveAllSemiColons
                            false //disableOptimizations
                    );
                    
                    out.write(JSClassRenamer.renameHTMLClasses(writer.toString(), compressedClassNames, jsFile.toString()));
                } catch (EvaluatorException ex) {
                    System.err.println("Exception trying to parse " + jsFile + ": " + ex.getMessage());
                    System.err.println("Copying JavaScript uncompressed.");
                    
                    out.write(fileContents.toString());
                }
            }
        }
    }

    private static Map<Path, Document> parseAll(Iterable<Path> htmlFiles) throws IOException {
        Map<Path, Document> parsed = new HashMap<>();

        for (Path htmlFile : htmlFiles) {
            parsed.put(htmlFile, Jsoup.parse(htmlFile.toFile(), "UTF-8"));
        }

        return parsed;
    }

    private static Pair<Set<Path>, Set<Path>> findReferencedLocalCssAndJsFiles(Map<Path, Document> htmlFiles) {
        Set<Path> cssFiles = new HashSet<>();
        Set<Path> jsFiles = new HashSet<>();

        for (Entry<Path, Document> entry : htmlFiles.entrySet()) {
            Path baseDir = entry.getKey().getParent();
            Document doc = entry.getValue();

            doc.select("link[rel=stylesheet]").stream()
                    .filter(e -> !e.attr("href").startsWith("http"))
                    .map(e -> baseDir.resolve(Paths.get(e.attr("href"))))
                    .forEach(cssFiles::add);

            doc.select("script[src]").stream()
                    .filter(e -> !e.attr("src").startsWith("http"))
                    .map(e -> baseDir.resolve(Paths.get(e.attr("src"))))
                    .forEach(jsFiles::add);
        }

        return new Pair<>(cssFiles, jsFiles);
    }

    private static Map<Path, Path> getTargets(Iterable<Path> htmlFiles, Iterable<Path> cssFiles, Iterable<Path> jsFiles, boolean replaceFiles) {
        Map<Path, Path> targets = new HashMap<>();

        for (Path file : htmlFiles) {
            targets.put(file, (replaceFiles ? file : addExtension(file)));
        }

        for (Path file : cssFiles) {
            targets.put(file, (replaceFiles ? file : addExtension(file)));
        }

        for (Path file : jsFiles) {
            targets.put(file, (replaceFiles ? file : addExtension(file)));
        }

        return targets;
    }

    private static Map<Path, Path> getTargets(Iterable<Path> htmlFiles, Iterable<Path> cssFiles, Iterable<Path> jsFiles, Path baseDir, Path outputDir) {
        Map<Path, Path> targets = new HashMap<>();

        for (Path file : htmlFiles) {
            targets.put(file, outputDir.resolve(baseDir.relativize(file)));
        }

        for (Path file : cssFiles) {
            targets.put(file, outputDir.resolve(baseDir.relativize(file)));
        }

        for (Path file : jsFiles) {
            targets.put(file, outputDir.resolve(baseDir.relativize(file)));
        }

        return targets;
    }

    private static Path addExtension(Path file) {
        String fileName = file.getFileName().toString();
        int lastPeriodIndex = fileName.lastIndexOf('.');
        String newName = (lastPeriodIndex < 0
                ? fileName + ".min"
                : fileName.substring(0, lastPeriodIndex) + ".min" + fileName.substring(lastPeriodIndex));
        return file.resolveSibling(newName);
    }

    private static final Pattern importPattern = Pattern.compile("@import [^;]+;");

    private static List<String> collectImportStatements(Path cssFile) throws IOException {
        List<String> imports = new ArrayList<>();

        try (BufferedReader in = Files.newBufferedReader(cssFile)) {
            for (String line = in.readLine(); line != null; line = in.readLine()) {
                Matcher match = importPattern.matcher(line);

                while (match.find()) {
                    imports.add(match.group());
                }
            }
        }

        return imports;
    }
}
