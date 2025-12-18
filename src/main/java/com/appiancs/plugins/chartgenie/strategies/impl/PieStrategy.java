package com.appiancs.plugins.chartgenie.strategies.impl;

import java.awt.Color;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.data.general.DefaultPieDataset;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.enums.ChartType;
import com.appiancs.plugins.chartgenie.strategies.AbstractChartStrategy;

public class PieStrategy extends AbstractChartStrategy<DefaultPieDataset> {

  private final ChartType specificType;

  public PieStrategy(ChartType specificType) {
    this.specificType = specificType;
  }

  @Override
  protected DefaultPieDataset createDataset(ChartConfiguration config) {
    DefaultPieDataset dataset = new DefaultPieDataset();
    config.getDataPoints().forEach(dataset::setValue);
    return dataset;
  }

  @Override
  protected JFreeChart createRawChart(DefaultPieDataset dataset, ChartConfiguration config) {
    if (specificType == ChartType.PIE_3D) {
      return ChartFactory.createPieChart3D(config.getTitle(), dataset, true, true, false);
    } else {
      return ChartFactory.createPieChart(config.getTitle(), dataset, true, true, false);
    }
  }

  @Override
  protected void applySpecificStyling(JFreeChart chart, ChartConfiguration config) {
    PiePlot plot = (PiePlot) chart.getPlot();

    // Remove the ugly border around the actual pie circle
    plot.setOutlineVisible(false);
    plot.setShadowPaint(null);

    // 3D Specifics
    if (plot instanceof PiePlot3D) {
      PiePlot3D p3d = (PiePlot3D) plot;
      p3d.setForegroundAlpha(0.6f);
    }

    // Apply Colors from DTO List
    List<String> keys = ((DefaultPieDataset) plot.getDataset()).getKeys();
    List<String> hexColors = config.getHexColors();

    for (int i = 0; i < keys.size(); i++) {
      if (hexColors != null && i < hexColors.size()) {
        plot.setSectionPaint(keys.get(i), Color.decode(hexColors.get(i)));
      }
    }
  }
}