package com.tourism.business;

import com.tourism.business.business.controller.MediaController;
import com.tourism.business.common.ApiException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import static org.junit.jupiter.api.Assertions.*;

class MediaControllerTest {
    @TempDir Path directory;

    @Test void storesDecodedPngAndServesItWithoutUsingClientFilename() throws Exception {
        var controller = new MediaController(directory.toString(), "https://example.com/");
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(20, 10, BufferedImage.TYPE_INT_ARGB), "png", output);
        var response = controller.upload(new MockMultipartFile("file", "../../bad.png", "image/png", output.toByteArray()));
        String url = response.get("url");
        assertTrue(url.startsWith("https://example.com/api/v1/media/"));
        String name = url.substring(url.lastIndexOf('/') + 1);
        var image = controller.read(name);
        assertEquals("image/png", image.getHeaders().getContentType().toString());
        try (var input = image.getBody().getInputStream()) {
            assertEquals(20, ImageIO.read(input).getWidth());
        }
        assertEquals("MEDIA_NOT_FOUND", assertThrows(ApiException.class, () -> controller.read("../bad.png")).code);
    }

    @Test void rejectsFakeImagesAndOversizedFiles() throws Exception {
        var controller = new MediaController(directory.toString(), "http://localhost:8080");
        assertEquals("INVALID_IMAGE", assertThrows(ApiException.class, () -> controller.upload(
            new MockMultipartFile("file", "fake.png", "image/png", "<script/>".getBytes()))).code);
        assertEquals(413, assertThrows(ApiException.class, () -> controller.upload(
            new MockMultipartFile("file", "large.png", "image/png", new byte[10 * 1024 * 1024 + 1]))).status);
    }
}
