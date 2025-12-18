package com.appiancs.plugins.chartgenie.service;

import com.appiancs.plugins.chartgenie.enums.ChartType;
import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;
import com.appiancs.plugins.chartgenie.strategies.impl.DonutChartStrategy;
import com.appiancs.plugins.chartgenie.strategies.impl.PieStrategy;
import com.appiancs.plugins.chartgenie.strategies.impl.StandardCategoryStrategy;
import com.appiancs.plugins.chartgenie.strategies.impl.StandardXYStrategy;

public class ChartStrategyFactory {
  public static ChartGeneratorStrategy getStrategy(ChartType type) {
    if (type == null)
      type = ChartType.COLUMN;

    switch (type) {
      case DONUT:
        return new DonutChartStrategy();
      case PIE:
      case PIE_3D:
        return new PieStrategy(type);
      case BAR:
      case COLUMN:
      case LINE:
      case AREA:
        return new StandardCategoryStrategy(type);
      case SCATTER:
      case XY_LINE:
        return new StandardXYStrategy(type);
      default:
        throw new IllegalArgumentException("Strategy not implemented for: " + type);
    }
  }
}
