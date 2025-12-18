package com.appiancs.plugins.chartgenie.strategies;

import org.jfree.chart.JFreeChart;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;

public interface ChartGeneratorStrategy {
  JFreeChart generate(ChartConfiguration config);
}
