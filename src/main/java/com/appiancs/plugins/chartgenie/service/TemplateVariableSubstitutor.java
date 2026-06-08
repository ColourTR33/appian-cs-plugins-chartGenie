package com.appiancs.plugins.chartgenie.service;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Replaces {@code {{key}}} tokens in a DOCX document with values from a variables map.
 * <h3>The Split-Run Problem</h3>
 * Word processors frequently split a single visible token like {@code {{clientName}}} across
 * multiple internal XML {@code <w:r>} (run) elements — e.g. {@code {{client}, {Name}}} — due
 * to spell-check, autocorrect, or formatting boundaries. A naive approach that checks each run
 * in isolation will never find the token.
 * <p>
 * This implementation solves the problem in two passes per paragraph:
 * <ol>
 * <li><b>Consolidate</b> — merge all run texts in the paragraph into a single string, then
 * check for any {@code {{...}}} tokens.</li>
 * <li><b>Rewrite</b> — if tokens are found, replace them in the consolidated string and write
 * the entire result back into the first run of the paragraph, clearing the rest. This
 * preserves the visual output while eliminating split-run issues.</li>
 * </ol>
 * <p>
 * Substitution is applied to: body paragraphs, table cells, headers, and footers.
 */
public class TemplateVariableSubstitutor {

  private static final Logger LOG = LoggerFactory.getLogger(TemplateVariableSubstitutor.class);
  private static final Pattern TOKEN_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
  private static final int MAX_VALUE_LENGTH = 2000;

  /**
   * Applies all variable substitutions to the document in-place.
   *
   * @param doc
   *          the DOCX document to modify
   * @param variables
   *          map of token keys (without braces) to replacement values
   */
  public void substitute(XWPFDocument doc, Map<String, String> variables) {
    if (variables == null || variables.isEmpty()) {
      return;
    }

    Map<String, String> safeVars = sanitizeVariables(variables);
    int replacements = 0;

    // Body paragraphs
    for (XWPFParagraph para : doc.getParagraphs()) {
      replacements += substituteInParagraph(para, safeVars);
    }

    // Table cells
    for (XWPFTable table : doc.getTables()) {
      for (XWPFTableRow row : table.getRows()) {
        for (XWPFTableCell cell : row.getTableCells()) {
          for (XWPFParagraph para : cell.getParagraphs()) {
            replacements += substituteInParagraph(para, safeVars);
          }
        }
      }
    }

    // Headers
    for (XWPFHeader header : doc.getHeaderList()) {
      for (XWPFParagraph para : header.getParagraphs()) {
        replacements += substituteInParagraph(para, safeVars);
      }
    }

    // Footers
    for (XWPFFooter footer : doc.getFooterList()) {
      for (XWPFParagraph para : footer.getParagraphs()) {
        replacements += substituteInParagraph(para, safeVars);
      }
    }

    LOG.debug("Template substitution complete: {} token(s) replaced.", replacements);
  }

  /**
   * Substitutes tokens in a single paragraph using the consolidate-then-rewrite strategy.
   * Returns the number of substitutions made.
   */
  private int substituteInParagraph(XWPFParagraph para, Map<String, String> variables) {
    List<XWPFRun> runs = para.getRuns();
    if (runs.isEmpty()) {
      return 0;
    }

    // Step 1: Consolidate all run texts into one string
    StringBuilder combined = new StringBuilder();
    for (XWPFRun run : runs) {
      String text = run.getText(0);
      combined.append(text != null ? text : "");
    }

    String original = combined.toString();
    if (!TOKEN_PATTERN.matcher(original).find()) {
      return 0; // No tokens — skip the rewrite entirely
    }

    // Step 2: Replace all tokens in the consolidated string
    String replaced = replaceTokens(original, variables);
    if (replaced.equals(original)) {
      return 0;
    }

    // Step 3: Write the result back into the first run, blank out the rest.
    // Preserve the first run's formatting (font, size, bold etc).
    runs.get(0).setText(replaced, 0);
    for (int i = 1; i < runs.size(); i++) {
      runs.get(i).setText("", 0);
    }

    return countTokensReplaced(original, variables);
  }

  private String replaceTokens(String text, Map<String, String> variables) {
    Matcher matcher = TOKEN_PATTERN.matcher(text);
    StringBuffer result = new StringBuffer();

    while (matcher.find()) {
      String key = matcher.group(1).trim();
      String value = variables.getOrDefault(key, matcher.group(0)); // leave unreplaced if missing
      matcher.appendReplacement(result, Matcher.quoteReplacement(value));
    }
    matcher.appendTail(result);
    return result.toString();
  }

  private int countTokensReplaced(String text, Map<String, String> variables) {
    Matcher matcher = TOKEN_PATTERN.matcher(text);
    int count = 0;
    while (matcher.find()) {
      String key = matcher.group(1).trim();
      if (variables.containsKey(key)) {
        count++;
      }
    }
    return count;
  }

  /**
   * Sanitizes variable values to prevent content injection into the document.
   * Limits length and strips control characters.
   */
  private Map<String, String> sanitizeVariables(Map<String, String> variables) {
    Map<String, String> safe = new java.util.LinkedHashMap<>();
    for (Map.Entry<String, String> entry : variables.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      if (key == null || key.trim().isEmpty()) {
        continue;
      }
      if (value == null) {
        value = "";
      }
      // Truncate and strip control characters
      if (value.length() > MAX_VALUE_LENGTH) {
        value = value.substring(0, MAX_VALUE_LENGTH) + "...";
      }
      value = value.replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", "");
      safe.put(key.trim(), value);
    }
    return safe;
  }
}
