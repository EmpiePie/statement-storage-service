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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        when(tokenStore.generateToken(anyString(), any())).thenReturn("abc123");

        DownloadLinkResponse resp = service.createDownloadLink(1L, YearMonth.of(2024,1));

        assertEquals("/api/public/download/abc123", resp.url());
        assertEquals(300, resp.expiresInSeconds());
    }

    @Test
    void downloadViaTokenInvalid() {
        when(tokenStore.validateToken("tok")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> service.downloadViaToken("tok"));
    }
}

