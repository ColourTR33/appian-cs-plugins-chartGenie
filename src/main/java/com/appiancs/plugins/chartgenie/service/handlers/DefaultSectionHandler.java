package com.appiancs.plugins.chartgenie.service.handlers;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import com.appiancs.plugins.chartgenie.dto.structure.ReportSection;

/**
 * Default handler for unrecognized section types.
 * Renders the section text as a plain paragraph without throwing an exception.
 */
public class DefaultSectionHandler implements SectionHandler {

    @Override
    public void render(SectionRenderContext context, ReportSection section) throws Exception {
        XWPFParagraph p = (context.getCell() != null)
            ? context.getCell().addParagraph()
            : context.getDocument().createParagraph();

        if (context.isSidebar()) {
            p.setAlignment(ParagraphAlignment.CENTER);
        }

        XWPFRun run = p.createRun();
        run.setText(section.getText() != null ? section.getText() : "");
    }
}
