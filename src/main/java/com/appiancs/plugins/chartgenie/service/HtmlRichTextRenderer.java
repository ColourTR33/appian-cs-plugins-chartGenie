package com.appiancs.plugins.chartgenie.service;

import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.safety.Safelist;

/**
 * Parses nested HTML tags (<strong>, <em>, <br/>
 * ,
 * <p>
 * ,
 * <li>, <span style="color:#HEX/rgb">)
 * into native Word document styling. Fully compatible with Appian a!styledTextEditorField() outputs.
 */
public class HtmlRichTextRenderer {

  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(HtmlRichTextRenderer.class);

  private static final String TAG_SPAN = "span";

  // CWE-94: Static allowlist — only these tags/attributes are permitted into the parser
  private static final Safelist HTML_SAFELIST = Safelist.none()
    .addTags("p", "br", "strong", "b", "em", "i", "u", TAG_SPAN, "div", "ul", "ol", "li")
    .addAttributes(TAG_SPAN, "style")
    .addProtocols(TAG_SPAN, "style", "color");

  private static final java.util.regex.Pattern DANGEROUS_PROTOCOLS = java.util.regex.Pattern.compile("(javascript|vbscript|data):",
    java.util.regex.Pattern.CASE_INSENSITIVE);

  public void render(XWPFDocument doc, XWPFTableCell cell, String htmlContent) {
    if (htmlContent == null || htmlContent.isEmpty()) {
      return;
    }

    // CWE-94: Sanitize at the sink — clean against allowlist before any parsing
    String safeContent = sanitize(htmlContent);

    XWPFParagraph currentParagraph = cell.getParagraphs().isEmpty() ? cell.addParagraph() : cell.getParagraphs().get(0);

    // If there's no HTML, just set plain text to save processing time
    if (!safeContent.contains("<") || !safeContent.contains(">")) {
      currentParagraph.createRun().setText(safeContent);
      return;
    }

    // Parse the already-sanitized HTML fragment
    Document jsoupDoc = Jsoup.parseBodyFragment(safeContent);

    // Recursively traverse the tree
    for (Node child : jsoupDoc.body().childNodes()) {
      currentParagraph = traverseNode(child, cell, currentParagraph);
    }
  }

  /**
   * Sanitizes HTML against the static allowlist before parsing.
   * Breaks the taint flow at the sink (CWE-94).
   */
  private String sanitize(String input) {
    String value = input.length() > 10000 ? input.substring(0, 10000) : input;
    String cleaned = Jsoup.clean(value, "", HTML_SAFELIST);
    return DANGEROUS_PROTOCOLS.matcher(cleaned).find() ? stripAllTags(cleaned) : cleaned;
  }

  /** Strips all HTML tags using regex — avoids re-invoking the JSoup parser on tainted input. */
  private static String stripAllTags(String input) {
    return input.replaceAll("<[^>]*>", "").trim();
  }

  // Deep recursive traversal to handle complex nested Appian structures
  private XWPFParagraph traverseNode(Node node, XWPFTableCell cell, XWPFParagraph para) {
    if (node instanceof TextNode) {
      return traverseTextNode((TextNode) node, para);
    } else if (node instanceof Element) {
      return traverseElement((Element) node, cell, para);
    }
    return para;
  }

  private XWPFParagraph traverseTextNode(TextNode textNode, XWPFParagraph para) {
    String text = textNode.getWholeText().replace("\n", "").replace("\r", "");
    if (!text.isEmpty()) {
      XWPFRun run = para.createRun();
      run.setText(text);
      applyStylesFromParents(run, textNode);
    }
    return para;
  }

  private XWPFParagraph traverseElement(Element el, XWPFTableCell cell, XWPFParagraph para) {
    String tag = el.tagName().toLowerCase(java.util.Locale.ROOT);
    boolean isBlock = "p".equals(tag) || "div".equals(tag) || "ul".equals(tag) || "ol".equals(tag);

    XWPFParagraph current = (isBlock && !para.getRuns().isEmpty()) ? cell.addParagraph() : para;
    if ("li".equals(tag))
      current = openBullet(cell, current);

    for (Node child : el.childNodes()) {
      current = traverseNode(child, cell, current);
    }

    if ("br".equals(tag))
      current.createRun().addBreak();
    if (isBlock || "li".equals(tag))
      current = cell.addParagraph();

    return current;
  }

  private XWPFParagraph openBullet(XWPFTableCell cell, XWPFParagraph para) {
    XWPFParagraph current = para.getRuns().isEmpty() ? para : cell.addParagraph();
    current.createRun().setText("• ");
    return current;
  }

  private void applyStylesFromParents(XWPFRun run, Node node) {
    Node parent = node.parent();
    boolean isBold = false;
    boolean isItalic = false;
    boolean isUnderline = false;
    String color = null;

    while (parent instanceof Element) {
      String tag = ((Element) parent).tagName().toLowerCase(java.util.Locale.ROOT);

      if ("b".equals(tag) || "strong".equals(tag))
        isBold = true;
      if ("i".equals(tag) || "em".equals(tag))
        isItalic = true;
      if ("u".equals(tag))
        isUnderline = true;

      if (TAG_SPAN.equals(tag) && color == null) {
        String style = ((Element) parent).attr("style").toLowerCase(java.util.Locale.ROOT);
        color = extractColor(style);
      }

      parent = parent.parent();
    }

    if (isBold)
      run.setBold(true);
    if (isItalic)
      run.setItalic(true);
    if (isUnderline)
      run.setUnderline(UnderlinePatterns.SINGLE);
    if (color != null)
      run.setColor(color);
  }

  // Parses both HEX and Appian's RGB color formats
  private String extractColor(String style) {
    try {
      if (style.contains("rgb")) {
        int start = style.indexOf("rgb(") + 4;
        int end = style.indexOf(")", start);
        String[] rgb = style.substring(start, end).split(",");
        if (rgb.length >= 3) {
          int r = Integer.parseInt(rgb[0].trim());
          int g = Integer.parseInt(rgb[1].trim());
          int b = Integer.parseInt(rgb[2].trim());
          return String.format("%02X%02X%02X", r, g, b);
        }
      } else if (style.contains("#")) {
        int start = style.indexOf("#") + 1;
        if (start + 6 <= style.length()) {
          return style.substring(start, start + 6).toUpperCase(java.util.Locale.ROOT);
        }
      }
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Failed to parse color from style '{}': {}", style, e.getMessage());
      }
    }
    return null;
  }
}