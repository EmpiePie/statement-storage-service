package za.co.statements.dto;

import java.time.YearMonth;

public record StatementMetadataDto(Long customerId, YearMonth period, String path) {}

