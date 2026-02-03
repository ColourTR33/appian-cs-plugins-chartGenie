package com.appiancs.plugins.chartgenie.service;

import org.apache.poi.xwpf.usermodel.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

public class HtmlRichTextRenderer {

  public void render(XWPFDocument doc, XWPFTableCell cell, String html) {
    if (html == null || html.isEmpty())
      return;

    Document soupDoc = Jsoup.parseBodyFragment(html);
    Element body = soupDoc.body();

    for (Node node : body.childNodes()) {
      if (node instanceof Element) {
        Element el = (Element) node;
        switch (el.tagName().toLowerCase()) {
          case "ul":
          case "ol":
            renderList(doc, cell, el);
            break;
          case "p":
          case "div":
            XWPFParagraph p = createParagraph(doc, cell);
            processInlineNodes(p, el, false, false, null);
            break;
          case "br":
            createParagraph(doc, cell);
            break;
          default:
            // Inline element at root (e.g. "<b>Title:</b>")
            XWPFParagraph defaultP = createParagraph(doc, cell);
            processInlineNodes(defaultP, el, false, false, null);
        }
      } else if (node instanceof TextNode) {
        XWPFParagraph p = createParagraph(doc, cell);
        processInlineNodes(p, node, false, false, null);
      }
    }
  }

  private void renderList(XWPFDocument doc, XWPFTableCell cell, Element listElement) {
    for (Element li : listElement.children()) {
      if (!li.tagName().equals("li"))
        continue;

      XWPFParagraph p = createParagraph(doc, cell);
      p.setIndentationLeft(720); // Indent

      XWPFRun bullet = p.createRun();
      bullet.setText("•  ");

      processInlineNodes(p, li, false, false, null);
    }
  }

  private void processInlineNodes(XWPFParagraph p, Node node, boolean isBold, boolean isItalic, String color) {
    if (node instanceof TextNode) {
      String text = ((TextNode) node).text();
      if (!text.isEmpty()) {
        XWPFRun run = p.createRun();
        run.setText(text);
        if (isBold)
          run.setBold(true);
        if (isItalic)
          run.setItalic(true);
        if (color != null)
          run.setColor(color.replace("#", ""));
        run.setFontSize(10);
        run.setFontFamily("Arial");
      }
    } else if (node instanceof Element) {
      Element el = (Element) node;
      String tagName = el.tagName().toLowerCase();

      boolean newBold = isBold || tagName.equals("b") || tagName.equals("strong");
      boolean newItalic = isItalic || tagName.equals("i") || tagName.equals("em");
      String newColor = color;

      if (el.hasAttr("style") && el.attr("style").toLowerCase().contains("color")) {
        // Simplified style parsing for brevity
        String style = el.attr("style").toLowerCase();
        int colorIndex = style.indexOf("color:");
        if (colorIndex >= 0) {
          int end = style.indexOf(";", colorIndex);
          if (end == -1)
            end = style.length();
          newColor = style.substring(colorIndex + 6, end).trim();
        }
      }

      if (tagName.equals("br"))
        p.createRun().addBreak();

      for (Node child : el.childNodes()) {
        processInlineNodes(p, child, newBold, newItalic, newColor);
      }
    }
  }

  private XWPFParagraph createParagraph(XWPFDocument doc, XWPFTableCell cell) {
    return (cell != null) ? cell.addParagraph() : doc.createParagraph();
  }
}