package miniweb;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Test {
    private static final String testFolder;
    private static final List<String> testFiles;

    static {
        //testFolder = "CG-Publy"; testFiles = Arrays.asList("CG-Lab.html");
        testFolder = "PersonalWebsite";
        testFiles = Arrays.asList("index.html", "misc.html", "oldaddress.html", "publications.html", "teaching.html");
        //testFolder = "ColorZebra"; testFiles = Arrays.asList("index.html");
    }

    private static final Path inputDir = Paths.get("testInputs/" + testFolder);
    private static final Path outputDir = Paths.get("testOutputs");

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {
        List<Path> inputs = new ArrayList<>();

        for (String testFile : testFiles) {
            inputs.add(inputDir.resolve(testFile));
        }

        MiniWeb.minify(inputs, inputDir, outputDir);
    }
}
