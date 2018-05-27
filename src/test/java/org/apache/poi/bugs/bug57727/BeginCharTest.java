package org.apache.poi.bugs.bug57727;

import org.apache.poi.bugs.bug57727.util.WordTestUtil;
import org.apache.poi.xwpf.usermodel.PositionInParagraph;
import org.apache.poi.xwpf.usermodel.TextSegement;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.Assert;
import org.junit.Test;

/**
 * The current implementation of the {@link XWPFParagraph#searchText(String, PositionInParagraph)} method
 * contains a bug: the beginChar is alway 0, when the searched text is split across runs.
 *
 * TODO: I have no clue what the "beginText" thing does - there is no JDoc at TextSegment or PositionInParagraph
 * TODO: but the beginText variable might has the same problem like the beginChar - it's alway reseted to 0 with every
 * TODO: new Run that is visited by the algorithm.
 */
public class BeginCharTest {

    /**
     * Demonstrates how the current implementation fails.
     */
    @Test
    public void testOriginalSearchTextShouldReturnCorrectPositions() {
        //Prepare Test Data:
        XWPFParagraph paragraph = WordTestUtil.createParagraphWithRuns("hello ", "example foo", "bar baz");
        //                                                 "foobar" split across 2 Runs:  ^--------^

        //Execute:
        TextSegement textSegement = paragraph.searchText("foobar", new PositionInParagraph());

        //Assert:
        Assert.assertEquals(1, textSegement.getBeginRun());  //passes
        Assert.assertEquals(8, textSegement.getBeginChar()); //fails - expected: char 8 in run 1
    }

    /**
     * Fixed implementation of {@link FixedParagraphSearchText#searchText(XWPFParagraph, String, PositionInParagraph)}
     * is moving the "beginCharPos" up above the loop that iterates on the runs. this way, beginCharPos is not set 0
     * with every new run.
     */
    @Test
    public void testFixed() {
        //Prepare Test Data:
        XWPFParagraph paragraph = WordTestUtil.createParagraphWithRuns("hello ", "example foo", "bar baz");
        //                                                 "foobar" split across 2 Runs:  ^--------^

        //Execute:
        TextSegement textSegement =
                new FixedParagraphSearchText().searchText(paragraph, "foobar", new PositionInParagraph());

        //Assert:
        Assert.assertEquals(1, textSegement.getBeginRun());  //passes
        Assert.assertEquals(8, textSegement.getBeginChar()); //passes
    }


}