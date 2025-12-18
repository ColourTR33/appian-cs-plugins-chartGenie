package com.appiancs.plugins.chartgenie.strategies.impl;

import java.awt.Color;
import java.awt.Font;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.RingPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.ui.RectangleInsets;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.strategies.AbstractChartStrategy;

public class DonutChartStrategy extends AbstractChartStrategy<DefaultPieDataset> {

  @Override
  protected DefaultPieDataset createDataset(ChartConfiguration config) {
    DefaultPieDataset dataset = new DefaultPieDataset();
    config.getDataPoints().forEach(dataset::setValue);
    return dataset;
  }

  @Override
  protected JFreeChart createRawChart(DefaultPieDataset dataset, ChartConfiguration config) {
    // Create Ring Chart (Donut)
    JFreeChart chart = ChartFactory.createRingChart(
      config.getTitle(), dataset, false, true, false);
    return chart;
  }

  @Override
  protected void applySpecificStyling(JFreeChart chart, ChartConfiguration config) {
    RingPlot plot = (RingPlot) chart.getPlot();

    // 1. Make it a Donut (Depth controls thickness)
    plot.setSectionDepth(0.35);
    plot.setShadowPaint(null);
    plot.setOutlineVisible(false);

    // 2. Colors
    List<String> keys = ((DefaultPieDataset) plot.getDataset()).getKeys();
    List<String> hexColors = config.getHexColors();

    int i = 0;
    for (Object key : keys) {
      Color paint = (hexColors != null && i < hexColors.size())
        ? Color.decode(hexColors.get(i))
        : Color.GRAY; // Fallback
      plot.setSectionPaint((Comparable) key, paint);
      i++;
    }

    // 3. Center Text (The specific requirement)
    if (config.getCenterText() != null && !config.getCenterText().isEmpty()) {
      TextTitle centerText = new TextTitle(config.getCenterText());
      centerText.setFont(new Font("SansSerif", Font.BOLD, 26));
      centerText.setPaint(Color.DARK_GRAY);

      // Pushes the title into the middle of the empty donut space
      centerText.setPosition(org.jfree.ui.RectangleEdge.BOTTOM);
      centerText.setPadding(new RectangleInsets(-100, 0, 0, 0)); // Hacky adjustment to overlay center
      chart.addSubtitle(centerText);
    }
  }
}