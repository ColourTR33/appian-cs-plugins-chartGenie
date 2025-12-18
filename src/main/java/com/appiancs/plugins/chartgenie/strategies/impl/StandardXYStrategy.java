package com.appiancs.plugins.chartgenie.strategies.impl;

import java.awt.Color;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.enums.ChartType;
import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;

public class StandardXYStrategy implements ChartGeneratorStrategy {

  private final ChartType specificType;

  public StandardXYStrategy(ChartType specificType) {
    this.specificType = specificType;
  }

  @Override
  public JFreeChart generate(ChartConfiguration config) {
    // 1. Prepare Data (Parsing String Keys to Doubles)
    XYSeries series = new XYSeries("Series 1");

    config.getDataPoints().forEach((k, v) -> {
      try {
        // We attempt to parse the Key string as a double for the X-Axis
        double xValue = Double.parseDouble(k);
        series.add(xValue, v);
      } catch (NumberFormatException e) {
        // Log warning: Skipping non-numeric X-axis value
        System.out.println("Skipping non-numeric X value: " + k);
      }
    });

    XYSeriesCollection dataset = new XYSeriesCollection(series);

    JFreeChart chart;
    String title = config.getTitle();

    // 2. Switch Factory Logic
    if (specificType == ChartType.SCATTER) {
      chart = ChartFactory.createScatterPlot(
        title, "X", "Y", dataset,
        PlotOrientation.VERTICAL, false, true, false);
    } else {
      // XY_LINE
      chart = ChartFactory.createXYLineChart(
        title, "X", "Y", dataset,
        PlotOrientation.VERTICAL, false, true, false);
    }

    // 3. Styling
    XYPlot plot = chart.getXYPlot();
    plot.setBackgroundPaint(Color.WHITE);
    plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
    plot.setDomainGridlinePaint(Color.LIGHT_GRAY);

    plot.getRenderer().setSeriesPaint(0, config.getPrimaryColor());

    return chart;
  }
}