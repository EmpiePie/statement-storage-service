package za.co.statements.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import za.co.statements.dto.response.DownloadLinkResponse;
import za.co.statements.service.StatementService;
import za.co.statements.service.StorageService;
import za.co.statements.token.DownloadTokenStore;

import java.time.YearMonth;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatementServiceTest {

    @Mock
    StorageService storageService;
    @Mock
    DownloadTokenStore tokenStore;

    @InjectMocks
    StatementService service;

    @BeforeEach
    void init() {
        ReflectionTestUtils.setField(service, "tokenTtlSeconds", 300L);
    }

    @Test
    void createDownloadLinkSuccess() {
        Long customerId = 1L;
        YearMonth period = YearMonth.of(2024, 1);
        byte[] pdfBytes = "dummy".getBytes();

        // Save statement first
        service.saveStatement(customerId, period, pdfBytes);

        // Ensure StorageService.exists returns true (if mocked)
        when(storageService.exists(anyString())).thenReturn(true);

        DownloadLinkResponse response = service.createDownloadLink(customerId, period);

        assertNotNull(response);
        assertTrue(response.url().contains("/api/public/download/"));
        assertEquals(300, response.expiresInSeconds());
    }

    @Test
    void downloadViaTokenInvalid() {
        when(tokenStore.validateToken("tok")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> service.downloadViaToken("tok"));
    }
}

