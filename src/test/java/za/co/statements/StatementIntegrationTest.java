package za.co.statements;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
class StatementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testUploadGenerateTokenDownloadFlow() throws Exception {

        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "statement_123_2024_10.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "This is a test PDF content".getBytes()
        );

        // ---- Upload ----
        mockMvc.perform(multipart("/api/statements/upload").file(pdfFile))
                .andExpect(status().isOk())
                .andExpect(content().string("Statement Uploaded"));

        // ---- Generate Token ----
        String tokenResponse = mockMvc.perform(
                        get("/api/statements/123/2024/10/download-link"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").exists())
                .andExpect(jsonPath("$.expiresInSeconds").value(5))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = mapper.readTree(tokenResponse);
        String url = root.get("url").asText();
        String token = url.substring(url.lastIndexOf('/') + 1);

        // ---- Download ----
        mockMvc.perform(get("/api/public/download/" + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(result ->
                        assertThat(result.getResponse().getContentAsByteArray())
                                .isEqualTo("This is a test PDF content".getBytes())
                );
    }

    @Test
    void testDownloadFailsForExpiredToken() throws Exception {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "statement_999_2024_01.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "expired".getBytes()
        );

        // ---- Upload ----
        mockMvc.perform(multipart("/api/statements/upload").file(pdfFile))
                .andExpect(status().isOk());

        // ---- Generate Token ----
        String json = mockMvc.perform(get("/api/statements/999/2024/1/download-link"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String tokenUrl = mapper.readTree(json).get("url").asText();
        String token = tokenUrl.substring(tokenUrl.lastIndexOf('/') + 1);

        // ---- Expire token by waiting > TTL (5s) ----
        Thread.sleep(6000);

        // ---- Attempt Download → expect new error format ----
        mockMvc.perform(get("/api/public/download/" + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Token invalid or expired"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}



