package com.appiancs.plugins.chartgenie.dto.structure;

import java.util.ArrayList;
import java.util.List;

public class ReportRequest {
  private DocumentSettings documentSettings;
  private List<ReportSection> content = new ArrayList<>();

  public void addContent(ReportSection section) {
    this.content.add(section);
  }

  public DocumentSettings getDocumentSettings() {
    return documentSettings;
  }

  public void setDocumentSettings(DocumentSettings documentSettings) {
    this.documentSettings = documentSettings;
  }

  public List<ReportSection> getContent() {
    return content;
  }

  public void setContent(List<ReportSection> content) {
    this.content = content;
  }
}