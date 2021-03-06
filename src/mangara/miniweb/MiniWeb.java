/*
 * Copyright 2016-2017 Sander Verdonschot <sander.verdonschot at gmail.com>.
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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.util.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class MiniWeb {

    public static final int MAJOR_VERSION = 1;
    public static final int MINOR_VERSION = 2;

    private static final Settings defaultSettings = new Settings();

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

        if (arguments.isHelp() || arguments.getHtmlFiles().isEmpty()) {
            jc.setProgramName("java -jar MiniWeb.jar");
            jc.usage();
            return;
        } else if (arguments.isVersion()) {
            printVersionInfo();
            return;
        }

        List<Path> htmlFiles = arguments.getHtmlFiles().stream()
                .map(f -> Paths.get(f).toAbsolutePath())
                .collect(Collectors.toList());

        Settings settings = new Settings();
        settings.setRemoveUnusedClasses(!arguments.isDontRemove());
        settings.setMungeClassNames(!arguments.isDontMunge());

        try {
            if (arguments.getOutputdir() != null || arguments.getInputdir() != null) {
                if (arguments.getOutputdir() != null && arguments.getInputdir() != null) {
                    Path inputDir = Paths.get(arguments.getInputdir()).toAbsolutePath();
                    Path outputDir = Paths.get(arguments.getOutputdir()).toAbsolutePath();

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
     * Toggles the removing of classes in the HTML file that do not have
     * associated rules in any of the CSS files.
     *
     * @param removeUnusedClasses
     */
    public static void setRemoveUnusedClasses(boolean removeUnusedClasses) {
        defaultSettings.setRemoveUnusedClasses(removeUnusedClasses);
    }

    /**
     * Sets the list of class names that won't be removed from the HTML file,
     * even if they are not used in any CSS rules.
     *
     * @param dontRemove
     */
    public static void setDontRemove(Collection<? extends String> dontRemove) {
        defaultSettings.setDontRemove(dontRemove);
    }

    /**
     * Toggles the shortening of class names across all relevant files.
     *
     * @param mungeClassNames
     */
    public static void setMungeClassNames(boolean mungeClassNames) {
        defaultSettings.setMungeClassNames(mungeClassNames);
    }

    /**
     * Sets the list of class names that won't be shortened.
     *
     * @param dontMunge
     */
    public static void setDontMunge(Collection<? extends String> dontMunge) {
        defaultSettings.setDontMunge(dontMunge);
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
        minify(htmlFiles, replaceFiles, defaultSettings);
    }

    private static void minify(Iterable<Path> htmlFiles, boolean replaceFiles, Settings settings) throws IOException {
        Map<Path, Document> docs = parseAll(htmlFiles);
        Pair<Set<Path>, Set<Path>> externalFiles = findReferencedLocalCssAndJsFiles(docs);
        Minifier.minify(docs, externalFiles.getKey(), externalFiles.getValue(), MiniWeb.getTargets(docs.keySet(), externalFiles.getKey(), externalFiles.getValue(), replaceFiles), settings);
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
        minify(htmlFiles, baseDir, outputDir, defaultSettings);
    }

    private static void minify(Iterable<Path> htmlFiles, Path baseDir, Path outputDir, Settings settings) throws IOException {
        Map<Path, Document> docs = parseAll(htmlFiles);
        Pair<Set<Path>, Set<Path>> externalFiles = findReferencedLocalCssAndJsFiles(docs);
        Minifier.minify(docs, externalFiles.getKey(), externalFiles.getValue(), getTargets(docs.keySet(), externalFiles.getKey(), externalFiles.getValue(), baseDir.toAbsolutePath(), outputDir.toAbsolutePath()), settings);
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

        Set<Path> absoluteCSSFiles = new HashSet<>();
        Set<Path> absoluteJSFiles = new HashSet<>();
        Map<Path, Path> absoluteTargets = new HashMap<>(targets.size());

        for (Path htmlFile : htmlFiles) {
            absoluteTargets.put(htmlFile.toAbsolutePath(), targets.get(htmlFile).toAbsolutePath());
        }

        for (Path cssFile : cssFiles) {
            Path absolute = cssFile.toAbsolutePath();

            absoluteCSSFiles.add(absolute);
            absoluteTargets.put(absolute, targets.get(cssFile).toAbsolutePath());
        }

        for (Path jsFile : jsFiles) {
            Path absolute = jsFile.toAbsolutePath();

            absoluteJSFiles.add(absolute);
            absoluteTargets.put(absolute, targets.get(jsFile).toAbsolutePath());
        }

        Minifier.minify(docs, absoluteCSSFiles, absoluteJSFiles, absoluteTargets, defaultSettings);
    }

    private static Map<Path, Document> parseAll(Iterable<Path> htmlFiles) throws IOException {
        Map<Path, Document> parsed = new HashMap<>();

        for (Path htmlFile : htmlFiles) {
            parsed.put(htmlFile.toAbsolutePath(), Jsoup.parse(htmlFile.toFile(), "UTF-8"));
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

}
