package com.whodis.backend.photo.service;

import com.whodis.backend.photo.entity.Photo;
import com.whodis.backend.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZipStreamingService {

    private final StorageService storageService;

    public StreamingResponseBody streamPhotosAsZip(List<Photo> photos) {
        return (OutputStream outputStream) -> {
            try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
                Set<String> usedEntryNames = new HashSet<>();
                byte[] buffer = new byte[8192];

                for (Photo photo : photos) {
                    String entryName = resolveUniqueEntryName(
                            photo.getOriginalFilename(),
                            usedEntryNames
                    );

                    ZipEntry zipEntry = new ZipEntry(entryName);
                    zipOut.putNextEntry(zipEntry);

                    try (InputStream photoStream = storageService.load(photo.getStorageKey())) {
                        int bytesRead;
                        while ((bytesRead = photoStream.read(buffer)) != -1) {
                            zipOut.write(buffer, 0, bytesRead);
                        }
                    }

                    zipOut.closeEntry();
                }

                zipOut.finish();
            } catch (Exception e) {
                log.error("Error streaming ZIP archive: {}", e.getMessage(), e);
                throw e;
            }
        };
    }

    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "photo.jpg";
        }

        // Strip path traversal components by extracting only the base name
        String baseName = Paths.get(filename).getFileName().toString();

        // Remove illegal/troublesome characters
        String sanitized = baseName.replaceAll("[/\\\\:*?\"<>|]", "_").trim();

        if (sanitized.isEmpty() || sanitized.equals(".") || sanitized.equals("..")) {
            return "photo.jpg";
        }

        return sanitized;
    }

    public static String resolveUniqueEntryName(String originalFilename, Set<String> usedNames) {
        String safeName = sanitizeFilename(originalFilename);

        if (!usedNames.contains(safeName)) {
            usedNames.add(safeName);
            return safeName;
        }

        int dotIndex = safeName.lastIndexOf('.');
        String nameWithoutExt = dotIndex > 0 ? safeName.substring(0, dotIndex) : safeName;
        String ext = dotIndex > 0 ? safeName.substring(dotIndex) : "";

        int counter = 1;
        String candidate;
        do {
            candidate = nameWithoutExt + "_" + counter + ext;
            counter++;
        } while (usedNames.contains(candidate));

        usedNames.add(candidate);
        return candidate;
    }
}
