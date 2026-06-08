package com.appiancs.plugins.chartgenie.service.handlers;

import java.math.BigInteger;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STJcTable;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appiancs.plugins.chartgenie.dto.structure.ReportSection;

/**
 * Renders a STATUS_BADGE section as a right-aligned badge with optional title label.
 */
public class StatusBadgeSectionHandler implements SectionHandler {

    private static final Logger logger = LoggerFactory.getLogger(StatusBadgeSectionHandler.class);

    private static final String COLOR_PRIMARY = "00395D";
    private static final String COLOR_WHITE = "FFFFFF";
    private static final int FONT_SIZE_BADGE = 10;
    private static final BigInteger BADGE_TOTAL_WIDTH = BigInteger.valueOf(5000);
    private static final BigInteger BADGE_LABEL_WIDTH = BigInteger.valueOf(1500);
    private static final BigInteger BADGE_VALUE_WIDTH = BigInteger.valueOf(3500);
    private static final BigInteger BADGE_MARGIN_Y = BigInteger.valueOf(100);
    private static final BigInteger BADGE_MARGIN_X = BigInteger.valueOf(150);

    @Override
    public void render(SectionRenderContext context, ReportSection section) throws Exception {
        XWPFDocument doc = context.getDocument();
        XWPFTableCell cell = context.getCell();

        boolean hasTitle = section.getTitle() != null && !section.getTitle().trim().isEmpty();
        int numCols = hasTitle ? 2 : 1;

        XWPFTable bTable = createInlineTable(doc, cell, 1, numCols);
        XWPFTableRow bRow = getOrCreateFirstRow(bTable, numCols);

        if (bRow == null) {
            logger.warn("Failed to create table row for status badge");
            return;
        }

        CTTblPr bTblPr = getOrCreateTblPr(bTable);
        bTblPr.addNewJc().setVal(STJcTable.RIGHT);
        bTblPr.addNewTblW().setType(STTblWidth.DXA);
        bTblPr.getTblW().setW(hasTitle ? BADGE_TOTAL_WIDTH : BADGE_VALUE_WIDTH);
        removeTableBorders(bTblPr);

        int currentCellIdx = 0;

        if (hasTitle) {
            XWPFTableCell lblCell = bRow.getCell(currentCellIdx++);
            if (lblCell == null) {
                logger.warn("Failed to get label cell for status badge");
                return;
            }

            CTTcPr lblTcPr = getOrCreateTcPr(lblCell);
            setCellWidth(lblTcPr, BADGE_LABEL_WIDTH);
            if (!lblTcPr.isSetNoWrap())
                lblTcPr.addNewNoWrap();

            lblCell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);

            XWPFParagraph lblP = lblCell.getParagraphs().isEmpty() ? lblCell.addParagraph() : lblCell.getParagraphs().get(0);
            lblP.setAlignment(ParagraphAlignment.RIGHT);
            lblP.setSpacingBefore(0);
            lblP.setSpacingAfter(0);
            XWPFRun lblR = lblP.createRun();
            lblR.setText(section.getTitle() + "  ");
            lblR.setBold(true);
            lblR.setFontSize(FONT_SIZE_BADGE);
        }

        XWPFTableCell valCell = bRow.getCell(currentCellIdx);
        if (valCell == null) {
            logger.warn("Failed to get value cell for status badge");
            return;
        }

        CTTcPr valTcPr = getOrCreateTcPr(valCell);
        setCellWidth(valTcPr, BADGE_VALUE_WIDTH);
        setCellMargins(valTcPr, BADGE_MARGIN_Y, BADGE_MARGIN_X);
        if (!valTcPr.isSetNoWrap())
            valTcPr.addNewNoWrap();

        String hexColor = (section.getAccentColor() != null) ? section.getAccentColor().replace("#", "") : COLOR_PRIMARY;
        valCell.setColor(hexColor);
        valCell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);

        XWPFParagraph valP = valCell.getParagraphs().isEmpty() ? valCell.addParagraph() : valCell.getParagraphs().get(0);
        valP.setAlignment(ParagraphAlignment.CENTER);
        valP.setSpacingBefore(0);
        valP.setSpacingAfter(0);
        XWPFRun valR = valP.createRun();
        valR.setText(" " + section.getText().toUpperCase(java.util.Locale.ROOT) + " ");
        valR.setBold(true);
        valR.setColor(COLOR_WHITE);
        valR.setFontSize(FONT_SIZE_BADGE);
    }

    // --- Helper methods ---

    private XWPFTable createInlineTable(XWPFDocument doc, XWPFTableCell cell, int rows, int cols) {
        if (cell != null) {
            XWPFParagraph sep = cell.addParagraph();
            sep.setSpacingAfter(0);
            sep.setSpacingBefore(0);
            try (XmlCursor cursor = sep.getCTP().newCursor()) {
                return cell.insertNewTbl(cursor);
            }
        } else {
            XWPFTable table = doc.createTable(rows, cols);
            doc.createParagraph();
            return table;
        }
    }

    private XWPFTableRow getOrCreateFirstRow(XWPFTable table, int requiredCols) {
        XWPFTableRow row;
        if (table.getRows().isEmpty()) {
            row = table.createRow();
        } else {
            row = table.getRow(0);
        }
        if (row != null) {
            while (row.getTableCells().size() < requiredCols) {
                row.createCell();
            }
        } else {
            row = table.createRow();
            for (int i = 0; i < requiredCols; i++) {
                row.createCell();
            }
        }
        return row;
    }

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
