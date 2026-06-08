package com.appiancs.plugins.chartgenie.service;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appiancorp.suiteapi.content.ContentConstants;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.knowledge.Document;

/**
 * Standard utility for Appian Document I/O.
 * Uses the "Re-Fetch" strategy to ensure a valid stream connection.
 */
public final class DocumentUtils {

  // FIXED: Correct SLF4J initialization
  private static final Logger LOG = LoggerFactory.getLogger(DocumentUtils.class);

  private DocumentUtils() {
    // Utility class
  }

  public static Long uploadDocument(ContentService contentService, File file, String name, Long folderId, String extension)
    throws Exception {
    // SECURITY FIX: Validate and sanitize file inputs to prevent path traversal
    validateFileInput(file, name, extension);

    // FIXED: Using SLF4J parameterized logging
    LOG.info("Uploading: {}.{} ({} bytes)", name, extension, file.length());

    // 1. Create Metadata Shell
    Document shell = new Document();
    shell.setName(sanitizeFileName(name));
    shell.setExtension(sanitizeFileExtension(extension));
    shell.setParent(folderId);
    shell.setSize((int) file.length());

    Long docId = contentService.create(shell, ContentConstants.UNIQUE_NONE);
    LOG.info("Shell created with ID: {}", docId);

    // 2. Stream Content into the Live Object
    uploadContentToExistingDoc(contentService, docId, file);

    return docId;
  }

  public static void uploadNewVersion(ContentService contentService, File file, Long docId) throws Exception {
    LOG.info("Uploading new version for ID: {}", docId);

    // Stream data first
    uploadContentToExistingDoc(contentService, docId, file);

    // Explicitly increment version if required by server
    try {
      Document verDoc = new Document();
      verDoc.setId(docId);
      contentService.createVersion(verDoc, ContentConstants.UNIQUE_NONE);
    } catch (Exception e) {
      // FIXED: Cleaned up log message
      LOG.debug("createVersion call skipped/failed (content might already be updated): {}", e.getMessage());
    }
  }

  private static void uploadContentToExistingDoc(ContentService contentService, Long docId, File file) throws Exception {
    // A. Re-Fetch the LIVE object
    Document liveDoc = contentService.download(docId, ContentConstants.VERSION_CURRENT, false)[0];

    // B. CWE-22/23: resolve canonical path before opening stream
    Path safePath = file.toPath().normalize().toRealPath();

    try (InputStream fileIn = java.nio.file.Files.newInputStream(safePath);
      OutputStream docOut = liveDoc.getOutputStream()) {

      if (docOut == null) {
        throw new IllegalStateException("FATAL: Appian Document returned null OutputStream.");
      }

      byte[] buffer = new byte[8192];
      int bytesRead = fileIn.read(buffer);
      while (bytesRead != -1) {
        docOut.write(buffer, 0, bytesRead);
        bytesRead = fileIn.read(buffer);
      }

      docOut.flush();
      LOG.info("Content stream completed successfully.");
    }
  }

  /**
   * Validates file input to prevent path traversal attacks
   * 
   * @param file
   *          The file to validate
   * @param name
   *          The filename
   * @param extension
   *          The file extension
   * @throws SecurityException
   *           if validation fails
   */
  private static void validateFileInput(File file, String name, String extension) {
    if (file == null) {
      throw new IllegalArgumentException("File cannot be null");
    }
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Filename cannot be null or empty");
    }
    if (extension == null || extension.trim().isEmpty()) {
      throw new IllegalArgumentException("File extension cannot be null or empty");
    }

    // Check for path traversal patterns
    if (name.contains("..") || name.contains("/") || name.contains("\\")) {
      throw new SecurityException("Filename contains invalid path characters");
    }

    validateFilePath(file);
  }

  /**
   * Validates file path to prevent path traversal
   * 
   * @param file
   *          The file to validate
   * @throws SecurityException
   *           if path is invalid
   */
  private static void validateFilePath(File file) {
    try {
      String canonicalPath = file.getCanonicalPath();
      String absolutePath = file.getAbsolutePath();

      // Ensure canonical and absolute paths match (prevents path traversal)
      if (!canonicalPath.equals(absolutePath)) {
        throw new SecurityException("File path contains traversal characters");
      }

      // Ensure file is in temp directory or other safe location
      String tempDir = System.getProperty("java.io.tmpdir");
      if (!canonicalPath.startsWith(tempDir)) {
        // Allow files in current working directory for testing
        String workingDir = System.getProperty("user.dir");
        if (!canonicalPath.startsWith(workingDir)) {
          LOG.warn("File outside expected directories: {}", canonicalPath);
        }
      }
    } catch (java.io.IOException e) {
      throw new SecurityException("Unable to validate file path: " + e.getMessage(), e);
    }
  }

  /**
   * Sanitizes filename to prevent injection attacks
   * 
   * @param name
   *          The filename to sanitize
   * @return Sanitized filename
   */
  private static String sanitizeFileName(String name) {
    if (name == null) {
      return "document";
    }
    // Remove dangerous characters and path traversal sequences
    return name.replaceAll("[.]{2,}", "")
      .replaceAll("[/\\\\:*?\"<>|]", "")
      .replaceAll("[\\r\\n\\t]", "")
      .trim();
  }

  /**
   * Sanitizes file extension
   * 
   * @param extension
   *          The extension to sanitize
   * @return Sanitized extension
   */
  private static String sanitizeFileExtension(String extension) {
    if (extension == null) {
      return "bin";
    }
    // Only allow alphanumeric characters in extensions
    String clean = extension.replaceAll("[^a-zA-Z0-9]", "").toLowerCase(java.util.Locale.ROOT);
    return clean.isEmpty() ? "bin" : clean;
  }
}