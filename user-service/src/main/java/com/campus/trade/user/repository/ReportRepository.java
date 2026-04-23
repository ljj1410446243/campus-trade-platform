package com.campus.trade.user.repository;

import com.campus.trade.user.model.Report;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ReportRepository extends MongoRepository<Report, String> {

    boolean existsByReporterIdAndReportTypeAndTargetIdAndStatus(String reporterId,
                                                                String reportType,
                                                                String targetId,
                                                                String status);

    List<Report> findByReporterIdOrderByCreatedAtDesc(String reporterId);

    List<Report> findByStatusOrderByCreatedAtDesc(String status);

    List<Report> findAllByOrderByCreatedAtDesc();

    Optional<Report> findByIdAndStatus(String id, String status);
}
