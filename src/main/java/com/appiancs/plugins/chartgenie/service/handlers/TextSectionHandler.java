package com.appiancs.plugins.chartgenie.service.handlers;

import java.math.BigInteger;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;

import com.appiancs.plugins.chartgenie.dto.structure.ReportSection;
import com.appiancs.plugins.chartgenie.service.HtmlRichTextRenderer;

/**
 * Renders TEXT, RICH_TEXT, and PARAGRAPH sections.
 * Detects HTML content and delegates to {@link HtmlRichTextRenderer} when appropriate.
 */
public class TextSectionHandler implements SectionHandler {

    private static final java.util.regex.Pattern HTML_TAG_PATTERN = java.util.regex.Pattern
        .compile("(?i).*<(/?)(b|i|u|ul|ol|li|p|br|strong|em)(\\s+[^>]*)?>.*");

    private final HtmlRichTextRenderer htmlRenderer;

    public TextSectionHandler(HtmlRichTextRenderer htmlRenderer) {
        this.htmlRenderer = htmlRenderer;
    }

    @Override
    public void render(SectionRenderContext context, ReportSection section) throws Exception {
        XWPFDocument doc = context.getDocument();
        XWPFTableCell cell = context.getCell();
        int availableWidthTwips = context.getAvailableWidthTwips();

        String content = section.getText();
        String type = section.getType() != null ? section.getType().toUpperCase(java.util.Locale.ROOT).trim() : "TEXT";
        boolean isHtml = content != null && HTML_TAG_PATTERN.matcher(content).matches();

        if (isHtml || "RICH_TEXT".equals(type)) {
            processRichTextContent(doc, cell, content, availableWidthTwips, context.isSidebar());
        } else {
            // Plain text — needs a paragraph
            XWPFParagraph p = (cell != null) ? cell.addParagraph() : doc.createParagraph();
            if (context.isSidebar()) {
                p.setAlignment(ParagraphAlignment.CENTER);
            }
            XWPFRun run = p.createRun();
            run.setText(content);
        }
    }

    private void processRichTextContent(XWPFDocument doc, XWPFTableCell cell, String content,
        int availableWidthTwips, boolean isSidebar) {
        if (cell != null) {
            htmlRenderer.render(doc, cell, content);
        } else {
            // Create a borderless wrapper table for rich text in the document body
            XWPFParagraph p = doc.createParagraph();
            if (isSidebar) {
                p.setAlignment(ParagraphAlignment.CENTER);
            }

            XWPFTable rtTable = doc.createTable(1, 1);
            CTTblPr rtTblPr = getOrCreateTblPr(rtTable);
            removeTableBorders(rtTblPr);

            CTTblWidth rtTblW = rtTblPr.addNewTblW();
            rtTblW.setType(STTblWidth.DXA);
            rtTblW.setW(BigInteger.valueOf(availableWidthTwips));

            XWPFTableCell rtCell = rtTable.getRow(0).getCell(0);
            CTTcPr rtTcPr = getOrCreateTcPr(rtCell);
            setCellMargins(rtTcPr, BigInteger.ZERO, BigInteger.ZERO);

            if (!rtCell.getParagraphs().isEmpty()) {
                rtCell.removeParagraph(0);
            }

            htmlRenderer.render(doc, rtCell, content);
            doc.createParagraph();

            // Remove the placeholder paragraph created before the table
            int pos = doc.getPosOfParagraph(p);
            if (pos >= 0)
                doc.removeBodyElement(pos);
        }
    }

    // --- Helper methods ---

    private CTTblPr getOrCreateTblPr(XWPFTable table) {
        return table.getCTTbl().getTblPr() != null ? table.getCTTbl().getTblPr() : table.getCTTbl().addNewTblPr();
    }

    private CTTcPr getOrCreateTcPr(XWPFTableCell cell) {
        return cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
    }

    private void setCellMargins(CTTcPr tcPr, BigInteger marginY, BigInteger marginX) {
        CTTcMar mar = tcPr.isSetTcMar() ? tcPr.getTcMar() : tcPr.addNewTcMar();
        mar.addNewTop().setW(marginY);
        mar.addNewBottom().setW(marginY);
        mar.addNewLeft().setW(marginX);
        mar.addNewRight().setW(marginX);
    }

    private void removeTableBorders(CTTblPr tblPr) {
        CTTblBorders borders = tblPr.isSetTblBorders() ? tblPr.getTblBorders() : tblPr.addNewTblBorders();
        borders.addNewBottom().setVal(STBorder.NONE);
        borders.addNewLeft().setVal(STBorder.NONE);
        borders.addNewRight().setVal(STBorder.NONE);
        borders.addNewTop().setVal(STBorder.NONE);
        borders.addNewInsideH().setVal(STBorder.NONE);
        borders.addNewInsideV().setVal(STBorder.NONE);
    }
}
