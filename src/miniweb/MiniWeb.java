/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package miniweb;

import cz.vutbr.web.css.CSSException;
import cz.vutbr.web.css.CSSFactory;
import cz.vutbr.web.css.CombinedSelector;
import cz.vutbr.web.css.MediaSpecNone;
import cz.vutbr.web.css.NetworkProcessor;
import cz.vutbr.web.css.RuleBlock;
import cz.vutbr.web.css.RuleMedia;
import cz.vutbr.web.css.RuleSet;
import cz.vutbr.web.css.Selector;
import cz.vutbr.web.css.Selector.ElementClass;
import cz.vutbr.web.css.StyleSheet;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 *
 * @author Sander Verdonschot <sander.verdonschot at gmail.com>
 */
public class MiniWeb {

    private static final Path inputDir = Paths.get("testInputs/ColorZebra");
    private static final Path outputDir = Paths.get("testOutputs/ColorZebra");
    private static final Path input = inputDir.resolve("index.html");

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, CSSException {
        Document doc = Jsoup.parse(input.toFile(), "UTF-8");

        List<StyleSheet> stylesheets = getStylesheets(doc);

        Map<Element, Set<String>> referencedByClassFromCSS = getReferencedElements(doc, stylesheets);

        ClassCleaner.removeUnreferencedClasses(doc, referencedByClassFromCSS);

        Map<String, Integer> htmlClassOccurrences = ClassCounter.countClasses(doc);

        Map<String, String> compressedClassNames = compressClassNames(htmlClassOccurrences);
        
        ClassRenamer.renameClasses(doc, compressedClassNames);
        
        System.out.println(doc);
    }

    private static List<StyleSheet> getStylesheets(Document doc) throws CSSException, IOException {
        List<StyleSheet> stylesheets = new ArrayList<>();

        NetworkProcessor local = new NetworkProcessor() {

            @Override
            public InputStream fetch(URL url) throws IOException {
                // Only resolve local files for now
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
        };

        // Process inline style blocks
        for (String css : getInlineStyleBlocks(doc)) {
            stylesheets.add(CSSFactory.parseString(css, input.toUri().toURL(), local));
        }

        // Process external stylesheets
        CSSFactory.setAutoImportMedia(new MediaSpecNone());

        for (Path cssFile : getExternalStyleSheets(doc)) {
            stylesheets.add(CSSFactory.parse(inputDir.resolve(cssFile).toUri().toURL(), local, "UTF-8"));
        }

        return stylesheets;
    }

    private static List<String> getInlineStyleBlocks(Document doc) {
        return doc.select("style").stream()
                .map(e -> e.html())
                .collect(Collectors.toList());
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

    private static Map<Element, Set<String>> getReferencedElements(Document doc, List<StyleSheet> stylesheets) {
        Map<Element, Set<String>> referencedByClass = new HashMap<>();

        for (StyleSheet stylesheet : stylesheets) {
            for (RuleBlock<?> rules : stylesheet) {
                if (rules instanceof RuleSet) {
                    RuleSet set = (RuleSet) rules;
                    CombinedSelector[] selectors = set.getSelectors();

                    for (CombinedSelector selectorList : selectors) {
                        StringBuilder select = new StringBuilder();

                        for (Selector selector : selectorList) {
                            select.append(selector.getCombinator() == null ? "" : selector.getCombinator().value());

                            for (Selector.SelectorPart part : selector) {
                                select.append(part);

                                if (part instanceof ElementClass) {
                                    String className = ((ElementClass) part).getClassName();

                                    for (Element e : doc.select(select.toString())) {
                                        Set<String> classes = referencedByClass.get(e);

                                        if (classes == null) {
                                            referencedByClass.put(e, new HashSet<>(Collections.singleton(className)));
                                        } else {
                                            classes.add(className);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (rules instanceof RuleMedia) {
                    RuleMedia rm = (RuleMedia) rules;

                    System.out.println("Rule media:");
                    //System.out.println(rm);
                } else {
                    System.err.println("Unexpected RuleBlock type: " + rules);
                }
            }
        }

        return referencedByClass;
    }

    private static Map<String, String> compressClassNames(Map<String, Integer> count) {
        // Sort classes by count
        List<String> classes = new ArrayList<>(count.keySet());

        Collections.sort(classes, (c1, c2) -> -Integer.compare(count.get(c1), count.get(c2)));

        // Assign codes to classes in order
        Map<String, String> compressed = new HashMap<>(2 * classes.size());

        for (int i = 0; i < classes.size(); i++) {
            String className = classes.get(i);

            if (className.charAt(0) == classNameCharacters[25]) {
                // Don't compress names starting with x
                compressed.put(className, className);
            } else {
                compressed.put(className, getCompressedName(i));
            }
        }

        for (String classe : classes) {
            System.out.println(classe + ": " + count.get(classe) + " -> " + compressed.get(classe));
        }
        return compressed;
    }

    private static final char[] classNameCharacters = new char[]{
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'y', 'z',
        'x', // Generated names won't start with x
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '_', '-'
    };

    private static String getCompressedName(int index) {
        int i = index;
        StringBuilder name = new StringBuilder();
        
        // First character may only be a-z, excluding x
        name.append(classNameCharacters[i % 25]);
        i /= 25;
        
        // Find out how long the remainng part is
        int length = 0;
        int numWords = 1; // number of words with <length> characters
        
        while (i >= numWords) {
            i -= numWords;
            
            length++;
            numWords *= classNameCharacters.length;
        }
        
        // Find the i-th word of this length
        for (int j = 0; j < length; j++) {
            name.append(classNameCharacters[i % classNameCharacters.length]);
            i /= classNameCharacters.length;
        }

        return name.toString();
    }
}
