package com.appiancs.plugins.chartgenie.strategies.impl;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.AreaRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import java.awt.Color;
import java.util.List;

public class AreaChartStrategy implements ChartGeneratorStrategy {

    @Override
    public JFreeChart generate(ChartConfiguration config) {
        // 1. Populate Dataset
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        List<String> categories = config.getCategories();
        List<Number> values = config.getValues();

        if (categories != null && values != null) {
            for (int i = 0; i < categories.size(); i++) {
                if (i < values.size()) {
                    dataset.addValue(values.get(i), "Current Trend", categories.get(i));
                }
            }
        }

        // 2. Create Chart
        JFreeChart chart = ChartFactory.createAreaChart(
                config.getTitle(),
                "Period",             // X-Axis Label
                "Volume",             // Y-Axis Label
                dataset,
                PlotOrientation.VERTICAL,
                false, true, false
        );

        // 3. Style It (The "Appian" Look)
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        plot.setOutlineVisible(false);

        // Customize the Area Renderer (Transparency looks great on Area charts)
        AreaRenderer renderer = (AreaRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(30, 60, 150, 180)); // Blue with 180 alpha (transparency)

        return chart;
    }
}