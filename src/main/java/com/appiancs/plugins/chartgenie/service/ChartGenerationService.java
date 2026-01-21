package com.appiancs.plugins.chartgenie.service;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;

import java.io.File;

public class ChartGenerationService {

    public File generateChartImage(ChartConfiguration config) throws Exception {

        // 1. Get Strategy
        ChartGeneratorStrategy strategy = ChartStrategyFactory.getStrategy(config.getChartType());

        // 2. Generate Chart
        JFreeChart chart = strategy.generate(config);

        // 3. Save using MODERN ChartUtils
        File outputFile = File.createTempFile("chart_", ".png");

        // This method signature is cleaner in 1.5.x
        ChartUtils.saveChartAsPNG(outputFile, chart, config.getWidth(), config.getHeight());

        return outputFile;
    }
}