package com.appiancs.plugins.chartgenie.service.handlers;

import com.appiancs.plugins.chartgenie.dto.structure.ReportSection;

/**
 * A handler responsible for rendering a single section type into a Word document or table cell.
 */
@FunctionalInterface
public interface SectionHandler {

    /**
     * Renders a section into the document or cell context.
     *
     * @param context rendering context containing doc, cell, and layout params
     * @param section the section data to render
     * @throws Exception if rendering fails
     */
    void render(SectionRenderContext context, ReportSection section) throws Exception;
}
