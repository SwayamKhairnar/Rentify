package com.rentify.report;

import com.rentify.common.PaginatedResponse;
import com.rentify.exception.BadRequestException;
import com.rentify.exception.NotFoundException;
import com.rentify.item.Item;
import com.rentify.item.ItemRepository;
import com.rentify.notification.NotificationService;
import com.rentify.notification.NotificationType;
import com.rentify.rental.Rental;
import com.rentify.rental.RentalRepository;
import com.rentify.report.dto.CreateReportRequest;
import com.rentify.report.dto.ReportResponse;
import com.rentify.report.dto.ResolveReportRequest;
import com.rentify.user.User;
import com.rentify.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final RentalRepository rentalRepository;
    private final ItemRepository itemRepository;
    private final NotificationService notificationService;

    public ReportService(
            ReportRepository reportRepository,
            UserRepository userRepository,
            RentalRepository rentalRepository,
            ItemRepository itemRepository,
            NotificationService notificationService
    ) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.rentalRepository = rentalRepository;
        this.itemRepository = itemRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public ReportResponse createReport(Long reporterId, CreateReportRequest request) {
        if (reporterId.equals(request.reportedUserId())) {
            throw new BadRequestException("You cannot report yourself");
        }

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new NotFoundException("Reporter not found"));

        User reportedUser = userRepository.findById(request.reportedUserId())
                .orElseThrow(() -> new NotFoundException("Reported user not found"));

        Rental rental = rentalRepository.findById(request.rentalId())
                .orElseThrow(() -> new NotFoundException("Rental not found"));

        Report report = new Report();
        report.setReporter(reporter);
        report.setReportedUser(reportedUser);
        report.setRental(rental);
        report.setReason(request.reason());
        report.setDescription(request.description().trim());
        report.setEvidenceImage(request.evidenceImage() != null ? request.evidenceImage().trim() : "");
        report.setStatus(ReportStatus.PENDING);
        report.setAdminAction(AdminAction.NONE);
        report.setAdminNotes("");

        Report saved = reportRepository.save(report);
        return ReportResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> getMyReports(Long reporterId) {
        List<Report> reports = reportRepository.findByReporterIdOrderByCreatedAtDesc(reporterId);
        return reports.stream()
                .map(ReportResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<ReportResponse> getAdminReports(ReportStatus status, ReportReason reason, int page, int limit) {
        int validatedPage = Math.max(page, 1);
        int validatedLimit = Math.min(Math.max(limit, 1), 100);

        Pageable pageable = PageRequest.of(validatedPage - 1, validatedLimit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Report> spec = Specification.where(ReportSpecifications.hasStatus(status))
                .and(ReportSpecifications.hasReason(reason));

        Page<Report> pageResult = reportRepository.findAll(spec, pageable);

        List<ReportResponse> responses = pageResult.getContent().stream()
                .map(ReportResponse::fromEntity)
                .toList();

        return PaginatedResponse.of("Reports fetched", responses, validatedPage, validatedLimit, pageResult.getTotalElements());
    }

    @Transactional
    public ReportResponse resolveReport(Long adminId, Long reportId, ResolveReportRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("Report not found"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin user not found"));

        report.setStatus(request.status());
        if (request.adminAction() != null) {
            report.setAdminAction(request.adminAction());
        }
        if (request.adminNotes() != null) {
            report.setAdminNotes(request.adminNotes().trim());
        }

        if (report.getAdminAction() == AdminAction.ACCOUNT_SUSPENDED) {
            User target = report.getReportedUser();
            target.setSuspended(true);
            userRepository.save(target);
        } else if (report.getAdminAction() == AdminAction.LISTING_REMOVED) {
            if (report.getRental() != null && report.getRental().getItem() != null) {
                Item item = report.getRental().getItem();
                item.setAvailable(false);
                itemRepository.save(item);
            }
        }

        Report saved = reportRepository.save(report);

        notificationService.createNotification(
                report.getReporter(),
                admin,
                NotificationType.SYSTEM,
                "Report Update",
                "Your report regarding " + report.getReportedUser().getName() + " has been " + request.status().getValue() + ".",
                "/reports/my-reports"
        );

        return ReportResponse.fromEntity(saved);
    }
}
