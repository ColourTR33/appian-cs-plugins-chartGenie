package com.appiancs.plugins.chartgenie.strategies.impl;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.strategies.ChartGeneratorStrategy;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.general.DefaultPieDataset;

import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.TextAnchor;

import java.awt.Color;
import java.awt.Font;
import java.util.List;

public class DonutChartStrategy implements ChartGeneratorStrategy {

    /**
     * The interface likely requires this specific signature.
     * We use ChartConfiguration instead of Map to ensure data passes through correctly.
     */
    @Override
    public JFreeChart generate(ChartConfiguration config) {

        // 1. Populate Data (This part was missing!)
        DefaultPieDataset dataset = new DefaultPieDataset();
        List<String> categories = config.getCategories();
        List<Number> values = config.getValues();

        // Check if we have data to graph
        if (categories != null && values != null) {
            for (int i = 0; i < categories.size(); i++) {
                if (i < values.size()) {
                    // Add data: (Label, Value)
                    dataset.setValue(categories.get(i), values.get(i));
                }
            }
        }
        // Fallback for Legacy Data (if using Maps)
        else if (config.getDataPoints() != null) {
            config.getDataPoints().forEach(dataset::setValue);
        }

        // 2. Create the Ring (Donut) Chart
        JFreeChart chart = ChartFactory.createRingChart(
                config.getTitle(),  // Chart Title
                dataset,            // Data
                true,               // Include Legend
                true,               // Tooltips
                false               // URLs
        );

        // 3. Customize Plot (Visuals)
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setSectionOutlinesVisible(false);
        plot.setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        plot.setNoDataMessage("No data available");
        plot.setCircular(true);
        plot.setLabelGap(0.02);

        // Optional: White background instead of default grey
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);

        // 4. Center Text Hack
        // Note: This relies on negative padding and might need adjustment
        // based on the exact chart size (height).
        TextTitle centerText = new TextTitle("Total");
        centerText.setPosition(RectangleEdge.BOTTOM);
        centerText.setPadding(new RectangleInsets(-100, 0, 0, 0));
        chart.addSubtitle(centerText);

        return chart;
    }
}