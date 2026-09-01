package com.rentify.item;

import com.rentify.auth.security.CustomUserDetails;
import com.rentify.common.ApiResponse;
import com.rentify.common.CurrentUser;
import com.rentify.common.PaginatedResponse;
import com.rentify.item.dto.CreateItemRequest;
import com.rentify.item.dto.ItemResponse;
import com.rentify.item.dto.UpdateItemRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<ItemResponse>> getItems(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ItemCategory category,
            @RequestParam(required = false) ItemCondition condition,
            @RequestParam(required = false) Long owner,
            @RequestParam(required = false) String sort
    ) {
        PaginatedResponse<ItemResponse> response = itemService.getItems(
                page, limit, search, category, condition, owner, sort
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<Map<String, List<ItemResponse>>>> getMyItems(
            @CurrentUser CustomUserDetails userDetails
    ) {
        List<ItemResponse> items = itemService.getMyItems(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Your items fetched", Map.of("items", items)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, ItemResponse>>> getItemById(@PathVariable Long id) {
        ItemResponse item = itemService.getItemById(id);
        return ResponseEntity.ok(ApiResponse.success("Item fetched", Map.of("item", item)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, ItemResponse>>> createItem(
            @CurrentUser CustomUserDetails userDetails,
            @Valid @RequestBody CreateItemRequest request
    ) {
        ItemResponse createdItem = itemService.createItem(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Item created", Map.of("item", createdItem)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, ItemResponse>>> updateItem(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UpdateItemRequest request
    ) {
        ItemResponse updatedItem = itemService.updateItem(userDetails.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Item updated", Map.of("item", updatedItem)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long id
    ) {
        itemService.deleteItem(userDetails.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Item deleted"));
    }
}
