package com.appiancs.plugins.chartgenie.strategies.impl;

import java.awt.Color;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.enums.ChartType;
import com.appiancs.plugins.chartgenie.strategies.AbstractChartStrategy;

public class StandardCategoryStrategy extends AbstractChartStrategy<DefaultCategoryDataset> {

  private final ChartType specificType;

  public StandardCategoryStrategy(ChartType specificType) {
    this.specificType = specificType;
  }

  @Override
  protected DefaultCategoryDataset createDataset(ChartConfiguration config) {
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    // Assuming single series. If you need multi-series, your DTO needs a List<Series>
    String seriesName = "Series 1";
    config.getDataPoints().forEach((k, v) -> dataset.addValue(v, seriesName, k));
    return dataset;
  }

  @Override
  protected JFreeChart createRawChart(DefaultCategoryDataset dataset, ChartConfiguration config) {
    String title = config.getTitle();
    String xLabel = "Category";
    String yLabel = "Value";
    boolean legend = false;

    switch (specificType) {
      case BAR:
        return ChartFactory.createBarChart(title, xLabel, yLabel, dataset,
          PlotOrientation.HORIZONTAL, legend, true, false);
      case LINE:
        return ChartFactory.createLineChart(title, xLabel, yLabel, dataset,
          PlotOrientation.VERTICAL, legend, true, false);
      case AREA:
        return ChartFactory.createAreaChart(title, xLabel, yLabel, dataset,
          PlotOrientation.VERTICAL, legend, true, false);
      case COLUMN:
      default:
        return ChartFactory.createBarChart(title, xLabel, yLabel, dataset,
          PlotOrientation.VERTICAL, legend, true, false);
    }
  }

  @Override
  protected void applySpecificStyling(JFreeChart chart, ChartConfiguration config) {
    CategoryPlot plot = chart.getCategoryPlot();

    // Gridlines (Specific to Category plots)
    plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
    plot.setRangeGridlinesVisible(true);

    // Series Color
    plot.getRenderer().setSeriesPaint(0, config.getPrimaryColor());
  }

}
