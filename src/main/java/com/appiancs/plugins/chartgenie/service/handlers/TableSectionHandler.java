package com.appiancs.plugins.chartgenie.service.handlers;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;

import com.appiancs.plugins.chartgenie.dto.structure.ReportSection;
import com.appiancs.plugins.chartgenie.service.TableGenerator;

/**
 * Renders a REPORT_TABLE section by delegating to {@link TableGenerator}.
 */
public class TableSectionHandler implements SectionHandler {

    private final TableGenerator tableGenerator;

    public TableSectionHandler(TableGenerator tableGenerator) {
        this.tableGenerator = tableGenerator;
    }

    @Override
    public void render(SectionRenderContext context, ReportSection section) throws Exception {
        if (section.getTableConfig() != null) {
            XWPFDocument doc = context.getDocument();
            XWPFTableCell cell = context.getCell();

            tableGenerator.createStyledTable(doc, cell, section.getTableConfig());

            if (cell != null) {
                XWPFParagraph sep = cell.addParagraph();
                sep.setSpacingAfter(0);
                sep.setSpacingBefore(0);
            }
        }
    }
}
