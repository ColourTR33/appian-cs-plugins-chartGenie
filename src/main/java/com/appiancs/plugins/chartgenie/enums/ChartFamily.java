package com.appiancs.plugins.chartgenie.enums;

public enum ChartFamily {
    PIE, // Requires PieDataset (Key -> Value)
    CATEGORY, // Requires CategoryDataset (RowKey, ColKey -> Value)
    XY, // Requires XYDataset (X, Y)
    INTERVAL, // Requires IntervalCategoryDataset (Start, End)
    OHLC // Requires OHLCDataset (Open, High, Low, Close)
}
