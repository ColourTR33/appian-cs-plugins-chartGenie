package com.appiancs.plugins.chartgenie.service.handlers;

import java.math.BigInteger;
import java.util.List;

import org.apache.poi.xwpf.usermodel.BodyElementType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblLayoutType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;

import com.appiancs.plugins.chartgenie.dto.structure.ReportSection;

/**
 * Renders a SIDEBAR_LAYOUT section as a two-column table layout.
 * Only renders when not already inside a cell (avoids nested sidebar layouts).
 * <p>
 * This handler requires a {@link SectionProcessor} callback to recursively process
 * sub-sections within the left and right columns.
 */
public class SidebarLayoutSectionHandler implements SectionHandler {

    private static final BigInteger EXACT_LINE_SPACING = BigInteger.valueOf(20);

    /**
     * Callback interface to process nested sections within sidebar columns.
     */
    @FunctionalInterface
    public interface SectionProcessor {
        void processSections(XWPFDocument doc, List<ReportSection> sections, XWPFTableCell cell,
            int availableWidthTwips, boolean isSidebar) throws Exception;
    }

    private final SectionProcessor sectionProcessor;

    public SidebarLayoutSectionHandler(SectionProcessor sectionProcessor) {
        this.sectionProcessor = sectionProcessor;
    }

    @Override
    public void render(SectionRenderContext context, ReportSection section) throws Exception {
        XWPFTableCell cell = context.getCell();

        // Sidebar layout only renders at the document body level
        if (cell != null) {
            return;
        }

        XWPFDocument doc = context.getDocument();
        int availableWidthTwips = context.getAvailableWidthTwips();

        XWPFTable table;
        if (doc.getTables().isEmpty() && !doc.getParagraphs().isEmpty()) {
            try (XmlCursor cursor = doc.getParagraphs().get(0).getCTP().newCursor()) {
                table = doc.insertNewTbl(cursor);
            }
        } else {
            table = doc.createTable();
        }

        XWPFTableRow row = table.getRow(0) == null ? table.createRow() : table.getRow(0);
        if (row.getCell(0) == null)
            row.createCell();
        if (row.getCell(1) == null)
            row.createCell();

        CTTblPr tblPr = getOrCreateTblPr(table);
        removeTableBorders(tblPr);

        CTTblWidth tblInd = tblPr.isSetTblInd() ? tblPr.getTblInd() : tblPr.addNewTblInd();
        tblInd.setType(STTblWidth.DXA);
        tblInd.setW(BigInteger.valueOf(-283));

        double leftRatio = section.getLeftColumnRatio() != null ? section.getLeftColumnRatio() : 0.65;
        int targetWidthTwips = availableWidthTwips + 567;
        int leftWidth = (int) (targetWidthTwips * leftRatio);
        int rightWidth = targetWidthTwips - leftWidth;

        tblPr.addNewTblLayout().setType(STTblLayoutType.FIXED);
        CTTblWidth tblW = tblPr.addNewTblW();
        tblW.setType(STTblWidth.DXA);
        tblW.setW(BigInteger.valueOf(targetWidthTwips));

        XWPFTableCell leftCell = row.getCell(0);
        CTTcPr leftTcPr = getOrCreateTcPr(leftCell);
        setCellWidth(leftTcPr, BigInteger.valueOf(leftWidth));
        if (!leftCell.getParagraphs().isEmpty())
            leftCell.removeParagraph(0);
        if (section.getMainContent() != null)
            sectionProcessor.processSections(doc, section.getMainContent(), leftCell, leftWidth, false);
        cleanCellParagraphs(leftCell);

        XWPFTableCell rightCell = row.getCell(1);
        CTTcPr rightTcPr = getOrCreateTcPr(rightCell);
        setCellWidth(rightTcPr, BigInteger.valueOf(rightWidth));
        if (!rightCell.getParagraphs().isEmpty())
            rightCell.removeParagraph(0);
        if (section.getSidebarContent() != null)
            sectionProcessor.processSections(doc, section.getSidebarContent(), rightCell, rightWidth, true);
        cleanCellParagraphs(rightCell);

        doc.createParagraph();
    }

    // --- Helper methods ---

    private CTTblPr getOrCreateTblPr(XWPFTable table) {
        return table.getCTTbl().getTblPr() != null ? table.getCTTbl().getTblPr() : table.getCTTbl().addNewTblPr();
    }

    private CTTcPr getOrCreateTcPr(XWPFTableCell cell) {
        return cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
    }

    private void setCellWidth(CTTcPr tcPr, BigInteger widthDxa) {
        CTTblWidth tcW = tcPr.isSetTcW() ? tcPr.getTcW() : tcPr.addNewTcW();
        tcW.setType(STTblWidth.DXA);
        tcW.setW(widthDxa);
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

    private void cleanCellParagraphs(XWPFTableCell cell) {
        for (XWPFParagraph p : cell.getParagraphs()) {
            if (p.getText().trim().isEmpty() && p.getRuns().isEmpty()) {
                p.setSpacingAfter(0);
                p.setSpacingBefore(0);
                CTPPr ppr = p.getCTP().isSetPPr() ? p.getCTP().getPPr() : p.getCTP().addNewPPr();
                CTSpacing spacing = ppr.isSetSpacing() ? ppr.getSpacing() : ppr.addNewSpacing();
                spacing.setLineRule(STLineSpacingRule.EXACT);
                spacing.setLine(EXACT_LINE_SPACING);
            }
        }
        if (cell.getParagraphs().isEmpty() ||
            cell.getBodyElements().get(cell.getBodyElements().size() - 1).getElementType() != BodyElementType.PARAGRAPH) {
            cell.addParagraph();
        }
    }
}
