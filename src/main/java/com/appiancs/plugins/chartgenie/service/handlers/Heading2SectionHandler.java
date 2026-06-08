package com.appiancs.plugins.chartgenie.service.handlers;

import java.math.BigInteger;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;

import com.appiancs.plugins.chartgenie.dto.structure.ReportSection;

/**
 * Renders a HEADING2 section as a coloured banner table cell.
 */
public class Heading2SectionHandler implements SectionHandler {

    private static final String COLOR_PRIMARY = "00395D";
    private static final String COLOR_WHITE = "FFFFFF";
    private static final int FONT_SIZE_H2 = 11;
    private static final BigInteger BADGE_TOTAL_WIDTH = BigInteger.valueOf(5000);
    private static final BigInteger BADGE_MARGIN_Y = BigInteger.valueOf(100);
    private static final BigInteger BADGE_MARGIN_X = BigInteger.valueOf(150);

    @Override
    public void render(SectionRenderContext context, ReportSection section) throws Exception {
        XWPFDocument doc = context.getDocument();
        XWPFTableCell cell = context.getCell();

        XWPFTable h2Table = createInlineTable(doc, cell, 1, 1);
        XWPFTableRow h2Row = getOrCreateFirstRow(h2Table, 1);

        CTTblPr h2TblPr = getOrCreateTblPr(h2Table);
        h2TblPr.addNewTblW().setType(STTblWidth.DXA);
        h2TblPr.getTblW().setW(BADGE_TOTAL_WIDTH);
        removeTableBorders(h2TblPr);

        XWPFTableCell h2Cell = h2Row.getCell(0);
        h2Cell.setColor(COLOR_PRIMARY);

        CTTcPr h2TcPr = getOrCreateTcPr(h2Cell);
        setCellWidth(h2TcPr, BADGE_TOTAL_WIDTH);
        if (!h2TcPr.isSetNoWrap())
            h2TcPr.addNewNoWrap();
        setCellMargins(h2TcPr, BADGE_MARGIN_Y, BADGE_MARGIN_X);

        XWPFParagraph h2P = h2Cell.getParagraphs().isEmpty() ? h2Cell.addParagraph() : h2Cell.getParagraphs().get(0);
        h2P.setSpacingBefore(0);
        h2P.setSpacingAfter(0);
        XWPFRun h2R = h2P.createRun();
        h2R.setText(section.getText());
        h2R.setBold(true);
        h2R.setColor(COLOR_WHITE);
        h2R.setFontSize(FONT_SIZE_H2);
    }

    // --- Helper methods (extracted from WordDocumentService) ---

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
