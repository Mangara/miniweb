/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jsoup.nodes;

import java.util.Arrays;
import org.jsoup.Jsoup;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Sander Verdonschot <sander.verdonschot at gmail.com>
 */
public class MinifyVisitorTest {

    public MinifyVisitorTest() {
    }

    private void testPage(String input, String expected) {
        assertEquals(expected,
                MinifyVisitor.minify(Jsoup.parse(input)));
    }

    private void testBodySnippet(String input, String expected) {
        assertEquals("<html><head></head><body>" + expected + "</body></html>",
                MinifyVisitor.minify(Jsoup.parse("<html><head></head><body>" + input + "</body></html>")));
    }

    @Test
    public void testMinifyExamplePage() {
        testPage("<!DOCTYPE html>\n"
                + "<html>\n"
                + "<head>\n"
                + "<meta charset=\"UTF-8\">\n"
                + "<title>Title of the document</title>\n"
                + "</head>\n"
                + "\n"
                + "<body>\n"
                + "Content of the document......\n"
                + "</body>\n"
                + "\n"
                + "</html> ",
                "<!doctype html><html><head><meta charset=UTF-8><title>Title of the document</title></head> <body>Content of the document......</body></html>");
    }

    @Test
    public void testBasicWhitespaceReduction() {
        // Basic white-space reduction between same-level elements
        testBodySnippet("<span>1</span> <span>2</span>", "<span>1</span> <span>2</span>");
        testBodySnippet("<span>1</span>       <span>2</span>", "<span>1</span> <span>2</span>");
        testBodySnippet("   <span>1</span>\n   <span>2</span>   ", "<span>1</span> <span>2</span>");
        testBodySnippet("<span>1</span><span>2</span>", "<span>1</span><span>2</span>");

        // Basic white-space reduction between nested elements
        testBodySnippet("<div><span>1</span></div>", "<div><span>1</span></div>");
        testBodySnippet("<div>\n<span>1</span>\n</div>", "<div><span>1</span></div>");
        testBodySnippet("   <div>\n     <span>1</span>\n   </div>", "<div><span>1</span></div>");
        testBodySnippet("<div> <span>1</span> </div>", "<div><span>1</span></div>");
    }

    @Test
    public void testCaseNormalization() {
        testBodySnippet("<P>foo</p>", "<p>foo</p>");
        testBodySnippet("<DIV>boo</DIV>", "<div>boo</div>");
        testBodySnippet("<DIV title=\"moo\">boo</DiV>", "<div title=moo>boo</div>");
        testBodySnippet("<DIV TITLE=\"blah\">boo</DIV>", "<div title=blah>boo</div>");
        testBodySnippet("<DIV tItLe=\"blah\">boo</DIV>", "<div title=blah>boo</div>");
        testBodySnippet("<DiV tItLe=\"blah\">boo</DIV>", "<div title=blah>boo</div>");
    }

    @Test
    public void testSpaceNormalizationBetweenAttributes() {
        testBodySnippet("<p title=\"bar\">foo</p>", "<p title=bar>foo</p>");
        testBodySnippet("<img src=\"test\"/>", "<img src=test>");
        testBodySnippet("<p title = \"bar\">foo</p>", "<p title=bar>foo</p>");
        testBodySnippet("<p title\n\n\t  =\n     \"bar\">foo</p>", "<p title=bar>foo</p>");
        testBodySnippet("<img src=\"test\" \n\t />", "<img src=test>");
        testBodySnippet("<input title=\"bar\"       id=\"boo\"    value=\"hello world\">", "<input title=bar id=boo value=\"hello world\">");
    }

    @Test
    public void testSpaceNormalizationAroundText() {
        testBodySnippet("   <p>blah</p>\n\n\n   ", "<p>blah</p>");

        Arrays.asList("a", "b", "big", "button", "code", "em", "font", "i", "kbd", "mark", "q", "s", "small", "span", "strike", "strong", "sub", "sup", "time", "tt", "u")
                .stream().forEach(el -> {
                    testBodySnippet("<p>foo <" + el + ">baz</" + el + "> bar</p>", "<p>foo <" + el + ">baz</" + el + "> bar</p>");
                    testBodySnippet("<p>foo<" + el + ">baz</" + el + ">bar</p>", "<p>foo<" + el + ">baz</" + el + ">bar</p>");
                    testBodySnippet("<p>foo <" + el + ">baz</" + el + ">bar</p>", "<p>foo <" + el + ">baz</" + el + ">bar</p>");
                    testBodySnippet("<p>foo<" + el + ">baz</" + el + "> bar</p>", "<p>foo<" + el + ">baz</" + el + "> bar</p>");
                    testBodySnippet("<p>foo <" + el + "> baz </" + el + "> bar</p>", "<p>foo <" + el + ">baz</" + el + "> bar</p>");
                    testBodySnippet("<p>foo<" + el + "> baz </" + el + ">bar</p>", "<p>foo<" + el + ">baz</" + el + ">bar</p>");
                    testBodySnippet("<p>foo <" + el + "> baz </" + el + ">bar</p>", "<p>foo <" + el + ">baz</" + el + ">bar</p>");
                    testBodySnippet("<p>foo<" + el + "> baz </" + el + "> bar</p>", "<p>foo<" + el + ">baz</" + el + "> bar</p>");
                });

        testBodySnippet("<p>foo <img> bar</p>", "<p>foo <img> bar</p>");
        testBodySnippet("<p>foo<img>bar</p>", "<p>foo<img>bar</p>");
        testBodySnippet("<p>foo <img>bar</p>", "<p>foo <img>bar</p>");
        testBodySnippet("<p>foo<img> bar</p>", "<p>foo<img> bar</p>");
    }

    @Test
    public void testRemovingComments() {
        testBodySnippet("<!-- test -->", "");
        testBodySnippet("<!-- foo --><div>baz</div><!-- bar\n\n moo -->", "<div>baz</div>");
        testBodySnippet("<p title=\"<!-- comment in attribute -->\">foo</p>", "<p title=\"<!-- comment in attribute -->\">foo</p>");
        testBodySnippet("<script><!-- alert(1) --></script>", "<script><!-- alert(1) --></script>");
        testBodySnippet("<STYLE><!-- alert(1) --></STYLE>", "<style><!-- alert(1) --></style>");
    }

    @Test
    public void testRemovingEmptyAttributes() {
        testBodySnippet("<p id=\"\" class=\"\" STYLE=\" \" title=\"\n\" lang=\"\" dir=\"\">x</p>", "<p>x</p>");
        testBodySnippet("<p onclick=\"\"   ondblclick=\" \" onmousedown=\"\" ONMOUSEUP=\"\" onmouseover=\" \" onmousemove=\"\" onmouseout=\"\" onkeypress=\n\n  \"\n     \" onkeydown=\n\"\" onkeyup\n=\"\">x</p>", "<p>x</p>");
        testBodySnippet("<input onfocus=\"\" onblur=\"\" onchange=\" \" value=\" boo \">", "<input value=\" boo \">");
        testBodySnippet("<input value=\"\" name=\"foo\">", "<input name=foo>");

        // Preserve image src and alt attributes
        testBodySnippet("<img src=\"\" alt=\"\">", "<img src alt>");
        testBodySnippet("<img src alt>", "<img src alt>");

        // Preserve unrecognized attributes
        testBodySnippet("<div data-foo class id style title lang dir onfocus onblur onchange onclick ondblclick onmousedown onmouseup onmouseover onmousemove onmouseout onkeypress onkeydown onkeyup></div>", "<div data-foo></div>");
    }
}
