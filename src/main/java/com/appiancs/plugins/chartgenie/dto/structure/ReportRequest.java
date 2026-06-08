package com.appiancs.plugins.chartgenie.dto.structure;

import java.util.List;
import java.util.Map;

public class ReportRequest {
  private ReportSettings settings;
  private List<ReportSection> sections;
  private Map<String, String> variables;

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

  public Map<String, String> getVariables() {
    return variables;
  }

  public void setVariables(Map<String, String> variables) {
    this.variables = variables;
  }
}