/*
 * Copyright 2017 Sander Verdonschot <sander.verdonschot at gmail.com>.
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

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import org.junit.Test;
import static org.junit.Assert.*;

public class PhantomJSIntegrationTest {

    @Test
    public void testCGLab() throws IOException, InterruptedException {
        //testPage("CG-Publy", Arrays.asList("CG-Lab.html"));
        // Takes too long and tests the same things my website tests
    }

    @Test
    public void testMySite() throws IOException, InterruptedException {
        testPage("PersonalWebsite", Arrays.asList("index.html", "misc.html", "oldaddress.html", "publications.html", "teaching.html"));
    }

    @Test
    public void testColorZebra() throws IOException, InterruptedException {
        //testPage("ColorZebra", Arrays.asList("html/index.html"));
        // currently expected to fail due to a chrome / webkit layout bug
    }

    @Test
    public void testCCCG() throws IOException, InterruptedException {
        testPage("CCCG", Arrays.asList("index.html", "papers.html"));
    }

    private void testPage(String testFolder, List<String> testFiles) throws IOException, InterruptedException {
        Path inputDir = Paths.get("testInputs/" + testFolder);
        Path outputDir = Paths.get("testOutputs/" + testFolder);

        if (Files.exists(outputDir)) {
            clearFolder(outputDir);
        }

        List<Path> inputs = new ArrayList<>();

        for (String testFile : testFiles) {
            inputs.add(inputDir.resolve(testFile));
        }

        MiniWeb.minify(inputs, inputDir, outputDir);

        for (String testFile : testFiles) {
            Path input = inputDir.resolve(testFile);
            Path output = outputDir.resolve(testFile);

            compare(input, output);
        }
    }

    private static void clearFolder(Path dir) throws IOException {
        List<Path> outputFiles = Files.list(dir).collect(Collectors.toList());

        for (Path outputFile : outputFiles) {
            if (Files.isDirectory(outputFile)) {
                clearFolder(outputFile);
            }

            Files.delete(outputFile);
        }
    }

    private void compare(Path expected, Path result) throws IOException, InterruptedException {
        String fileName = expected.toString().replaceAll("\\\\", "-");
        Path expectedRender = Paths.get("renders/" + fileName + "-expected.png");
        Path resultRender = Paths.get("renders/" + fileName + "-result.png");

        render(expected, expectedRender);
        render(result, resultRender);

        if (!Arrays.equals(Files.readAllBytes(expectedRender), Files.readAllBytes(resultRender))) {
            saveDiffImage(expectedRender, resultRender, Paths.get("renders/" + fileName + "-diff.png"));
            fail("The compressed version of " + expected + " is not pixel-perfect.");
        }
        
        Files.delete(expectedRender);
        Files.delete(resultRender);
    }

    private static final String PHANTOMJS_LOCATION = "tools\\phantomjs-2.1.1-windows\\";

    private static void render(Path html, Path png) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                PHANTOMJS_LOCATION + "bin\\phantomjs.exe",
                PHANTOMJS_LOCATION + "examples\\rasterize.js",
                html.toAbsolutePath().toUri().toString(),
                png.toString(),
                "1920px")
                .redirectErrorStream(true)
                .directory(new File(System.getProperty("user.dir")));
        Process process = pb.start();

        System.out.println("Output for phantomjs:");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                System.out.println(": " + line);
            }
        }

        int exitStatus = process.waitFor();
        System.out.println("Process ended with status: " + exitStatus);
    }

    private void saveDiffImage(Path expected, Path result, Path diff) throws IOException {
        BufferedImage expectedImage = ImageIO.read(expected.toFile());
        BufferedImage resultImage = ImageIO.read(result.toFile());

        final int w = expectedImage.getWidth(),
                h = expectedImage.getHeight(),
                highlight = Color.MAGENTA.getRGB();
        final int[] p1 = expectedImage.getRGB(0, 0, w, h, null, 0, w);
        final int[] p2 = resultImage.getRGB(0, 0, w, h, null, 0, w);
        
        // compare img1 to img2, pixel by pixel. If different, highlight img1's pixel...
        for (int i = 0; i < p1.length; i++) {
            if (p1[i] != p2[i]) {
                p1[i] = highlight;
            }
        }
        
        // save img1's pixels to a new BufferedImage, and return it...
        final BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, w, h, p1, 0, w);
        
        ImageIO.write(out, "png", diff.toFile());
    }
}
