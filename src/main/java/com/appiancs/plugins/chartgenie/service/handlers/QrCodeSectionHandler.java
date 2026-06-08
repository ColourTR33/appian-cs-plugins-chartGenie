package com.appiancs.plugins.chartgenie.service.handlers;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appiancs.plugins.chartgenie.dto.structure.ReportSection;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * Renders a QR_CODE section by encoding the section text as a QR code image.
 */
public class QrCodeSectionHandler implements SectionHandler {

    private static final Logger logger = LoggerFactory.getLogger(QrCodeSectionHandler.class);

    private static final int QR_DIMENSION_PX = 200;
    private static final int QR_DISPLAY_SIZE_PT = 150;

    @Override
    public void render(SectionRenderContext context, ReportSection section) throws Exception {
        XWPFDocument doc = context.getDocument();
        XWPFTableCell cell = context.getCell();

        XWPFParagraph p = (cell != null) ? cell.addParagraph() : doc.createParagraph();
        if (context.isSidebar()) {
            p.setAlignment(ParagraphAlignment.CENTER);
        }

        p.setAlignment(ParagraphAlignment.CENTER);

        // CWE-22/23: anchor QR temp file to real temp dir
        java.nio.file.Path safeTempDir = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir")).toRealPath();
        java.nio.file.Path qrPath = java.nio.file.Files.createTempFile(safeTempDir, "qr_code_", ".png");
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(section.getText(), BarcodeFormat.QR_CODE, QR_DIMENSION_PX, QR_DIMENSION_PX);
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", qrPath);

            try (java.io.InputStream is = java.nio.file.Files.newInputStream(qrPath)) {
                XWPFRun rQR = p.createRun();
                int sizeEmu = Units.toEMU(QR_DISPLAY_SIZE_PT);
                rQR.addPicture(is, XWPFDocument.PICTURE_TYPE_PNG, "qr.png", sizeEmu, sizeEmu);
            }
        } finally {
            if (!qrPath.toFile().delete()) {
                logger.warn("Failed to delete temporary QR code file: {}", qrPath);
            }
        }
    }
}
