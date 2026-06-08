package com.appiancs.plugins.chartgenie.service.handlers;

import java.math.BigInteger;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appiancs.plugins.chartgenie.dto.structure.ReportSection;

/**
 * Renders a SPACER section as an empty paragraph with configurable height.
 * Height is specified in twips via the section text field (default 240 twips).
 */
public class SpacerSectionHandler implements SectionHandler {

    private static final Logger logger = LoggerFactory.getLogger(SpacerSectionHandler.class);

    @Override
    public void render(SectionRenderContext context, ReportSection section) throws Exception {
        XWPFDocument doc = context.getDocument();
        XWPFTableCell cell = context.getCell();

        // Default spacer height is 240 twips; caller can pass height in text field as integer
        int heightTwips = 240;
        if (section.getText() != null && !section.getText().trim().isEmpty()) {
            try {
                heightTwips = Math.max(20, Math.min(5760, Integer.parseInt(section.getText().trim())));
            } catch (NumberFormatException e) {
                logger.debug("SPACER text '{}' is not a valid integer, using default", section.getText());
            }
        }

        XWPFParagraph spacer = (cell != null) ? cell.addParagraph() : doc.createParagraph();
        spacer.setSpacingBefore(0);
        spacer.setSpacingAfter(0);
        CTPPr ppr = spacer.getCTP().isSetPPr() ? spacer.getCTP().getPPr() : spacer.getCTP().addNewPPr();
        CTSpacing spacing = ppr.isSetSpacing() ? ppr.getSpacing() : ppr.addNewSpacing();
        spacing.setLineRule(STLineSpacingRule.EXACT);
        spacing.setLine(BigInteger.valueOf(heightTwips));
    }
}
