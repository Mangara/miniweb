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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClassnameCompressor {

    private static final char[] classNameCharacters = new char[]{
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '_', '-'
    };
    
    public static Map<String, String> compressClassNames(Map<String, Integer> count, Set<String> dontMunge) {
        // Sort classes by count
        List<String> classNames = new ArrayList<>(count.keySet());

        Collections.sort(classNames, (c1, c2) -> -Integer.compare(count.get(c1), count.get(c2)));

        // Assign codes to classes in order
        Map<String, String> compressed = new HashMap<>(2 * classNames.size());

        int compressedIndex = 0;
        
        for (String className : classNames) {
            if (dontMunge.contains(className)) {
                continue;
            }
            
            String compressedName;
            
            do {
                compressedName = getCompressedName(compressedIndex);
                compressedIndex++;
            } while (dontMunge.contains(compressedName));
            
            compressed.put(className, compressedName);
        }

        return compressed;
    }

    private static String getCompressedName(int index) {
        int i = index;
        StringBuilder name = new StringBuilder();

        // First character may only be a-z
        name.append(classNameCharacters[i % 26]);
        i /= 26;

        // Find out how long the remaining part is
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
