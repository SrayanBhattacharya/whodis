package com.whodis.backend.referenceimage.service;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@Service
public class ImageValidationService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final int MAX_WIDTH = 4096;
    private static final int MAX_HEIGHT = 4096;
    private static final long MAX_PIXELS = 16_777_216L;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final Tika tika = new Tika();

    public void validate(MultipartFile file) {
        validateBasicProperties(file);

        String detectedType = detectContentType(file);

        validateContentType(detectedType);

        validateImage(file);
    }

    private void validateBasicProperties(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidImageException("File must not be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidImageException(
                    "File size must not exceed 10 MB"
            );
        }
    }

    private String detectContentType(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return tika.detect(inputStream);
        } catch (IOException e) {
            throw new InvalidImageException(
                    "Unable to inspect uploaded file"
            );
        }
    }

    private void validateContentType(String detectedType) {
        if (!ALLOWED_CONTENT_TYPES.contains(detectedType)) {
            throw new InvalidImageException(
                    "Unsupported image type: " + detectedType
            );
        }
    }

    private void validateImage(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {

            BufferedImage image = ImageIO.read(inputStream);

            if (image == null) {
                throw new InvalidImageException(
                        "Uploaded file is not a valid image"
                );
            }

            validateDimensions(image);

        } catch (IOException e) {
            throw new InvalidImageException(
                    "Unable to decode uploaded image"
            );
        }
    }

    private void validateDimensions(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        if (width > MAX_WIDTH || height > MAX_HEIGHT) {
            throw new InvalidImageException(
                    "Image dimensions must not exceed 4096x4096 pixels"
            );
        }

        long pixels = (long) width * height;

        if (pixels > MAX_PIXELS) {
            throw new InvalidImageException(
                    "Image contains too many pixels"
            );
        }
    }
}
