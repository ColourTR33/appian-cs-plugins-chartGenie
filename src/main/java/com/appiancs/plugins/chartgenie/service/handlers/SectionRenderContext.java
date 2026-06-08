package com.appiancs.plugins.chartgenie.service.handlers;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;

/**
 * Immutable context object passed to each {@link SectionHandler} during rendering.
 * Contains the document, optional cell target, available width, and sidebar flag.
 */
public class SectionRenderContext {

    private final XWPFDocument document;
    private final XWPFTableCell cell;
    private final int availableWidthTwips;
    private final boolean isSidebar;

    /**
     * Creates a new render context.
     *
     * @param document           the Word document being rendered into
     * @param cell               the table cell to render into, or null if rendering to document body
     * @param availableWidthTwips the available width in twips for the section content
     * @param isSidebar          true if this section is being rendered inside a sidebar layout
     */
    public SectionRenderContext(XWPFDocument document, XWPFTableCell cell, int availableWidthTwips, boolean isSidebar) {
        this.document = document;
        this.cell = cell;
        this.availableWidthTwips = availableWidthTwips;
        this.isSidebar = isSidebar;
    }

    public XWPFDocument getDocument() {
        return document;
    }

    /**
     * Returns the table cell to render into, or null if rendering directly to the document body.
     */
    public XWPFTableCell getCell() {
        return cell;
    }

    public int getAvailableWidthTwips() {
        return availableWidthTwips;
    }

    public boolean isSidebar() {
        return isSidebar;
    }
}
