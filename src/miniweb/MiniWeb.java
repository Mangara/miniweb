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
import cz.vutbr.web.css.Selector.ElementID;
import cz.vutbr.web.css.StyleSheet;
import cz.vutbr.web.domassign.Analyzer;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.util.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 *
 * @author Sander Verdonschot <sander.verdonschot at gmail.com>
 */
public class MiniWeb {

    private static final Path inputDir = Paths.get("testInputs/CG-Publy");
    private static final Path outputDir = Paths.get("testOutputs/CG-Publy");
    private static final Path input = inputDir.resolve("CG-Lab.html");

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, CSSException {
        Document doc = Jsoup.parse(input.toFile(), "UTF-8");

        List<StyleSheet> stylesheets = getStylesheets(doc);

        Map<Element, Set<String>> referencedByClassFromCSS = getReferencedElements(doc, stylesheets);
        
        ClassCleaner.removeUnreferencedClasses(doc, referencedByClassFromCSS);
        
        Map<String, Integer> htmlClassOccurrences = ClassCounter.countClasses(doc);
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
}
