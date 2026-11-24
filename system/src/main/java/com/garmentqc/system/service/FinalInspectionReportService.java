package com.garmentqc.system.service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.garmentqc.system.dto.FinalInspectionReportResponse;
import com.garmentqc.system.enums.FinalInspectionPurpose;
import com.garmentqc.system.enums.FinalInspectionResult;
import com.garmentqc.system.exception.UnauthorizedException;
import com.garmentqc.system.model.FinalInspection;
import com.garmentqc.system.model.User;
import com.garmentqc.system.repository.FinalInspectionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FinalInspectionReportService {

    private final FinalInspectionRepository finalInspectionRepository;

    @Transactional(readOnly = true)
    public FinalInspectionReportResponse getDailyReport(LocalDate date) {
        validateReportAccess();

        if (date == null) {
            throw new IllegalArgumentException("Tanggal laporan wajib diisi");
        }

        return generateReport("DAILY", date, date);
    }

    @Transactional(readOnly = true)
    public FinalInspectionReportResponse getWeeklyReport(LocalDate date) {
        validateReportAccess();

        if (date == null) {
            throw new IllegalArgumentException("Tanggal laporan wajib diisi");
        }

        LocalDate startDate = getWeeklyStartDate(date);
        LocalDate endDate = startDate.plusDays(6);

        return generateReport("WEEKLY", startDate, endDate);
    }

    @Transactional(readOnly = true)
    public FinalInspectionReportResponse getMonthlyReport(int year, int month) {
        validateReportAccess();

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        return generateReport("MONTHLY", startDate, endDate);
    }

    private FinalInspectionReportResponse generateReport(String reportType, LocalDate startDate, LocalDate endDate) {
        List<FinalInspection> inspections = finalInspectionRepository.findByInspectionDateBetween(startDate, endDate);

        Accumulator overall = new Accumulator();

        Map<FinalInspectionPurpose, Accumulator> purposeMap = new LinkedHashMap<>();
        Map<String, Accumulator> buyerMap = new LinkedHashMap<>();
        Map<String, String> buyerNameMap = new LinkedHashMap<>();
        Map<String, Accumulator> resultMap = new LinkedHashMap<>();

        for (FinalInspection inspection : inspections) {
            overall.add(inspection);

            FinalInspectionPurpose purpose = inspection.getPurpose();

            purposeMap.computeIfAbsent(
                    purpose,
                    key -> new Accumulator())
                    .add(inspection);

            String buyerId = inspection.getBuyer() != null
                    ? inspection.getBuyer().getId()
                    : "UNKNOWN";

            String buyerName = inspection.getBuyer() != null
                    ? inspection.getBuyer().getName()
                    : "Unknown";

            buyerNameMap.putIfAbsent(buyerId, buyerName);

            buyerMap.computeIfAbsent(
                    buyerId,
                    key -> new Accumulator())
                    .add(inspection);

            String resultKey = inspection.getInspectionResult() == null
                    ? "PENDING"
                    : inspection.getInspectionResult().name();

            resultMap.computeIfAbsent(
                    resultKey,
                    key -> new Accumulator())
                    .add(inspection);
        }

        List<FinalInspectionReportResponse.PurposeSummary> purposeSummaries = purposeMap
                .entrySet()
                .stream()
                .map(entry -> {
                    FinalInspectionPurpose purpose = entry.getKey();
                    Accumulator value = entry.getValue();

                    return new FinalInspectionReportResponse.PurposeSummary(
                            purpose,
                            purpose != null ? purpose.getLabel() : "Tidak ditentukan",
                            value.totalInspections,
                            value.totalInspected,
                            value.totalDefects,
                            value.totalMajor,
                            value.totalMinor,
                            value.totalRelease,
                            value.totalReject,
                            value.totalPendingResult);
                })
                .toList();

        List<FinalInspectionReportResponse.BuyerSummary> buyerSummaries = buyerMap
                .entrySet()
                .stream()
                .map(entry -> {
                    Accumulator value = entry.getValue();
                    String buyerName = buyerNameMap.getOrDefault(
                            entry.getKey(),
                            "Unknown");

                    return new FinalInspectionReportResponse.BuyerSummary(
                            buyerName,
                            value.totalInspections,
                            value.totalInspected,
                            value.totalDefects,
                            value.totalMajor,
                            value.totalMinor,
                            value.totalRelease,
                            value.totalReject,
                            value.totalPendingResult);
                })
                .toList();

        List<FinalInspectionReportResponse.ResultSummary> resultSummaries = resultMap
                .entrySet()
                .stream()
                .map(entry -> {
                    Accumulator value = entry.getValue();

                    String resultLabel = switch (entry.getKey()) {
                        case "SHIPMENT_RELEASE" -> "Shipment Release";
                        case "SHIPMENT_REJECT" -> "Shipment Reject";
                        default -> "Pending";
                    };

                    return new FinalInspectionReportResponse.ResultSummary(
                            entry.getKey(),
                            resultLabel,
                            value.totalInspections,
                            value.totalInspected,
                            value.totalDefects,
                            value.totalMajor,
                            value.totalMinor);
                })
                .toList();

        return new FinalInspectionReportResponse(
                reportType,
                startDate,
                endDate,
                overall.totalInspections,
                overall.totalInspected,
                overall.totalDefects,
                overall.totalMajor,
                overall.totalMinor,
                overall.totalRelease,
                overall.totalReject,
                overall.totalPendingResult,
                purposeSummaries,
                buyerSummaries,
                resultSummaries);
    }

    private LocalDate getWeeklyStartDate(LocalDate date) {
        int daysSinceFriday = (date.getDayOfWeek().getValue() - 5 + 7) % 7;

        return date.minusDays(daysSinceFriday);
    }

    private void validateReportAccess() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof User currentUser)) {

            throw new UnauthorizedException("Autentikasi diperlukan");
        }

        if (currentUser.getRole() == null) {
            throw new UnauthorizedException("Role pengguna tidak valid");
        }

        String role = currentUser.getRole().name();

        if ("SUPER_ADMIN".equals(role) || "VISITOR".equals(role)) {
            return;
        }

        if (("ADMIN".equals(role) || "USER".equals(role))
                && currentUser.getSection() != null
                && "FINAL".equals(currentUser.getSection().name())) {
            return;
        }

        throw new UnauthorizedException(
                "Hanya pengguna section FINAL dan Super Admin yang dapat mengakses laporan Final Inspection");
    }

    private static class Accumulator {

        private long totalInspections;
        private long totalInspected;
        private long totalDefects;
        private long totalMajor;
        private long totalMinor;
        private long totalRelease;
        private long totalReject;
        private long totalPendingResult;

        private void add(FinalInspection inspection) {
            totalInspections++;

            totalInspected += valueOrZero(inspection.getTotalInspected());
            totalDefects += valueOrZero(inspection.getTotalDefect());
            totalMajor += valueOrZero(inspection.getTotalMajor());
            totalMinor += valueOrZero(inspection.getTotalMinor());

            FinalInspectionResult result = inspection.getInspectionResult();

            if (result == FinalInspectionResult.SHIPMENT_RELEASE) {
                totalRelease++;
            } else if (result == FinalInspectionResult.SHIPMENT_REJECT) {
                totalReject++;
            } else {
                totalPendingResult++;
            }
        }

        private long valueOrZero(Integer value) {
            return value == null ? 0L : value.longValue();
        }
    }
}