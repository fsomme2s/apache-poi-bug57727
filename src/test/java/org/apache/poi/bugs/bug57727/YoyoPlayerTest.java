package org.apache.poi.bugs.bug57727;

import org.apache.commons.collections4.iterators.PermutationIterator;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.poi.bugs.bug57727.util.WordTestUtil;
import org.apache.poi.xwpf.usermodel.PositionInParagraph;
import org.apache.poi.xwpf.usermodel.TextSegement;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The current implementation of the {@link XWPFParagraph#searchText(String, PositionInParagraph)} method
 * contains a bug, that occurs on a somewhat tricky edge case:
 *
 * Consider the example:
 *
 * Searched = "yoyoPlayer"
 * Text = "Hello yoyoyoPlayer"
 *         01234567^--------^
 *
 * We expect to find "yoyoPlayer" at char 8.
 *
 * Basically it is a buggy implementation of the KMP Algorithm
 * (https://en.wikipedia.org/wiki/Knuth%E2%80%93Morris%E2%80%93Pratt_algorithm)
 *
 * The algorithm works like using two pointers:
 * one iterates over Text, the other saves the position on Searched, up to which we found a match.
 *
 * So the first time the SearchedPointer is moved, is after the textpointer reaches the "y":
 *
 * 1)
 * Searched = "yoyoPlayer"
 *   pointer:  |                //expects a 'y'
 *
 * Text = "Hello yoyoyoPlayer"
 *   pointer:    |              //finds a 'y'
 *
 *
 * Now we find matches until this point:
 *
 * 2)
 * Searched = "yoyoPlayer"
 *                 |            //expects a 'P'
 * Text = "Hello yoyoyoPlayer"
 *                   |          //finds a 'y'
 *
 *
 * The chars do not match. THE PROBLEM HERE is that the SearchedPointer (called 'candCharPos' in the original code)
 * is RESET TO 0:
 *
 * 3)
 * Searched = "yoyoPlayer"
 *             |                //Reset to position 0, "forgetting" that we already matched the first "yo".
 * Text = "Hello yoyoyoPlayer"
 *                   |          //finds a 'y'
 *
 * From this point, the algorithm searches for "yoyoPlayer" in the remaining text "yoPlayer".
 * And the result, of course, is null (Not Found)!
 *
 * ----
 *
 * Here a more mind twisting example:
 * Searched = "ababaca"
 *                  |       //expects 'c'
 * Text = "abababaca"
 *              |           //finds 'b'
 *
 * Expected Result:
 *
 *
 *
 *
 * My fix is based on the KMP Algorithm article of wikipedia and the following gist:
 * https://gist.github.com/vinnyoodles/c643c6f9d3c36771935459c8534da20b
 *
 *
 * TODO: It would be less complicated if we would just use Java String's indexOf Method on the paragraph's text
 * TODO: Then we "only" need to re-calculate the Run- and Char-Positions from that index?
 * something like:
 * int i = paragraph.getText().indexOf("searched");
 * int beginRun = getRunAt(i);
 * int beginChar = i - lengthOfAllRunsBefore(beginRun);
 *
 *
 */
public class YoyoPlayerTest {

    /**
     * Demonstrates how the current implementation fails.
     */
    @Test
    public void testFail() {
        //Prepare Test Data:
        XWPFParagraph paragraph = WordTestUtil.createParagraphWithRuns("yoyoyoPlayer");

        //Execute:
        TextSegement textSegement = paragraph.searchText("yoyoPlayer", new PositionInParagraph());

        //Assert:
        Assert.assertNotNull(textSegement); //fails
    }

    @Test
    public void testPass() {
        //Prepare Test Data:
        XWPFParagraph paragraph = WordTestUtil.createParagraphWithRuns("yoyoyoPlayer");

        //Execute:
        TextSegement textSegement =
                new FixedParagraphSearchText().searchText(paragraph, "yoyoPlayer", new PositionInParagraph());

        //Assert:
        Assert.assertNotNull(textSegement); //passes
    }

    @Test
    public void test1() {
        //Prepare Test Data:
        XWPFParagraph paragraph = WordTestUtil.createParagraphWithRuns("abababaca");
        //                                                                ababaca
        //Execute:
        TextSegement textSegement =
                new FixedParagraphSearchText().searchText(paragraph, "ababaca", new PositionInParagraph());

        //Assert:
        Assert.assertNotNull(textSegement); //fails
        Assert.assertEquals(2, textSegement.getBeginChar());
    }

    @Test
    public void test2() {
        //Prepare Test Data:
        XWPFParagraph paragraph = WordTestUtil.createParagraphWithRuns("abaababcaa");
        //                                                                 ababcaa
        //Execute:
        TextSegement textSegement =
                new FixedParagraphSearchText().searchText(paragraph, "ababcaa", new PositionInParagraph());

        //Assert:
        Assert.assertNotNull(textSegement); //fails
        Assert.assertEquals(3, textSegement.getBeginChar());
    }

    @Test
    public void test2AcrossRuns() {
        //Prepare Test Data:
        XWPFParagraph paragraph = WordTestUtil.createParagraphWithRuns("ab", "aababcaa");
        //                                                                     ababcaa
        //Execute:
        TextSegement textSegement =
                new FixedParagraphSearchText().searchText(paragraph, "ababcaa", new PositionInParagraph());

        //Assert:
        Assert.assertNotNull(textSegement); //fails
        Assert.assertEquals(1, textSegement.getBeginChar());
    }




    @Test
    @Ignore //Takes some time - starting it manually
    public void exhaustiveTest() {
        FixedParagraphSearchText underTest = new FixedParagraphSearchText();

        List<Character> chars = Arrays.asList(new Character[]{'a', 'a', 'a', 'a', 'b', 'b', 'c',});
        PermutationIterator<Character> searchedIterator = new PermutationIterator<>(chars);

        while (searchedIterator.hasNext()) {
            String searched = new String(ArrayUtils.toPrimitive((Character[]) searchedIterator.next().toArray(new Character[chars.size()])));


            for (int prefixLength = 0; prefixLength <= 4; prefixLength++) {
                for (int suffixLength = 0; suffixLength <= 4; suffixLength++) {
                    String prefix = searched.substring(0, prefixLength);
                    String suffix = searched.substring(searched.length() - suffixLength);
                    String text = prefix + searched + suffix;

                    XWPFParagraph paragraph = WordTestUtil.createParagraphWithRuns(text);

                    System.out.printf("Searching for '%s' in '%s'.\n", searched, text);

                    TextSegement textSegement = underTest.searchText(paragraph, searched, new PositionInParagraph());
                    if (textSegement == null) {
                        System.out.println("^----------------------- /!\\ not found /!\\ ---------------------^");
                    }
                    Assert.fail(String.format("searched='%s' - text='%s'.\n", searched, text));
                    Assert.assertEquals(text.indexOf(searched), textSegement.getBeginChar());
                }
            }
        }
    }
}
