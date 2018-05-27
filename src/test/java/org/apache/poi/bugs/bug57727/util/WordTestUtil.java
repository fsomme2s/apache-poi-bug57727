package org.apache.poi.bugs.bug57727.util;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

/**
 * Test Util to create simulated docx documents as test input.
 */
public class WordTestUtil {
    /**
     * Creates a document containing exactly one paragraph. The paragraph contains one run per element in runTexts.
     *
     * @param runTexts list of texts - one text = one run.
     * @return the created document.
     */
    public static XWPFDocument createOneParagraphDocxWithRuns(String... runTexts) {
        XWPFDocument xwpfDocument = new XWPFDocument();

        @SuppressWarnings("resource")
        XWPFParagraph paragraph = xwpfDocument.createParagraph();

        for (String runText : runTexts) {
            XWPFRun run = paragraph.createRun();
            run.setText(runText, 0);
        }

        return xwpfDocument;
    }

    /**
     * Creates a paragraph containing one run per element in runTexts.
     * <p>
     *  Convenience Method for {@link #createOneParagraphDocxWithRuns(String...)} - returning the paragraph instead
     *  of the document.
     * </p>
     *
     * @param runTexts list of texts - one text = one run.
     * @return the created paragraph.
     */
    public static XWPFParagraph createParagraphWithRuns(String... runTexts) {
        return createOneParagraphDocxWithRuns(runTexts).getParagraphs().get(0);
    }
}
