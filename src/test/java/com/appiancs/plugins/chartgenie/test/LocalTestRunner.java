package com.appiancs.plugins.chartgenie.test;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.dto.structure.DocumentSettings;
import com.appiancs.plugins.chartgenie.dto.structure.ReportRequest;
import com.appiancs.plugins.chartgenie.dto.structure.ReportSection;
import com.appiancs.plugins.chartgenie.service.ReportOrchestrationService;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class LocalTestRunner {

    public static void main(String[] args) {
        System.out.println("--- Starting Local Test Runner ---");

        try {
            // ============================================================
            // 1. CONSTRUCT THE REPORT REQUEST (The "JSON" Simulation)
            // ============================================================
            ReportRequest request = new ReportRequest();
            request.setDocumentSettings(new DocumentSettings("company_template.docx", "Master_Audit_Report.docx"));

            // --- TITLE PAGE ---
            request.addContent(ReportSection.createHeading("Audit & Performance Master Report"));
            request.addContent(ReportSection.createParagraph(
                    "This document serves as a comprehensive test of all available chart modules. " +
                            "It was generated dynamically by the ChartGenie plugin."
            ));

            // ============================================================
            // 2. COLUMN CHART (Standard)
            // ============================================================
            request.addContent(ReportSection.createHeading("1. Regional Sales (Column)"));

            ChartConfiguration colConfig = new ChartConfiguration();
            colConfig.setChartType("COLUMN");
            colConfig.setTitle("Sales by Region 2024");
            colConfig.setWidth(600);
            colConfig.setHeight(350);
            colConfig.setDataFromLists(
                    Arrays.asList("North", "South", "East", "West"),
                    Arrays.asList(120, 95, 150, 80)
            );
            request.addContent(ReportSection.createChart(colConfig));

            // ============================================================
            // 3. BAR CHART (Horizontal)
            // ============================================================
            request.addContent(ReportSection.createHeading("2. Risk Assessment (Bar)"));

            ChartConfiguration barConfig = new ChartConfiguration();
            barConfig.setChartType("BAR");
            barConfig.setTitle("Risk Count by Category");
            barConfig.setWidth(600);
            barConfig.setHeight(350);
            barConfig.setDataFromLists(
                    Arrays.asList("Operational", "Financial", "Compliance", "IT", "Reputational"),
                    Arrays.asList(45, 30, 60, 25, 10)
            );
            request.addContent(ReportSection.createChart(barConfig));

            // ============================================================
            // 4. DATA TABLE
            // ============================================================
            request.addContent(ReportSection.createParagraph("The following table details the specific risk metrics visualized above:"));

            request.addContent(ReportSection.createTable(
                    Arrays.asList("Risk Category", "Incident Count", "Severity"), // Headers
                    Arrays.asList(
                            Arrays.asList("Operational", "45", "High"),
                            Arrays.asList("Financial",   "30", "Medium"),
                            Arrays.asList("Compliance",  "60", "Critical"),
                            Arrays.asList("IT",          "25", "Medium")
                    )
            ));

            // ============================================================
            // 5. AREA CHART (New Type!)
            // ============================================================
            request.addContent(ReportSection.createHeading("3. Traffic Volume (Area)"));
            request.addContent(ReportSection.createParagraph("Visualizing cumulative volume trends over the first half of the year."));

            ChartConfiguration areaConfig = new ChartConfiguration();
            areaConfig.setChartType("AREA"); // Make sure your Factory handles "AREA"
            areaConfig.setTitle("Website Traffic (Cumulative)");
            areaConfig.setWidth(600);
            areaConfig.setHeight(350);
            areaConfig.setDataFromLists(
                    Arrays.asList("Jan", "Feb", "Mar", "Apr", "May", "Jun"),
                    Arrays.asList(100, 150, 180, 220, 210, 300)
            );
            request.addContent(ReportSection.createChart(areaConfig));

            // ============================================================
            // 6. DONUT CHART (Special Strategy)
            // ============================================================
            request.addContent(ReportSection.createHeading("4. Budget Distribution (Donut)"));

            ChartConfiguration donutConfig = new ChartConfiguration();
            donutConfig.setChartType("DONUT"); // Uses your new DonutChartStrategy
            donutConfig.setTitle("Q1 Budget Allocation");
            donutConfig.setWidth(500);
            donutConfig.setHeight(400); // Donuts need to be square-ish
            donutConfig.setDataFromLists(
                    Arrays.asList("Marketing", "R&D", "Sales", "Admin"),
                    Arrays.asList(40, 30, 20, 10)
            );
            request.addContent(ReportSection.createChart(donutConfig));

            // ... inside main method of LocalTestRunner ...

// ============================================================
// NEW: LINE CHART (Trend Analysis)
// ============================================================
            request.addContent(ReportSection.createHeading("5. Audit Issue Trends (Line)"));
            request.addContent(ReportSection.createParagraph(
                    "The trend line below indicates a steady decrease in high-severity issues over the last 6 months."
            ));

            ChartConfiguration lineConfig = new ChartConfiguration();
            lineConfig.setChartType("LINE");
            lineConfig.setTitle("Open Issues Trend");
            lineConfig.setWidth(600);
            lineConfig.setHeight(350);
            lineConfig.setDataFromLists(
                    Arrays.asList("Jan", "Feb", "Mar", "Apr", "May", "Jun"),
                    Arrays.asList(45, 42, 35, 28, 20, 15) // clear downward trend
            );
            request.addContent(ReportSection.createChart(lineConfig));
            // ... inside main method ...

// ============================================================
// NEW: STACKED BAR CHART
// ============================================================
            request.addContent(ReportSection.createHeading("6. Risk Composition (Stacked)"));
            request.addContent(ReportSection.createParagraph(
                    "Breakdown of risk exposure across key business units."
            ));

            ChartConfiguration stackedConfig = new ChartConfiguration();
            stackedConfig.setChartType("STACKED");
            stackedConfig.setTitle("Portfolio Risk Exposure");
            stackedConfig.setWidth(600);
            stackedConfig.setHeight(400);
            stackedConfig.setDataFromLists(
                    Arrays.asList("Unit A", "Unit B", "Unit C", "Unit D"),
                    Arrays.asList(500, 300, 150, 50)
            );
            request.addContent(ReportSection.createChart(stackedConfig));

            // ============================================================
            // 7. EXECUTE ENGINE
            // ============================================================
            System.out.println("... Sending Request to Orchestration Service ...");

            ReportOrchestrationService engine = new ReportOrchestrationService();
            File finalReport = engine.generateReport(request);

            System.out.println("\n========================================================");
            System.out.println(" [SUCCESS] Test Complete.");
            System.out.println(" Generated Report: " + finalReport.getAbsolutePath());
            System.out.println("========================================================");

        } catch (Exception e) {
            System.err.println("Test Failed with Exception:");
            e.printStackTrace();
        }
    }
}