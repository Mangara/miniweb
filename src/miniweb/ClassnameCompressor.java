/*
 */
package miniweb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Sander Verdonschot <sander.verdonschot at gmail.com>
 */
public class ClassnameCompressor {

    private static final char[] classNameCharacters = new char[]{
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'y', 'z',
        'x', // Generated names won't start with x
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '_', '-'
    };

    public static final String exclusionPrefix = Character.toString(classNameCharacters[25]);
    
    public static Map<String, String> compressClassNames(Map<String, Integer> count) {
        // Sort classes by count
        List<String> classNames = new ArrayList<>(count.keySet());

        Collections.sort(classNames, (c1, c2) -> -Integer.compare(count.get(c1), count.get(c2)));

        // Assign codes to classes in order
        Map<String, String> compressed = new HashMap<>(2 * classNames.size());

        for (int i = 0; i < classNames.size(); i++) {
            String className = classNames.get(i);

            if (className.charAt(0) == classNameCharacters[25]) {
                // Don't compress names starting with x
                compressed.put(className, className);
            } else {
                compressed.put(className, getCompressedName(i));
            }
        }

        return compressed;
    }

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
