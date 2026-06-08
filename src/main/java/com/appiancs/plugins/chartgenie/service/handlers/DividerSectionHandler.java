package com.appiancs.plugins.chartgenie.service.handlers;

import java.math.BigInteger;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPBdr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;

import com.appiancs.plugins.chartgenie.dto.structure.ReportSection;

/**
 * Renders a DIVIDER section as a horizontal line using a paragraph bottom border.
 */
public class DividerSectionHandler implements SectionHandler {

    @Override
    public void render(SectionRenderContext context, ReportSection section) throws Exception {
        XWPFDocument doc = context.getDocument();
        XWPFTableCell cell = context.getCell();

        XWPFParagraph para = (cell != null) ? cell.addParagraph() : doc.createParagraph();
        para.setSpacingBefore(120);
        para.setSpacingAfter(120);

        // Use a bottom border on the paragraph as the visual divider line
        CTPPr ppr = para.getCTP().isSetPPr() ? para.getCTP().getPPr() : para.getCTP().addNewPPr();
        CTPBdr pbd = ppr.isSetPBdr() ? ppr.getPBdr() : ppr.addNewPBdr();
        CTBorder bottom = pbd.isSetBottom() ? pbd.getBottom() : pbd.addNewBottom();
        bottom.setVal(STBorder.SINGLE);
        bottom.setColor("BFBFBF");
        bottom.setSz(BigInteger.valueOf(6));
        bottom.setSpace(BigInteger.valueOf(1));
    }
}
