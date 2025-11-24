package za.co.statements.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import za.co.statements.dto.response.ErrorResponse;
import lombok.RequiredArgsConstructor;
import za.co.statements.service.StatementService;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Tag(
        name = "Public Statement Download",
        description = "Public-facing endpoints for downloading statements using secure temporary tokens"
)
public class PublicDownloadController {

    private final StatementService statementService;

    @Operation(
            summary = "Download statement via token",
            description = """
                    Downloads a PDF statement using a temporary secure token.
                    The token is created via the `/api/statements/{customerId}/{year}/{month}/download-link` endpoint
                    and expires in a few minutes.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF downloaded successfully",
                    content = @Content(mediaType = "application/pdf")),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/download/{token}")
    public ResponseEntity<byte[]> download(@PathVariable String token) {
        log.info("Downloading via token={}", token);

        byte[] pdf = statementService.downloadViaToken(token);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=statement.pdf")
                .body(pdf);
    }
}
