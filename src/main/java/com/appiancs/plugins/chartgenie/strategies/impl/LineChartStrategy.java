package com.appiancs.plugins.chartgenie.strategies.impl;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.util.List;

public class LineChartStrategy implements ChartGeneratorStrategy {

    @Override
    public JFreeChart generate(ChartConfiguration config) {

        // 1. Populate Data
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        List<String> categories = config.getCategories();
        List<Number> values = config.getValues();

        if (categories != null && values != null) {
            for (int i = 0; i < categories.size(); i++) {
                if (i < values.size()) {
                    dataset.addValue(values.get(i), "Trend Series", categories.get(i));
                }
            }
        }

        // 2. Create Base Chart
        JFreeChart chart = ChartFactory.createLineChart(
                config.getTitle(),
                "Period",       // X-Axis Label
                "Value",        // Y-Axis Label
                dataset,
                PlotOrientation.VERTICAL,
                false, true, false
        );

        // 3. customize Plot
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        plot.setOutlineVisible(false);

        // 4. Professional Styling (Thick Lines + Dots)
        LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();

        // Enable "Shapes" (the dots at data points)
        renderer.setDefaultShapesVisible(true);
        renderer.setDrawOutlines(true);
        renderer.setUseFillPaint(true);
        renderer.setDefaultFillPaint(Color.WHITE); // White center for the dots

        // Make the line thicker (3.0f pixels)
        renderer.setSeriesStroke(0, new BasicStroke(3.0f));

        // Set Color (Appian Blue)
        renderer.setSeriesPaint(0, new Color(30, 60, 150));

        // Set Shape size (Circle)
        renderer.setSeriesShape(0, new Ellipse2D.Double(-4.0, -4.0, 8.0, 8.0));

        return chart;
    }
}