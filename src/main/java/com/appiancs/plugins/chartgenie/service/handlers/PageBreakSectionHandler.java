package com.appiancs.plugins.chartgenie.service.handlers;

import org.apache.poi.xwpf.usermodel.BodyElementType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;

import com.appiancs.plugins.chartgenie.dto.structure.ReportSection;

/**
 * Renders a PAGE_BREAK section by setting a page break on the last paragraph.
 * Page breaks are only applied when rendering to the document body (not inside cells).
 */
public class PageBreakSectionHandler implements SectionHandler {

    @Override
    public void render(SectionRenderContext context, ReportSection section) throws Exception {
        XWPFDocument doc = context.getDocument();
        XWPFTableCell cell = context.getCell();

        if (cell == null) {
            int lastIdx = doc.getBodyElements().size() - 1;
            if (lastIdx >= 0 && doc.getBodyElements().get(lastIdx).getElementType() == BodyElementType.PARAGRAPH) {
                ((XWPFParagraph) doc.getBodyElements().get(lastIdx)).setPageBreak(true);
            } else {
                doc.createParagraph().setPageBreak(true);
            }
        }
    }
}
