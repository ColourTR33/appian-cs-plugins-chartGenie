package com.appiancs.plugins.chartgenie.dto;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement(name = "chart-configuration")
@XmlAccessorType(XmlAccessType.FIELD)
public class ChartConfiguration {

    @XmlElement(name = "chart-type")
    private String chartType;

    @XmlElement(name = "title")
    private String title;

    @XmlElement(name = "width")
    private int width;

    @XmlElement(name = "height")
    private int height;

    // --- NEW FIELDS (For Local Test Runner) ---
    @XmlElement(name = "categories")
    private List<String> categories;

    @XmlElement(name = "values")
    private List<Number> values;

    // --- LEGACY FIELDS (For Old Strategies) ---
    @XmlTransient // transient means "don't try to save this to XML"
    private Map<String, Double> dataPoints = new HashMap<>();

    @XmlTransient
    private Color primaryColor = new Color(30, 60, 150); // Default Blue

    @XmlTransient
    private List<String> hexColors = new ArrayList<>();

    // --- HELPER: Syncs the Lists (New) with the Map (Old) ---
    // This ensures both parts of your code work together
    public void setDataFromLists(List<String> cats, List<Number> vals) {
        this.categories = cats;
        this.values = vals;

        // Auto-fill the legacy map so old strategies work
        this.dataPoints.clear();
        if (cats != null && vals != null) {
            for (int i = 0; i < cats.size(); i++) {
                if (i < vals.size()) {
                    this.dataPoints.put(cats.get(i), vals.get(i).doubleValue());
                }
            }
        }
    }

    // --- GETTERS & SETTERS ---
    public String getChartType() { return chartType; }
    public void setChartType(String chartType) { this.chartType = chartType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }

    public List<Number> getValues() { return values; }
    public void setValues(List<Number> values) { this.values = values; }

    // --- LEGACY GETTERS (Fixes "cannot find symbol") ---
    public Map<String, Double> getDataPoints() { return dataPoints; }
    public void setDataPoints(Map<String, Double> dataPoints) { this.dataPoints = dataPoints; }

    public Color getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(Color primaryColor) { this.primaryColor = primaryColor; }

    public List<String> getHexColors() { return hexColors; }
    public void setHexColors(List<String> hexColors) { this.hexColors = hexColors; }
}