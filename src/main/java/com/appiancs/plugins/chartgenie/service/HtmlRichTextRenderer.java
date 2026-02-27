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

/**
 * Parses basic HTML tags (<b>, <i>, <br/>
 * , <span style="color:#HEX">)
 * into Word document styling for rich text table cells.
 */
public class HtmlRichTextRenderer {

  public void render(XWPFDocument doc, XWPFTableCell cell, String htmlContent) {
    if (htmlContent == null || htmlContent.isEmpty()) {
      return;
    }

    XWPFParagraph currentParagraph = cell.getParagraphs().isEmpty() ? cell.addParagraph() : cell.getParagraphs().get(0);

    // If there's no HTML, just set plain text to save processing time
    if (!htmlContent.contains("<") || !htmlContent.contains(">")) {
      currentParagraph.createRun().setText(htmlContent);
      return;
    }

    // Parse HTML fragment
    Document jsoupDoc = Jsoup.parseBodyFragment(htmlContent);

    // Iterate through nodes and apply styles
    for (Node node : jsoupDoc.body().childNodes()) {
      processNode(node, currentParagraph);
    }
  }

  private void processNode(Node node, XWPFParagraph paragraph) {
    if (node instanceof TextNode) {
      String text = ((TextNode) node).getWholeText();
      if (!text.trim().isEmpty() || text.contains(" ")) {
        paragraph.createRun().setText(text);
      }
    } else if (node instanceof Element) {
      Element element = (Element) node;
      String tagName = element.tagName().toLowerCase();

      if (tagName.equals("br")) {
        paragraph.createRun().addBreak();
      } else {
        // Apply inline styles to children
        for (Node child : element.childNodes()) {
          if (child instanceof TextNode) {
            XWPFRun run = paragraph.createRun();
            applyStyles(run, element);
            run.setText(((TextNode) child).getWholeText());
          } else if (child instanceof Element && ((Element) child).tagName().equalsIgnoreCase("br")) {
            paragraph.createRun().addBreak();
          }
        }
      }
    }
  }

  private void applyStyles(XWPFRun run, Element element) {
    String tagName = element.tagName().toLowerCase();

    if (tagName.equals("b") || tagName.equals("strong")) {
      run.setBold(true);
    } else if (tagName.equals("i") || tagName.equals("em")) {
      run.setItalic(true);
    } else if (tagName.equals("u")) {
      run.setUnderline(UnderlinePatterns.SINGLE);
    } else if (tagName.equals("span")) {
      String style = element.attr("style");
      // Extract Hex color from format: style='color:#FF0000'
      if (style.contains("color")) {
        String hexColor = style.replaceAll(".*color:\\s*#([A-Fa-f0-9]{6}).*", "$1");
        if (hexColor.length() == 6) {
          run.setColor(hexColor);
        }
      }
    }
  }
}