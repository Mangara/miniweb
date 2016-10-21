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
package miniweb;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;
import miniweb.html.ClassRenamer;
import miniweb.html.ClassCounter;
import miniweb.html.ClassCleaner;
import cz.vutbr.web.css.CSSException;
import cz.vutbr.web.css.StyleSheet;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
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
import miniweb.css.CSSPrinter;
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

    public static final int MAJOR_VERSION = 1;
    public static final int MINOR_VERSION = 0;

    /**
     * Runs MiniWeb with command-line parameters.
     *
     * @param args
     */
    public static void main(String[] args) {
        CommandLineArguments arguments = new CommandLineArguments();
        JCommander jc;

        try {
            jc = new JCommander(arguments, args);
        } catch (ParameterException ex) {
            System.err.println("Incorrect parameters: " + ex.getMessage());
            return;
        }

        if (arguments.isHelp()) {
            jc.setProgramName("java -jar MiniWeb.jar");
            jc.usage();
            return;
        } else if (arguments.isVersion()) {
            printVersionInfo();
            return;
        }

        List<Path> htmlFiles = arguments.getHtmlFiles().stream()
                .map(f -> Paths.get(f))
                .collect(Collectors.toList());

        Settings settings = new Settings();
        settings.setRemoveUnusedClasses(!arguments.isDontRemove());
        settings.setMungeClassNames(!arguments.isDontMunge());
        
        try {
            if (arguments.getOutputdir() != null || arguments.getInputdir() != null) {
                if (arguments.getOutputdir() != null && arguments.getInputdir() != null) {
                    Path inputDir = Paths.get(arguments.getInputdir());
                    Path outputDir = Paths.get(arguments.getOutputdir());

                    minify(htmlFiles, inputDir, outputDir, settings);
                } else {
                    System.err.println("Options inputdir and outputdir can only be used in combination.");
                }
            } else {
                minify(htmlFiles, arguments.isReplace(), settings);
            }
        } catch (FileNotFoundException ex) {
            System.err.println("An error occurred while accessing the files: " + ex);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void printVersionInfo() {
        System.out.printf("MiniWeb %d.%d%n"
                + "Copyright (c) 2016 Sander Verdonschot%n"
                + "License Apache v2%n"
                + "This is free software. You are free to change and redistribute it.%n",
                MAJOR_VERSION, MINOR_VERSION);
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
        minify(htmlFiles, replaceFiles, new Settings());
    }
    
    private static void minify(Iterable<Path> htmlFiles, boolean replaceFiles, Settings settings) throws IOException {
        Map<Path, Document> docs = parseAll(htmlFiles);
        Pair<Set<Path>, Set<Path>> externalFiles = findReferencedLocalCssAndJsFiles(docs);
        minify(docs, externalFiles.getKey(), externalFiles.getValue(), getTargets(htmlFiles, externalFiles.getKey(), externalFiles.getValue(), replaceFiles), settings);
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
        minify(htmlFiles, baseDir, outputDir, new Settings());
    }
    
    private static void minify(Iterable<Path> htmlFiles, Path baseDir, Path outputDir, Settings settings) throws IOException {
        Map<Path, Document> docs = parseAll(htmlFiles);
        Pair<Set<Path>, Set<Path>> externalFiles = findReferencedLocalCssAndJsFiles(docs);
        minify(docs, externalFiles.getKey(), externalFiles.getValue(), getTargets(htmlFiles, externalFiles.getKey(), externalFiles.getValue(), baseDir, outputDir), settings);
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
        minify(docs, cssFiles, jsFiles, targets, new Settings());
    }

    private static void minify(Map<Path, Document> htmlFiles, Iterable<Path> cssFiles, Iterable<Path> jsFiles, Map<Path, Path> targets, Settings settings) throws IOException {
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

        // Count how often each class occurs in the combined HTML files
        Map<String, Integer> htmlClassOccurrences = new HashMap<>();
        getAllCSSClasses(htmlFiles, cssFiles).forEach(c -> htmlClassOccurrences.put(c, 0));
        htmlFiles.forEach((path, doc) -> ClassCounter.countClasses(htmlClassOccurrences, doc));

        // Optimally compress the classnames, based on their frequencies in the HTML
        return ClassnameCompressor.compressClassNames(htmlClassOccurrences, settings.getDontMunge());
    }

    private static Set<String> getAllCSSClasses(Map<Path, Document> htmlFiles, Iterable<Path> cssFiles) throws IOException {
        Set<Path> unreferencedCSSFiles = new HashSet<>();
        cssFiles.forEach(unreferencedCSSFiles::add);

        List<StyleSheet> styles = new ArrayList<>();

        for (Entry<Path, Document> htmlFile : htmlFiles.entrySet()) {
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

            Files.createDirectories(targets.get(cssFile).getParent());

            try (BufferedWriter out = Files.newBufferedWriter(targets.get(cssFile))) {
                for (String aImport : imports) {
                    out.write(aImport);
                }

                String css = CSSPrinter.toString(stylesheet);
                CssCompressor compressor = new CssCompressor(new StringReader(css));
                compressor.compress(out, -1);
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

            Files.createDirectories(targets.get(jsFile).getParent());

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
