package com.appiancs.plugins.chartgenie.service.handlers;

import java.io.ByteArrayInputStream;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.dto.structure.ReportSection;
import com.appiancs.plugins.chartgenie.service.ChartGenerationService;

/**
 * Renders a CHART section by generating a chart image and embedding it in the document.
 */
public class ChartSectionHandler implements SectionHandler {

    private static final int FONT_SIZE_H2 = 11;
    private static final int CHART_SIDEBAR_WIDTH_PT = 115;
    private static final double CHART_WIDTH_MULTIPLIER = 635.0;
    private static final double CHART_MAX_WIDTH_PCT = 0.95;

    @Override
    public void render(SectionRenderContext context, ReportSection section) throws Exception {
        if (section.getChartConfig() == null) {
            return;
        }

        XWPFDocument doc = context.getDocument();
        XWPFTableCell cell = context.getCell();
        boolean isSidebar = context.isSidebar();
        int availableWidthTwips = context.getAvailableWidthTwips();
        ChartConfiguration config = section.getChartConfig();

        // Create the paragraph for the chart
        XWPFParagraph p = (cell != null) ? cell.addParagraph() : doc.createParagraph();
        if (isSidebar) {
            p.setAlignment(ParagraphAlignment.CENTER);
        }

        // Add title if present
        if (config.getTitle() != null && !config.getTitle().isEmpty()) {
            XWPFRun rTitle = p.createRun();
            rTitle.setText(config.getTitle());
            rTitle.setBold(true);
            rTitle.setFontSize(FONT_SIZE_H2);
            p = (cell != null) ? cell.addParagraph() : doc.createParagraph();
            if (isSidebar)
                p.setAlignment(ParagraphAlignment.CENTER);
        }

        ChartGenerationService chartGen = new ChartGenerationService();
        byte[] chartImageBytes = chartGen.generateChartImage(config);

        try (ByteArrayInputStream is = new ByteArrayInputStream(chartImageBytes)) {
            XWPFRun r = p.createRun();

            double aspectRatio = (double) config.getHeight() / config.getWidth();
            int finalWidthEMU;
            int finalHeightEMU;

            if (isSidebar) {
                finalWidthEMU = Units.toEMU(CHART_SIDEBAR_WIDTH_PT);
                finalHeightEMU = (int) (finalWidthEMU * aspectRatio);
            } else {
                double maxDisplayWidthEMU = (availableWidthTwips * CHART_WIDTH_MULTIPLIER) * CHART_MAX_WIDTH_PCT;
                finalWidthEMU = (int) maxDisplayWidthEMU;
                finalHeightEMU = (int) (finalWidthEMU * aspectRatio);
            }

            r.addPicture(is, XWPFDocument.PICTURE_TYPE_PNG, "chart.png", finalWidthEMU, finalHeightEMU);
        }
    }
}
