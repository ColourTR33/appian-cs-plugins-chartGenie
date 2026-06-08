package com.appiancs.plugins.chartgenie.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy;
import org.apache.poi.xwpf.usermodel.BodyElementType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appiancs.plugins.chartgenie.dto.structure.ReportSection;
import com.appiancs.plugins.chartgenie.dto.structure.ReportSettings;
import com.appiancs.plugins.chartgenie.service.handlers.ChartSectionHandler;
import com.appiancs.plugins.chartgenie.service.handlers.DefaultSectionHandler;
import com.appiancs.plugins.chartgenie.service.handlers.DividerSectionHandler;
import com.appiancs.plugins.chartgenie.service.handlers.Heading2SectionHandler;
import com.appiancs.plugins.chartgenie.service.handlers.HeadingSectionHandler;
import com.appiancs.plugins.chartgenie.service.handlers.ImageSectionHandler;
import com.appiancs.plugins.chartgenie.service.handlers.PageBreakSectionHandler;
import com.appiancs.plugins.chartgenie.service.handlers.QrCodeSectionHandler;
import com.appiancs.plugins.chartgenie.service.handlers.SectionHandler;
import com.appiancs.plugins.chartgenie.service.handlers.SectionRenderContext;
import com.appiancs.plugins.chartgenie.service.handlers.SidebarLayoutSectionHandler;
import com.appiancs.plugins.chartgenie.service.handlers.SpacerSectionHandler;
import com.appiancs.plugins.chartgenie.service.handlers.StatusBadgeSectionHandler;
import com.appiancs.plugins.chartgenie.service.handlers.TableSectionHandler;
import com.appiancs.plugins.chartgenie.service.handlers.TextSectionHandler;

public class WordDocumentService {

  private static final Logger logger = LoggerFactory.getLogger(WordDocumentService.class);

  // --- DESIGN CONSTANTS ---
  private static final String COLOR_WHITE = "FFFFFF";
  private static final String COLOR_GREY_TEXT = "666666";

  private static final int FONT_SIZE_HEADER = 16;
  private static final int FONT_SIZE_SUBHEADER = 12;
  private static final int FONT_SIZE_FOOTER = 9;

  // --- LAYOUT CONSTANTS ---
  private static final int PAGE_CONTENT_WIDTH_TWIPS = 11054;
  private static final BigInteger MARGIN_STANDARD = BigInteger.valueOf(1440);
  private static final BigInteger MARGIN_HEADER_Y = BigInteger.valueOf(100);
  private static final BigInteger MARGIN_FOOTER_Y = BigInteger.valueOf(340);
  private static final BigInteger EXACT_LINE_SPACING = BigInteger.valueOf(20);

  private static final long A4_WIDTH_TWIPS = 11906;
  private static final long A4_HEIGHT_TWIPS = 16838;
  private static final long LETTER_WIDTH_TWIPS = 12240;
  private static final long LETTER_HEIGHT_TWIPS = 15840;

  // --- DEPENDENCIES ---
  private final HtmlRichTextRenderer htmlRenderer;
  private final TableGenerator tableGenerator;
  private final TemplateVariableSubstitutor substitutor;

  // --- HANDLER REGISTRY ---
  private final Map<String, SectionHandler> handlers;
  private final SectionHandler defaultHandler = new DefaultSectionHandler();
  private final ImageSectionHandler imageHandler;

  /** Production no-arg constructor — preserves existing behaviour. */
  public WordDocumentService() {
    this(new HtmlRichTextRenderer(), new TableGenerator(), new TemplateVariableSubstitutor());
  }

  /** Parameterized constructor for testing. */
  public WordDocumentService(HtmlRichTextRenderer htmlRenderer,
                             TableGenerator tableGenerator,
                             TemplateVariableSubstitutor substitutor) {
    if (htmlRenderer == null) {
      throw new IllegalArgumentException("htmlRenderer must not be null");
    }
    if (tableGenerator == null) {
      throw new IllegalArgumentException("tableGenerator must not be null");
    }
    if (substitutor == null) {
      throw new IllegalArgumentException("substitutor must not be null");
    }
    this.htmlRenderer = htmlRenderer;
    this.tableGenerator = tableGenerator;
    this.substitutor = substitutor;

    imageHandler = new ImageSectionHandler();

    handlers = new HashMap<>();
    handlers.put("HEADING", new HeadingSectionHandler());
    handlers.put("HEADING2", new Heading2SectionHandler());
    handlers.put("STATUS_BADGE", new StatusBadgeSectionHandler());
    handlers.put("REPORT_TABLE", new TableSectionHandler(tableGenerator));
    handlers.put("TEXT", new TextSectionHandler(htmlRenderer));
    handlers.put("RICH_TEXT", new TextSectionHandler(htmlRenderer));
    handlers.put("PARAGRAPH", new TextSectionHandler(htmlRenderer));
    handlers.put("CHART", new ChartSectionHandler());
    handlers.put("IMAGE", imageHandler);
    handlers.put("PAGE_BREAK", new PageBreakSectionHandler());
    handlers.put("QR_CODE", new QrCodeSectionHandler());
    handlers.put("DIVIDER", new DividerSectionHandler());
    handlers.put("SPACER", new SpacerSectionHandler());
    handlers.put("SIDEBAR_LAYOUT", new SidebarLayoutSectionHandler(this::processSections));
  }

  public void setContentService(com.appiancorp.suiteapi.content.ContentService contentService) {
    imageHandler.setContentService(contentService);
  }

  /**
   * Convenience overload — delegates to the full method with an empty variables map.
   */
  public byte[] generateReport(
    File templateFile, ReportSettings settings, List<ReportSection> sections) throws Exception {
    return generateReport(templateFile, settings, sections, java.util.Collections.emptyMap());
  }

  public byte[] generateReport(
    File templateFile, ReportSettings settings, List<ReportSection> sections,
    java.util.Map<String, String> variables) throws Exception {
    logger.debug("Starting report generation for file: {}", templateFile.getName());

    // CWE-22/23: Resolve canonical path to block any traversal sequences
    java.nio.file.Path safePath = templateFile.toPath().normalize().toRealPath();

    try (java.io.InputStream fis = java.nio.file.Files.newInputStream(safePath);
      XWPFDocument doc = new XWPFDocument(fis)) {

      int currentAvailableWidth = PAGE_CONTENT_WIDTH_TWIPS;

      if (settings != null) {
        currentAvailableWidth = applyPageSettings(doc, settings.getPageSize(), settings.getOrientation());
        applyHeaderFooter(doc, settings, settings.getHeaderColor(), settings.getFooterText());
      }

      // Apply template variable substitution before sections are rendered
      substitutor.substitute(doc, variables);

      if (doc.getBodyElements().size() > 0 && doc.getBodyElements().get(0).getElementType() == BodyElementType.PARAGRAPH) {
        XWPFParagraph firstPara = (XWPFParagraph) doc.getBodyElements().get(0);
        if (firstPara.getText().trim().isEmpty()) {
          firstPara.setSpacingAfter(0);
          firstPara.setSpacingBefore(0);
          CTPPr ppr = firstPara.getCTP().isSetPPr() ? firstPara.getCTP().getPPr() : firstPara.getCTP().addNewPPr();
          CTSpacing spacing = ppr.isSetSpacing() ? ppr.getSpacing() : ppr.addNewSpacing();
          spacing.setLineRule(STLineSpacingRule.EXACT);
          spacing.setLine(EXACT_LINE_SPACING);
        }
      }

      if (sections != null) {
        processSections(doc, sections, null, currentAvailableWidth, false);
      }

      for (XWPFTable t : doc.getTables()) {
        for (XWPFTableRow r : t.getRows()) {
          for (XWPFTableCell c : r.getTableCells()) {
            c.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            for (XWPFParagraph cp : c.getParagraphs()) {
              cp.setSpacingBefore(0);
              cp.setSpacingAfter(0);
            }
          }
        }
      }

      int lastIdx = doc.getBodyElements().size() - 1;
      while (lastIdx >= 0 && doc.getBodyElements().get(lastIdx).getElementType() == BodyElementType.PARAGRAPH) {
        XWPFParagraph lastPara = (XWPFParagraph) doc.getBodyElements().get(lastIdx);
        if (lastPara.getText().trim().isEmpty() && lastPara.getRuns().isEmpty() && !lastPara.isPageBreak()) {
          doc.removeBodyElement(lastIdx);
          lastIdx--;
        } else {
          break;
        }
      }

      try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        doc.write(baos);
        logger.info("Report generation successful.");
        return baos.toByteArray();
      }
    } catch (Exception e) {
      logger.error("Error generating Word report: {}", e.getMessage(), e);
      throw e;
    }
  }

  private void processSections(XWPFDocument doc, List<ReportSection> sections, XWPFTableCell cell,
    int availableWidthTwips, boolean isSidebar) throws Exception {
    for (ReportSection section : sections) {
      String type = section.getType() != null ? section.getType().toUpperCase(java.util.Locale.ROOT).trim() : "TEXT";
      SectionRenderContext context = new SectionRenderContext(doc, cell, availableWidthTwips, isSidebar);
      SectionHandler handler = handlers.getOrDefault(type, defaultHandler);
      handler.render(context, section);
    }
  }

  // --- PAGE SETTINGS ---

  private int applyPageSettings(XWPFDocument doc, String pageSize, String orientation) {
    CTBody body = doc.getDocument().getBody();
    CTSectPr section = body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr();
    CTPageSz pgSz = section.isSetPgSz() ? section.getPgSz() : section.addNewPgSz();

    long width = A4_WIDTH_TWIPS;
    long height = A4_HEIGHT_TWIPS;
    if ("LETTER".equalsIgnoreCase(pageSize)) {
      width = LETTER_WIDTH_TWIPS;
      height = LETTER_HEIGHT_TWIPS;
    }

    long activeWidth = width;
    if ("LANDSCAPE".equalsIgnoreCase(orientation)) {
      pgSz.setOrient(STPageOrientation.LANDSCAPE);
      pgSz.setW(BigInteger.valueOf(height));
      pgSz.setH(BigInteger.valueOf(width));
      activeWidth = height;
    } else {
      pgSz.setOrient(STPageOrientation.PORTRAIT);
      pgSz.setW(BigInteger.valueOf(width));
      pgSz.setH(BigInteger.valueOf(height));
    }

    CTPageMar pageMar = section.isSetPgMar() ? section.getPgMar() : section.addNewPgMar();
    pageMar.setTop(MARGIN_STANDARD);
    pageMar.setBottom(MARGIN_STANDARD);
    pageMar.setLeft(MARGIN_STANDARD);
    pageMar.setRight(MARGIN_STANDARD);
    pageMar.setHeader(MARGIN_HEADER_Y);
    pageMar.setFooter(MARGIN_FOOTER_Y);

    return (int) (activeWidth - 2880);
  }

  // --- HEADER / FOOTER ---

  private void applyHeaderFooter(XWPFDocument doc, ReportSettings settings, String headerColor, String footerText) {
    try {
      CTSectPr sectPr = doc.getDocument().getBody().isSetSectPr() ? doc.getDocument().getBody().getSectPr()
        : doc.getDocument().getBody().addNewSectPr();
      XWPFHeaderFooterPolicy policy = new XWPFHeaderFooterPolicy(doc, sectPr);

      createHeaderIfNeeded(policy, settings, headerColor, doc);
      createFooterIfNeeded(policy, settings, footerText);

    } catch (Exception e) {
      logger.error("Failed to apply header/footer to document: {}", e.getMessage(), e);
    }
  }

  private void createHeaderIfNeeded(XWPFHeaderFooterPolicy policy, ReportSettings settings, String headerColor, XWPFDocument doc) {
    if (settings.getHeaderText() == null || settings.getHeaderText().isEmpty()) {
      return;
    }

    if (policy.getDefaultHeader() == null) {
      policy.createHeader(XWPFHeaderFooterPolicy.DEFAULT);
    }

    for (XWPFHeader h : doc.getHeaderList()) {
      XWPFParagraph p = createHeaderParagraph(h);
      addHeaderContent(p, settings, headerColor);
    }
  }

  private XWPFParagraph createHeaderParagraph(XWPFHeader header) {
    XWPFParagraph p;
    if (header.getParagraphs().isEmpty()) {
      p = header.createParagraph();
    } else {
      try (XmlCursor cursor = header.getParagraphs().get(0).getCTP().newCursor()) {
        p = header.insertNewParagraph(cursor);
      }
    }

    p.setAlignment(ParagraphAlignment.LEFT);
    p.setSpacingBefore(0);
    p.setSpacingAfter(0);
    return p;
  }

  private void addHeaderContent(XWPFParagraph p, ReportSettings settings, String headerColor) {
    XWPFRun r1 = p.createRun();
    r1.setText(settings.getHeaderText());
    r1.setBold(true);
    r1.setFontSize(FONT_SIZE_HEADER);
    r1.setColor(headerColor != null ? headerColor : COLOR_WHITE);

    if (settings.getSubheaderText() != null && !settings.getSubheaderText().isEmpty()) {
      r1.addBreak();
      XWPFRun r2 = p.createRun();
      r2.setText(settings.getSubheaderText());
      r2.setBold(false);
      r2.setFontSize(FONT_SIZE_SUBHEADER);
      r2.setColor(COLOR_WHITE);
    }
  }

  private void createFooterIfNeeded(XWPFHeaderFooterPolicy policy, ReportSettings settings, String footerText) {
    String fullFooterText = buildFooterText(settings, footerText);

    if (fullFooterText.isEmpty()) {
      return;
    }

    XWPFFooter footer = policy.getDefaultFooter();
    if (footer == null) {
      footer = policy.createFooter(XWPFHeaderFooterPolicy.DEFAULT);
    }

    XWPFParagraph p = footer.getParagraphs().isEmpty() ? footer.createParagraph() : footer.getParagraphs().get(0);

    // Clear existing runs
    for (int i = p.getRuns().size() - 1; i >= 0; i--) {
      p.removeRun(i);
    }

    p.setAlignment(ParagraphAlignment.CENTER);
    p.setSpacingBefore(0);
    p.setSpacingAfter(0);

    XWPFRun r = p.createRun();
    r.setText(fullFooterText);
    r.setFontSize(FONT_SIZE_FOOTER);
    r.setColor(COLOR_GREY_TEXT);
  }

  private String buildFooterText(ReportSettings settings, String footerText) {
    StringBuilder fullFooterText = new StringBuilder();

    if (footerText != null && !footerText.isEmpty()) {
      fullFooterText.append(footerText);
    }

    if (settings.getAuditReference() != null && !settings.getAuditReference().isEmpty()) {
      if (fullFooterText.length() > 0) {
        fullFooterText.append(" | ");
      }
      fullFooterText.append("ID: ").append(settings.getAuditReference());
    }

    if (settings.getReportDate() != null && !settings.getReportDate().isEmpty()) {
      if (fullFooterText.length() > 0) {
        fullFooterText.append(" | ");
      }
      fullFooterText.append("Date: ").append(settings.getReportDate());
    }

    return fullFooterText.toString();
  }
}
