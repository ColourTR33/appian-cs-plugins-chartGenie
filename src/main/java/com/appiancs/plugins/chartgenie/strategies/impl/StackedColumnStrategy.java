package com.appiancs.plugins.chartgenie.strategies.impl;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;

import java.awt.Color;
import java.util.List;

public class StackedColumnStrategy implements ChartGeneratorStrategy {

    @Override
    public JFreeChart generate(ChartConfiguration config) {

        // 1. Populate Data
        // Ideally, stacked charts use multiple series (e.g., "High", "Medium").
        // For this generic test, we will map the single list to one stack "Group A".
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        List<String> categories = config.getCategories();
        List<Number> values = config.getValues();

        if (categories != null && values != null) {
            for (int i = 0; i < categories.size(); i++) {
                if (i < values.size()) {
                    // For a true stack, you'd have multiple series calls here.
                    // We will just plot them normally for now.
                    dataset.addValue(values.get(i), "Series 1", categories.get(i));
                }
            }
        }

        // 2. Create Stacked Chart
        JFreeChart chart = ChartFactory.createStackedBarChart(
                config.getTitle(),
                "Category",
                "Value",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
        );

        // 3. Customize Plot
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        plot.setOutlineVisible(false);

        // 4. Custom Renderer (Flat Look)
        StackedBarRenderer renderer = (StackedBarRenderer) plot.getRenderer();
        renderer.setBarPainter(new StandardBarPainter()); // Flat, no glare
        renderer.setSeriesPaint(0, new Color(30, 60, 150)); // Appian Blue
        renderer.setShadowVisible(false);
        renderer.setDrawBarOutline(false);

        return chart;
    }
}