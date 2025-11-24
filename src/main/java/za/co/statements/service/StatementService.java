package za.co.statements.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import za.co.statements.token.DownloadTokenStore;
import za.co.statements.dto.response.DownloadLinkResponse;
import za.co.statements.dto.response.UploadResponse;
import za.co.statements.dto.StatementMetadataDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Duration;

import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatementService {

    private final StorageService storageService;
    private final DownloadTokenStore tokenStore;

    @Value("${statement.token.ttl-seconds}")
    private long tokenTtlSeconds;

    public UploadResponse saveStatement(Long customerId, YearMonth period, byte[] pdfBytes) {
        String path = buildPath(customerId, period);
        log.info("Saving statement customerId={} period={} path={}", customerId, period, path);
        storageService.upload(path, pdfBytes);
        return new UploadResponse("Statement uploaded", path);
    }

    public DownloadLinkResponse createDownloadLink(Long customerId, YearMonth period) {
        String path = buildPath(customerId, period); // FIX: unify path (includes .pdf)
        String token = tokenStore.generateToken(path, Duration.ofSeconds(tokenTtlSeconds));
        String url = "/api/public/download/" + token;
        return new DownloadLinkResponse(url, tokenTtlSeconds);
    }

    public byte[] downloadViaToken(String token) {
        log.info("Attempting download via token={}", token);
        String path = tokenStore.validateToken(token);
        if (path == null) {
            log.warn("Invalid or expired token={}", token);
            throw new IllegalArgumentException("Token invalid or expired");
        }
        log.info("Token valid. Serving path={}", path);
        return storageService.read(path);
    }

    private String buildPath(Long customerId, YearMonth period) {
        return String.format("statements/%d/%s.pdf", customerId, period);
    }

    public Page<StatementMetadataDto> listStatements(Long customerId, Pageable pageable) {
        List<String> allPaths = storageService.list("statements/" + customerId + "/");
        List<StatementMetadataDto> entries = allPaths.stream()
                .map(this::toMetadata)
                .sorted(Comparator.comparing(StatementMetadataDto::period).reversed())
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), entries.size());
        List<StatementMetadataDto> pageContent = (start <= end ? entries.subList(start, end) : List.of());
        return new PageImpl<>(pageContent, pageable, entries.size());
    }

    private StatementMetadataDto toMetadata(String path) {
        String[] parts = path.split("/");
        Long customerId = Long.valueOf(parts[1]);
        String periodString = parts[2].replace(".pdf", "");
        YearMonth ym = YearMonth.parse(periodString);
        return new StatementMetadataDto(customerId, ym, path);
    }
}



