package com.appiancs.plugins.chartgenie.strategies.impl;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import java.awt.Color;
import java.util.List;

public class BarChartStrategy implements ChartGeneratorStrategy {

    @Override
    public JFreeChart generate(ChartConfiguration config) {
        // 1. Populate Data
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        List<String> categories = config.getCategories();
        List<Number> values = config.getValues();

        if (categories != null && values != null) {
            for (int i = 0; i < categories.size(); i++) {
                if (i < values.size()) {
                    dataset.addValue(values.get(i), "Series 1", categories.get(i));
                }
            }
        }

        // 2. Create Horizontal Chart
        JFreeChart chart = ChartFactory.createBarChart(
                config.getTitle(),
                "Category", "Value", dataset,
                PlotOrientation.HORIZONTAL, // <--- CRITICAL: This makes it a "Bar" chart
                false, true, false
        );

        // 3. Styling
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setSeriesPaint(0, new Color(30, 60, 150));
        renderer.setShadowVisible(false);

        return chart;
    }
}