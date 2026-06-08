package com.appiancs.plugins.chartgenie.service.handlers;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import com.appiancs.plugins.chartgenie.dto.structure.ReportSection;

/**
 * Renders a HEADING section as a styled Heading1 paragraph.
 */
public class HeadingSectionHandler implements SectionHandler {

    private static final String COLOR_PRIMARY = "00395D";
    private static final int FONT_SIZE_H1 = 14;

    @Override
    public void render(SectionRenderContext context, ReportSection section) throws Exception {
        XWPFParagraph p = (context.getCell() != null)
            ? context.getCell().addParagraph()
            : context.getDocument().createParagraph();

        if (context.isSidebar()) {
            p.setAlignment(ParagraphAlignment.CENTER);
        }

        p.setStyle("Heading1");
        XWPFRun run = p.createRun();
        run.setText(section.getText());
        run.setBold(true);
        run.setFontSize(FONT_SIZE_H1);
        run.setColor(COLOR_PRIMARY);
    }
}
