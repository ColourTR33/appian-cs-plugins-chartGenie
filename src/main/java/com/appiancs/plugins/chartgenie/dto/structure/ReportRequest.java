package com.appiancs.plugins.chartgenie.dto.structure;

import java.util.List;

public class ReportRequest {
  private ReportSettings settings;
  private List<ReportSection> sections;

  public ReportSettings getSettings() {
    return settings;
  }

  public void setSettings(ReportSettings settings) {
    this.settings = settings;
  }

  public List<ReportSection> getSections() {
    return sections;
  }

  public void setSections(List<ReportSection> sections) {
    this.sections = sections;
  }
}