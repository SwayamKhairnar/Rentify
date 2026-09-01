package com.rentify.notification;

import com.rentify.auth.security.CustomUserDetails;
import com.rentify.common.ApiResponse;
import com.rentify.common.CurrentUser;
import com.rentify.notification.dto.NotificationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, List<NotificationResponse>>>> getNotifications(
            @CurrentUser CustomUserDetails userDetails
    ) {
        List<NotificationResponse> notifications = notificationService.getNotifications(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Notifications fetched", Map.of("notifications", notifications)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @CurrentUser CustomUserDetails userDetails
    ) {
        long count = notificationService.getUnreadCount(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Unread count fetched", Map.of("count", count, "unreadCount", count)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Map<String, NotificationResponse>>> markAsRead(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long id
    ) {
        NotificationResponse notification = notificationService.markAsRead(userDetails.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", Map.of("notification", notification)));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @CurrentUser CustomUserDetails userDetails
    ) {
        notificationService.markAllAsRead(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long id
    ) {
        notificationService.deleteNotification(userDetails.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted"));
    }
}
