package com.appiancs.plugins.chartgenie.service;

import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;
import com.appiancs.plugins.chartgenie.strategies.impl.*;
// Add imports for Line, Pie, Bar strategies if you have them

public class ChartStrategyFactory {

    public static ChartGeneratorStrategy getStrategy(String chartType) {
        if (chartType == null) {
            return new ColumnChartStrategy(); // Default
        }

        switch (chartType.toUpperCase()) {
            case "COLUMN":
                return new ColumnChartStrategy();
            case "LINE":
                return new LineChartStrategy();
            case "COLUMN_STACKED":
                return new StackedColumnStrategy();
            case "BAR":
                // return new BarChartStrategy(); // If you have a specific class
                return new ColumnChartStrategy(); // Placeholder if sharing logic
            case "PIE":
                // return new PieStrategy();
                return new DonutChartStrategy(); // Temporary fallback if Pie missing

            // --- NEW TYPES START HERE ---
            case "DONUT":
                return new DonutChartStrategy();

            case "AREA":
                return new AreaChartStrategy();
            // -----------------------------

            default:
                System.out.println("Warning: Unknown chart type '" + chartType + "'. Defaulting to Column.");
                return new ColumnChartStrategy();
        }
    }
}