package miniweb.css;

import cz.vutbr.web.css.CSSException;
import cz.vutbr.web.css.CSSFactory;
import cz.vutbr.web.css.MediaSpecNone;
import cz.vutbr.web.css.NetworkProcessor;
import cz.vutbr.web.css.StyleSheet;
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

    public static List<Pair<Path, StyleSheet>> getStylesheets(Document doc, Path inputDir, Path input) throws CSSException, IOException {
        List<Pair<Path, StyleSheet>> stylesheets = new ArrayList<>();

        // Process inline style blocks
        for (String css : getInlineStyleBlocks(doc)) {
            stylesheets.add(new Pair<>(null, CSSFactory.parseString(css, input.toUri().toURL(), localNetworkProcessor)));
        }

        // Process external stylesheets
        for (Path cssFile : getExternalStyleSheets(doc)) {
            stylesheets.add(new Pair<>(cssFile, CSSFactory.parse(inputDir.resolve(cssFile).toUri().toURL(), localNetworkProcessor, "UTF-8")));
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
}
