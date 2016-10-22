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
package mangara.miniweb;

import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;
import cz.vutbr.web.css.CSSException;
import cz.vutbr.web.css.StyleSheet;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javafx.util.Pair;
import mangara.miniweb.css.CSSPrinter;
import mangara.miniweb.css.ClassFinder;
import mangara.miniweb.css.CssClassRenamer;
import mangara.miniweb.css.ReferencedElementsFinder;
import mangara.miniweb.css.StylesheetExtractor;
import mangara.miniweb.html.ClassCleaner;
import mangara.miniweb.html.ClassCounter;
import mangara.miniweb.html.ClassRenamer;
import mangara.miniweb.js.BasicErrorReporter;
import mangara.miniweb.js.JSClassRenamer;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.MinifyVisitor;
import org.mozilla.javascript.EvaluatorException;

public class Minifier {

    private static final Pattern importPattern = Pattern.compile("@import [^;]+;");

    static void minify(Map<Path, Document> htmlFiles, Iterable<Path> cssFiles, Iterable<Path> jsFiles, Map<Path, Path> targets, Settings settings) throws IOException {
        findAndUpdateSettings(htmlFiles.keySet(), settings);

        removeUnreferencedClasses(htmlFiles, settings);

        Map<String, String> compressedClassNames = compressClassnames(htmlFiles, cssFiles, settings);

        writeCompressedHTMLFiles(htmlFiles, compressedClassNames, targets);
        writeCompressedCSSFiles(cssFiles, compressedClassNames, targets);
        writeCompressedJSFiles(jsFiles, compressedClassNames, targets);
    }

    private static void findAndUpdateSettings(Set<Path> htmlFiles, Settings settings) throws IOException {
        Settings parsedSettings = null;

        for (Path htmlFile : htmlFiles) {
            Path settingsFile = htmlFile.getParent().resolve("miniweb.properties");

            if (Files.exists(settingsFile)) {
                parsedSettings = Settings.parse(settingsFile);
                break;
            }
        }

        if (parsedSettings != null) {
            settings.setRemoveUnusedClasses(settings.isRemoveUnusedClasses() && parsedSettings.isRemoveUnusedClasses());
            settings.setDontRemove(parsedSettings.getDontRemove());

            settings.setMungeClassNames(settings.isMungeClassNames() && parsedSettings.isMungeClassNames());
            settings.setDontMunge(parsedSettings.getDontMunge());
        }
    }

    private static void removeUnreferencedClasses(Map<Path, Document> htmlFiles, Settings settings) throws IOException {
        if (!settings.isRemoveUnusedClasses()) {
            return;
        }

        for (Map.Entry<Path, Document> htmlFile : htmlFiles.entrySet()) {
            try {
                Document doc = htmlFile.getValue();
                Path location = htmlFile.getKey();

                List<Pair<Path, StyleSheet>> stylesheets = StylesheetExtractor.getStylesheets(doc, location.getParent(), location);
                List<StyleSheet> styles = stylesheets.stream().map((Pair<Path, StyleSheet> p) -> p.getValue()).collect(Collectors.toList());

                Map<Element, Set<String>> referencedByClassFromCSS = ReferencedElementsFinder.getReferencedElements(doc, styles);
                ClassCleaner.removeUnreferencedClasses(doc, referencedByClassFromCSS, settings.getDontRemove());
            } catch (CSSException ex) {
                throw new IOException(ex);
            }
        }
    }

    private static Map<String, String> compressClassnames(Map<Path, Document> htmlFiles, Iterable<Path> cssFiles, Settings settings) throws IOException {
        if (!settings.isMungeClassNames()) {
            return Collections.emptyMap();
        }

        Map<String, Integer> htmlClassOccurrences = new HashMap<>();
        getAllCSSClasses(htmlFiles, cssFiles).forEach((String c) -> htmlClassOccurrences.put(c, 0));

        htmlFiles.forEach((Path path, Document doc) -> ClassCounter.countClasses(htmlClassOccurrences, doc));

        return ClassnameCompressor.compressClassNames(htmlClassOccurrences, settings.getDontMunge());
    }

    private static Set<String> getAllCSSClasses(Map<Path, Document> htmlFiles, Iterable<Path> cssFiles) throws IOException {
        Set<Path> unreferencedCSSFiles = new HashSet<>();
        cssFiles.forEach(unreferencedCSSFiles::add);

        List<StyleSheet> styles = new ArrayList<>();

        for (Map.Entry<Path, Document> htmlFile : htmlFiles.entrySet()) {
            try {
                Document doc = htmlFile.getValue();
                Path location = htmlFile.getKey();

                // Find all inline and referenced stylesheets
                List<Pair<Path, StyleSheet>> stylesheets = StylesheetExtractor.getStylesheets(doc, location.getParent(), location);

                for (Pair<Path, StyleSheet> stylesheet : stylesheets) {
                    styles.add(stylesheet.getValue());
                    unreferencedCSSFiles.remove(stylesheet.getKey());
                }
            } catch (CSSException ex) {
                throw new IOException(ex);
            }
        }

        for (Path cssFile : unreferencedCSSFiles) {
            try {
                styles.add(StylesheetExtractor.parseFile(cssFile));
            } catch (CSSException ex) {
                throw new IOException(ex);
            }
        }

        return ClassFinder.getAllClasses(styles);
    }

    private static void writeCompressedHTMLFiles(Map<Path, Document> htmlFiles, Map<String, String> compressedClassNames, Map<Path, Path> targets) throws IOException {
        for (Map.Entry<Path, Document> htmlFile : htmlFiles.entrySet()) {
            Document doc = htmlFile.getValue();
            Path location = htmlFile.getKey();
            Path target = targets.get(location);

            ClassRenamer.renameHtmlClasses(doc, compressedClassNames);

            String html = MinifyVisitor.minify(doc);

            Files.createDirectories(target.getParent());
            try (final BufferedWriter out = Files.newBufferedWriter(target)) {
                out.write(html);
            }
        }
    }

    private static void writeCompressedCSSFiles(Iterable<Path> cssFiles, Map<String, String> compressedClassNames, Map<Path, Path> targets) throws IOException {
        for (Path cssFile : cssFiles) {
            List<String> imports = collectImportStatements(cssFile);
            
            StyleSheet stylesheet;
            
            try {
                stylesheet = StylesheetExtractor.parseFile(cssFile);
            } catch (CSSException ex) {
                System.err.println("Exception while parsing CSS file " + cssFile + ": " + ex.getMessage());
                continue;
            }
            
            CssClassRenamer.renameCssClasses(compressedClassNames, stylesheet);
            
            Files.createDirectories(targets.get(cssFile).getParent());
            try (final BufferedWriter out = Files.newBufferedWriter(targets.get(cssFile))) {
                for (String aImport : imports) {
                    out.write(aImport);
                }
                
                String css = CSSPrinter.toString(stylesheet);
                CssCompressor compressor = new CssCompressor(new StringReader(css));
                compressor.compress(out, -1);
            }
        }
    }

    private static List<String> collectImportStatements(Path cssFile) throws IOException {
        List<String> imports = new ArrayList<>();
        
        try (final BufferedReader in = Files.newBufferedReader(cssFile)) {
            for (String line = in.readLine(); line != null; line = in.readLine()) {
                Matcher match = importPattern.matcher(line);
                
                while (match.find()) {
                    imports.add(match.group());
                }
            }
        }
        
        return imports;
    }

    private static void writeCompressedJSFiles(Iterable<Path> jsFiles, Map<String, String> compressedClassNames, Map<Path, Path> targets) throws IOException {
        for (Path jsFile : jsFiles) {
            StringBuilder fileContents = new StringBuilder();
            
            try (final BufferedReader in = Files.newBufferedReader(jsFile)) {
                for (String line = in.readLine(); line != null; line = in.readLine()) {
                    fileContents.append(line);
                    fileContents.append('\n');
                }
            }
            
            Files.createDirectories(targets.get(jsFile).getParent());
            try (final BufferedWriter out = Files.newBufferedWriter(targets.get(jsFile))) {
                try {
                    JavaScriptCompressor compressor = new JavaScriptCompressor(new StringReader(fileContents.toString()), new BasicErrorReporter(jsFile.toString()));
                    StringWriter writer = new StringWriter();
                    compressor.compress(writer, -1, //linebreakpos
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

}