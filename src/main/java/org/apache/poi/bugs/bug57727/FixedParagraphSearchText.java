package org.apache.poi.bugs.bug57727;

import org.apache.poi.xwpf.usermodel.PositionInParagraph;
import org.apache.poi.xwpf.usermodel.TextSegement;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTProofErr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText;

import java.util.ArrayList;

public class FixedParagraphSearchText {
    /**
     * Fixed version of {@link XWPFParagraph#searchText(String, PositionInParagraph)}, returning the proper
     * beginChar values and also works for "the yoyo problem".
     */
    public static TextSegement searchText(XWPFParagraph para, String searched, PositionInParagraph startPos) {
        int startRun = startPos.getRun(),
                startText = startPos.getText(),
                startChar = startPos.getChar();
        int beginRunPos = 0, candCharPos = 0;
        boolean newList = false;

        /* need to move beginCharPos up here, or it will be overwritten with 0 in every new run,
         * when "searched" is split across several runs: */
        int beginCharPos = 0;

        //used to know the position we need to reset the candCharPos to, when a char does not match.
        int[] failureTable = getFailureTable(searched);

        char firstCharOfSearched = searched.charAt(0);

        @SuppressWarnings("deprecation")
        CTR[] rArray = para.getCTP().getRArray();
        for (int runPos = startRun; runPos < rArray.length; runPos++) {
            int beginTextPos = 0, textPos = 0, charPos = 0;
            CTR ctRun = rArray[runPos];
            XmlCursor c = ctRun.newCursor();
            c.selectPath("./*");
            while (c.toNextSelection()) {
                XmlObject o = c.getObject();
                if (o instanceof CTText) {
                    if (textPos >= startText) {
                        String candidate = ((CTText) o).getStringValue();
                        if (runPos == startRun)
                            charPos = startChar;
                        else
                            charPos = 0;

                        for (; charPos < candidate.length(); charPos++) {
                            char candidateChar = candidate.charAt(charPos);
                            char expectedChar = searched.charAt(candCharPos);

                            if (candidateChar == expectedChar) {
                                if (candCharPos == 0) {
                                    // moved this "if" here to be more self-explaining
                                    // before it was line 1492 in XWPFParagraph
                                    beginTextPos = textPos;
                                    beginCharPos = charPos;
                                    beginRunPos = runPos;
                                    newList = true;
                                }

                                if (candCharPos + 1 < searched.length())
                                    candCharPos++;
                                else if (newList) {
                                    TextSegement segement = new TextSegement();
                                    segement.setBeginRun(beginRunPos);
                                    segement.setBeginText(beginTextPos);
                                    segement.setBeginChar(beginCharPos);
                                    segement.setEndRun(runPos);
                                    segement.setEndText(textPos);
                                    segement.setEndChar(charPos);
                                    return segement;
                                }
                            } else if (candCharPos == 0) {
                                candCharPos++;
                            } else {
                                //a char diffed - reset the search position:
                                candCharPos = failureTable[candCharPos];
                                charPos--;
                            }
                        }
                    }

                    textPos++;
                } else if (o instanceof CTProofErr) {
                    c.removeXml();
                } else if (o instanceof CTRPr) ;
                    //do nothing
                else
                    candCharPos = 0;
            }

            c.dispose();
        }
        return null;
    }

    /**
     * Copied from https://gist.github.com/vinnyoodles/c643c6f9d3c36771935459c8534da20b
     */
    private static int[] getFailureTable(String searched) {
        int[] table = new int[searched.length() + 1];
        // state 0 and 1 are guarenteed be the prior
        table[0] = -1;
        table[1] = 0;

        // the pointers pointing at last failure and current satte
        int left = 0;
        int right = 2;

        while (right < table.length) { // RIGHT NEVER MOVES RIGHT UNTIL ASSIGNED A VALID POINTER
            if (searched.charAt(right - 1) == searched.charAt(left)) { // when both chars before left and right are equal, link both and move both forward
                left++;
                table[right] = left;
                right++;
            }  else if (left > 0) { // if left isn't at the very beginning, then send left backward
                // by following the already set pointer to where it is pointing to
                left = table[left];
            } else { // left has fallen all the way back to the beginning
                table[right] = left;
                right++;
            }
        }
        return table;
    }

}
