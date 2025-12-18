package com.appiancs.plugins.chartgenie.strategies;

import java.awt.Color;
import java.awt.Font;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.Plot;
import org.jfree.data.general.Dataset;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;

// T represents the specific JFreeChart Dataset type (e.g., DefaultCategoryDataset)
public abstract class AbstractChartStrategy<T extends Dataset> implements ChartGeneratorStrategy {

  @Override
  public JFreeChart generate(ChartConfiguration config) {
    // Step 1: Create the specific dataset (Child responsibility)
    T dataset = createDataset(config);

    // Step 2: Create the raw chart (Child responsibility)
    JFreeChart chart = createRawChart(dataset, config);

    // Step 3: Apply common styling (Base responsibility)
    applyBaseStyling(chart);

    // Step 4: Apply specific styling (Child responsibility)
    applySpecificStyling(chart, config);

    return chart;
  }

  protected abstract T createDataset(ChartConfiguration config);

  protected abstract JFreeChart createRawChart(T dataset, ChartConfiguration config);

  protected abstract void applySpecificStyling(JFreeChart chart, ChartConfiguration config);

  private void applyBaseStyling(JFreeChart chart) {
    // 1. Clean Main Background
    chart.setBackgroundPaint(Color.WHITE);

    // 2. Clean Plot Background
    Plot plot = chart.getPlot();
    plot.setBackgroundPaint(Color.WHITE);
    plot.setOutlineVisible(false); // Remove border around the plot area

    // 3. Standard Title Font
    if (chart.getTitle() != null) {
      chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 18));
    }
  }
}