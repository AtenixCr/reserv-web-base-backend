package com.tourism.business.business.controller;

import com.tourism.business.common.ApiException;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/media")
public class MediaController {

    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final long MAX_PIXELS = 12_000_000L;

    private final Path directory;
    private final String publicBaseUrl;

    public MediaController(
        @Value("${app.media.directory}") String directory,
        @Value("${app.media.public-base-url}") String publicBaseUrl
    ) throws IOException {
        this.directory = Path.of(directory)
            .toAbsolutePath()
            .normalize();

        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");

        Files.createDirectories(this.directory);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> upload(
        @RequestParam("file") MultipartFile file
    ) {
        if (file.isEmpty()) {
            throw new ApiException(400, "INVALID_IMAGE");
        }

        if (file.getSize() > MAX_BYTES) {
            throw new ApiException(413, "FILE_TOO_LARGE");
        }

        BufferedImage image = decode(file);
        byte[] encoded;

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "png", output)) {
                throw new ApiException(400, "INVALID_IMAGE");
            }

            encoded = output.toByteArray();
        } catch (IOException error) {
            throw new ApiException(400, "INVALID_IMAGE");
        }

        if (encoded.length > MAX_BYTES) {
            throw new ApiException(413, "FILE_TOO_LARGE");
        }

        String name = UUID.randomUUID() + ".png";
        Path destination = directory.resolve(name);

        try {
            Files.write(
                destination,
                encoded,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
            );
        } catch (IOException error) {
            throw new ApiException(500, "MEDIA_STORAGE_ERROR");
        }

        return Map.of(
            "url",
            publicBaseUrl + "/api/v1/media/" + name
        );
    }

    @GetMapping("/{name}")
    public ResponseEntity<Resource> read(
        @PathVariable("name") String name
    ) {
        if (!name.matches(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.png"
        )) {
            throw new ApiException(404, "MEDIA_NOT_FOUND");
        }

        Path file = directory.resolve(name).normalize();

        if (!file.startsWith(directory) ||
            !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new ApiException(404, "MEDIA_NOT_FOUND");
        }

        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_PNG)
            .header("X-Content-Type-Options", "nosniff")
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"" + name + "\""
            )
            .body(new FileSystemResource(file));
    }

    private BufferedImage decode(MultipartFile file) {
        try (
            InputStream input = file.getInputStream();
            ImageInputStream imageInput =
                ImageIO.createImageInputStream(input)
        ) {
            if (imageInput == null) {
                throw new ApiException(400, "INVALID_IMAGE");
            }

            Iterator<ImageReader> readers =
                ImageIO.getImageReaders(imageInput);

            if (!readers.hasNext()) {
                throw new ApiException(400, "INVALID_IMAGE");
            }

            ImageReader reader = readers.next();

            try {
                String format = reader.getFormatName()
                    .toLowerCase(Locale.ROOT);

                if (!Set.of("png", "jpeg", "jpg").contains(format)) {
                    throw new ApiException(
                        400,
                        "UNSUPPORTED_IMAGE_FORMAT"
                    );
                }

                reader.setInput(imageInput, true, true);

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                if (width <= 0 || height <= 0 ||
                    (long) width * height > MAX_PIXELS) {
                    throw new ApiException(
                        400,
                        "IMAGE_DIMENSIONS_TOO_LARGE"
                    );
                }

                BufferedImage image = reader.read(0);

                if (image == null) {
                    throw new ApiException(400, "INVALID_IMAGE");
                }

                return image;
            } finally {
                reader.dispose();
            }
        } catch (IOException error) {
            throw new ApiException(400, "INVALID_IMAGE");
        }
    }
}