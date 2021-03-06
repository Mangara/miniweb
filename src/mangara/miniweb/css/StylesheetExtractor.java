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
package mangara.miniweb.css;

import cz.vutbr.web.css.CSSException;
import cz.vutbr.web.css.CSSFactory;
import cz.vutbr.web.css.MediaSpecNone;
import cz.vutbr.web.css.NetworkProcessor;
import cz.vutbr.web.css.StyleSheet;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.util.Pair;
import org.jsoup.nodes.Document;

public class StylesheetExtractor {

    static {
        // Don't resolve @import statements
        // NOTE: This is bugged in the current release, but fixed in the latest build.
        CSSFactory.setAutoImportMedia(new MediaSpecNone());

        // Using the localNetworkProcessor is a work-around that essentially removes non-local @import statements
    }

    public static final NetworkProcessor localNetworkProcessor = new NetworkProcessor() {

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

    public static final NetworkProcessor emptyNetworkProcessor = new NetworkProcessor() {

        @Override
        public InputStream fetch(URL url) throws IOException {
            return new InputStream() {

                @Override
                public int read() throws IOException {
                    return -1;
                }
            };
        }
    };

    public static StyleSheet parseString(String css) {
        try {
            return CSSFactory.parseString(css, null, emptyNetworkProcessor);
        } catch (IOException ex) {
            // Should never happen
            throw new InternalError(ex);
        } catch (CSSException ex) {
            System.err.printf("Error while parsing inline CSS fragment \"%s\":%n%s", css, ex.getLocalizedMessage());
        }
        
        return null;
    }

    public static StyleSheet parseFile(Path cssFile) throws IOException {
        try {
            return CSSFactory.parse(cssFile.toUri().toURL(), localNetworkProcessor, "UTF-8");
        } catch (FileNotFoundException ex) {
            System.err.printf("External style sheet \"%s\" not found.%n", cssFile.toString());
        } catch (CSSException ex) {
            System.err.printf("Error while parsing external style sheet \"%s\":%n%s", cssFile.toString(), ex.getLocalizedMessage());
        }
        
        return null;
    }

    public static List<Pair<Path, StyleSheet>> getStylesheets(Document doc, Path inputDir, Path input) throws IOException {
        List<Pair<Path, StyleSheet>> stylesheets = new ArrayList<>();

        // Process inline style blocks
        for (String css : getInlineStyleBlocks(doc)) {
            try {
                stylesheets.add(new Pair<>(null, CSSFactory.parseString(css, input.toUri().toURL(), localNetworkProcessor)));
            } catch (CSSException ex) {
                System.err.printf("Error while parsing inline CSS fragment \"%s\" in \"%s\":%n%s", css, input.toString(), ex.getLocalizedMessage());
            }
        }

        // Process external stylesheets
        for (Path cssFile : getExternalStyleSheets(doc)) {
            try {
                stylesheets.add(new Pair<>(cssFile, CSSFactory.parse(inputDir.resolve(cssFile).toUri().toURL(), localNetworkProcessor, "UTF-8")));
            } catch (FileNotFoundException ex) {
                System.err.printf("External style sheet \"%s\" (referenced by \"%s\") not found.%n", cssFile.toString(), input.toString());
            } catch (CSSException ex) {
                System.err.printf("Error while parsing external style sheet \"%s\":%n%s", cssFile.toString(), ex.getLocalizedMessage());
            }
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
                .map(e -> e.attr("href"))
                .filter(url -> !url.contains("://")) // Only process relative urls
                .map(url -> Paths.get(url))
                .collect(Collectors.toList());
    }
}
