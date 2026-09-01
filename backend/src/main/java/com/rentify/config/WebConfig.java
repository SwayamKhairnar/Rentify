package com.rentify.config;

import com.rentify.item.ItemCategory;
import com.rentify.item.ItemCondition;
import com.rentify.notification.NotificationType;
import com.rentify.rental.RentalStatus;
import com.rentify.report.AdminAction;
import com.rentify.report.ReportReason;
import com.rentify.report.ReportStatus;
import com.rentify.review.ReviewType;
import com.rentify.user.UserRole;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, ItemCategory.class, source -> source != null && !source.isBlank() ? ItemCategory.fromValue(source) : null);
        registry.addConverter(String.class, ItemCondition.class, source -> source != null && !source.isBlank() ? ItemCondition.fromValue(source) : null);
        registry.addConverter(String.class, RentalStatus.class, source -> source != null && !source.isBlank() ? RentalStatus.fromValue(source) : null);
        registry.addConverter(String.class, UserRole.class, source -> source != null && !source.isBlank() ? UserRole.fromValue(source) : null);
        registry.addConverter(String.class, ReviewType.class, source -> source != null && !source.isBlank() ? ReviewType.fromValue(source) : null);
        registry.addConverter(String.class, NotificationType.class, source -> source != null && !source.isBlank() ? NotificationType.fromValue(source) : null);
        registry.addConverter(String.class, ReportReason.class, source -> source != null && !source.isBlank() ? ReportReason.fromValue(source) : null);
        registry.addConverter(String.class, ReportStatus.class, source -> source != null && !source.isBlank() ? ReportStatus.fromValue(source) : null);
        registry.addConverter(String.class, AdminAction.class, source -> source != null && !source.isBlank() ? AdminAction.fromValue(source) : null);
    }
}
