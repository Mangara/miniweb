package miniweb.css;

import cz.vutbr.web.css.RuleBlock;
import cz.vutbr.web.css.StyleSheet;

public class CSSPrinter {

    public static String toString(StyleSheet stylesheet) {
        StringBuilder sb = new StringBuilder();
        
        for (RuleBlock<?> rules : stylesheet) {
            String css = rules.toString();
            css = css.replaceAll(".0%", "%"); // Work-around for a bug in YUI Compressor
            sb.append(css);
        }
        
        return sb.toString();
    }
}
