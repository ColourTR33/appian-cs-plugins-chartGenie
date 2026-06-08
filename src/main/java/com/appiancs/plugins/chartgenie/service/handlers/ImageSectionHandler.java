package com.appiancs.plugins.chartgenie.service.handlers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appiancs.plugins.chartgenie.dto.structure.ReportSection;
import com.appiancorp.suiteapi.content.ContentService;

/**
 * Renders an IMAGE section by downloading the image from Appian content services
 * and embedding it in the document.
 */
public class ImageSectionHandler implements SectionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ImageSectionHandler.class);

    private static final int PAGE_CONTENT_WIDTH_TWIPS = 11054;
    private static final double CHART_WIDTH_MULTIPLIER = 635.0;

    private ContentService contentService;

    public void setContentService(ContentService contentService) {
        this.contentService = contentService;
    }

    @Override
    public void render(SectionRenderContext context, ReportSection section) throws Exception {
        if (section.getImageDocumentId() == null) {
            logger.warn("IMAGE section has no imageDocumentId, skipping.");
            return;
        }
        if (contentService == null) {
            logger.warn("IMAGE section requires contentService — not available in this context, skipping.");
            return;
        }

        XWPFDocument doc = context.getDocument();
        XWPFTableCell cell = context.getCell();

        try {
            com.appiancorp.suiteapi.knowledge.Document imgDoc = contentService.download(section.getImageDocumentId(),
                com.appiancorp.suiteapi.content.ContentConstants.VERSION_CURRENT, false)[0];

            byte[] imgBytes;
            try (java.io.InputStream in = imgDoc.getInputStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) {
                    baos.write(buf, 0, n);
                }
                imgBytes = baos.toByteArray();
            }

            // Determine display width in EMU based on widthPercent (default 90%)
            int widthPercent = (section.getImageWidthPercent() != null && section.getImageWidthPercent() > 0 &&
                section.getImageWidthPercent() <= 100)
                    ? section.getImageWidthPercent()
                    : 90;
            int availTwips = PAGE_CONTENT_WIDTH_TWIPS;
            int widthEmu = (int) (availTwips * (widthPercent / 100.0) * CHART_WIDTH_MULTIPLIER);

            // Detect image type from extension
            String ext = imgDoc.getExtension() != null
                ? imgDoc.getExtension().toLowerCase(java.util.Locale.ROOT)
                : "png";
            int pictureType = "jpg".equals(ext) || "jpeg".equals(ext)
                ? XWPFDocument.PICTURE_TYPE_JPEG
                : XWPFDocument.PICTURE_TYPE_PNG;

            // Resolve paragraph alignment
            ParagraphAlignment alignment = ParagraphAlignment.CENTER;
            if (section.getImageAlignment() != null) {
                switch (section.getImageAlignment().toUpperCase(java.util.Locale.ROOT)) {
                    case "LEFT":
                        alignment = ParagraphAlignment.LEFT;
                        break;
                    case "RIGHT":
                        alignment = ParagraphAlignment.RIGHT;
                        break;
                    default:
                        break;
                }
            }

            XWPFParagraph imgPara = (cell != null) ? cell.addParagraph() : doc.createParagraph();
            imgPara.setAlignment(alignment);
            XWPFRun imgRun = imgPara.createRun();
            try (ByteArrayInputStream bis = new ByteArrayInputStream(imgBytes)) {
                imgRun.addPicture(bis, pictureType, "image." + ext, widthEmu,
                    Units.toEMU(widthEmu / 72.0 * 96)); // approximate height — aspect ratio preserved by Word
            }
        } catch (Exception e) {
            logger.error("Failed to embed IMAGE section (docId={}): {}",
                section.getImageDocumentId(), e.getMessage(), e);
        }
    }
}
