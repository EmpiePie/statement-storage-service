package za.co.statements.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import za.co.statements.dto.response.ErrorResponse;

import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;
import za.co.statements.dto.StatementMetadataDto;
import za.co.statements.dto.response.DownloadLinkResponse;
import za.co.statements.dto.response.UploadResponse;
import za.co.statements.service.StatementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.time.YearMonth;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@Slf4j
@RequestMapping("/api/statements")
@RequiredArgsConstructor
@Tag(
        name = "Statements",
        description = "Endpoints for uploading, listing, and downloading customer account statements"
)
public class StatementController {

    private final StatementService statementService;

// --------------------------------------------------------------
// Upload Statement (Multipart PDF Upload — Filename-Based)
// --------------------------------------------------------------

    @Operation(
            summary = "Upload a customer statement PDF",
            description = """
                Uploads a PDF file where the filename encodes the customer and period.
                
                **Expected filename format:**
                ```
                statement_<customerId>_<year>_<month>.pdf
                ```
                Examples:
                - `statement_123_2024_10.pdf`
                - `statement_99999_2023_01.pdf`
                
                The backend automatically extracts:
                
                - `customerId` from filename
                - `year` from filename
                - `month` from filename
                
                ### How to upload in Postman
                1. Select **POST**
                2. URL: `/api/statements/upload`
                3. Body → `form-data`
                4. Add a key named **file**
                5. Type = *File*
                6. Choose a PDF file with the filename format above
                
                ### Content type:
                - `multipart/form-data`
                """
    )
    @Parameters({
            @Parameter(
                    name = "file",
                    description = "PDF file named using the format `statement_<customerId>_<year>_<month>.pdf`",
                    required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statement uploaded successfully",
                    content = @Content(schema = @Schema(implementation = UploadResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file or filename format",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.matches("statement_\\d+_\\d{4}_\\d{1,2}.*")) {
            return ResponseEntity.badRequest()
                    .body("Invalid filename format. Expected: statement_<customerId>_<year>_<month>");
        }

        Pattern pattern = Pattern.compile("statement_(\\d+)_(\\d{4})_(\\d{1,2})");
        Matcher matcher = pattern.matcher(filename);

        if (!matcher.find()) {
            return ResponseEntity.badRequest()
                    .body("Unable to extract customerId, year, month from filename.");
        }

        long customerId = Long.parseLong(matcher.group(1));
        int year = Integer.parseInt(matcher.group(2));
        int month = Integer.parseInt(matcher.group(3));

        byte[] pdfBytes = file.getBytes();

        statementService.saveStatement(
                customerId,
                YearMonth.of(year, month),
                pdfBytes
        );

        // ✔ Clean text-only response
        return ResponseEntity.ok("Statement Uploaded");
    }


    // --------------------------------------------------------------
    // Create Download Link
    // --------------------------------------------------------------

    @Operation(
            summary = "Generate a time-limited download link",
            description = """
                    Generates a secure, time-limited token-based download link
                    for a customer's statement PDF.
                    
                    The link expires after the configured TTL (default 5 minutes).
                    """
    )
    @Parameters({
            @Parameter(name = "customerId", example = "12345"),
            @Parameter(name = "year", example = "2024"),
            @Parameter(name = "month", example = "1")
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Download link created"),
            @ApiResponse(responseCode = "404", description = "Statement not found")
    })
    @GetMapping("/{customerId}/{year}/{month}/download-link")
    public ResponseEntity<DownloadLinkResponse> getDownloadLink(
            @PathVariable Long customerId,
            @PathVariable int year,
            @PathVariable int month) {

        DownloadLinkResponse link = statementService.createDownloadLink(
                customerId,
                YearMonth.of(year, month)
        );

        return ResponseEntity.ok(link);
    }

    // --------------------------------------------------------------
    // Paginated Statement Listing
    // --------------------------------------------------------------

    @Operation(
            summary = "List all statements for a customer (paginated)",
            description = """
                    Returns a paginated list of stored statements for a customer.
                    
                    Accepts query parameters:
                    - page (default: 0)
                    - size (default: 10)
                    - sort (default: period,desc)
                    
                    Example:
                    GET /api/statements/123?page=0&size=20&sort=period,desc
                    """
    )
    @Parameters({
            @Parameter(name = "customerId", example = "12345"),
            @Parameter(name = "page", example = "0", description = "Page number (0-based)"),
            @Parameter(name = "size", example = "10", description = "Page size"),
            @Parameter(name = "sort", example = "period,desc", description = "Sort field and direction")
    })
    @ApiResponse(responseCode = "200", description = "Statements retrieved")
    @GetMapping("/{customerId}")
    public Page<StatementMetadataDto> list(
            @PathVariable Long customerId,
            @PageableDefault(size = 10, sort = "period", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.info("Listing statements for customerId={} page={} size={} sort={}",
                customerId,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort()
        );

        return statementService.listStatements(customerId, pageable);
    }
}




