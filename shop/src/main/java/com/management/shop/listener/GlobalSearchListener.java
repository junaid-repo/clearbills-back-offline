package com.management.shop.listener;

import com.management.shop.entity.BillingEntity;
import com.management.shop.entity.CustomerEntity;
import com.management.shop.entity.ProductEntity;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class GlobalSearchListener {

    private static JdbcTemplate jdbcTemplate;

    @Autowired
    public void setJdbcTemplate(@Lazy JdbcTemplate jdbcTemplate) {
        GlobalSearchListener.jdbcTemplate = jdbcTemplate;
    }

    // ========================================================================
    // SINGLE ENTRY POINTS (Required by JPA Spec)
    // ========================================================================

    @PostPersist
    public void onInsert(Object entity) {
        routeEntity(entity);
    }

    @PostUpdate
    public void onUpdate(Object entity) {
        routeEntity(entity);
    }

    // Helper to route to the correct specific handler
    private void routeEntity(Object entity) {
        if (entity instanceof CustomerEntity) {
            handleCustomer((CustomerEntity) entity);
        } else if (entity instanceof ProductEntity) {
            handleProduct((ProductEntity) entity);
        } else if (entity instanceof BillingEntity) {
            handleSales((BillingEntity) entity);
        }
    }

    // ========================================================================
    // SPECIFIC HANDLERS (No Annotations Here)
    // ========================================================================

    private void handleCustomer(CustomerEntity customer) {
        updateSearchIndex(
                customer.getId(),
                "CUSTOMER",
                customer.getName(),
                (customer.getName() + " " + (customer.getEmail() != null ? customer.getEmail() : "") + " " + (customer.getPhone() != null ? customer.getPhone() : "")).toLowerCase(),
                customer.getUserId(),
                "/customers/" + customer.getId(),
                customer.getIsActive() ? 1 : 0
        );
    }

    private void handleProduct(ProductEntity product) {
        updateSearchIndex(
                product.getId(),
                "PRODUCT",
                product.getName(),
                (product.getName() + " " + (product.getCategory() != null ? product.getCategory() : "") + " " + (product.getHsn() != null ? product.getHsn() : "")).toLowerCase(),
                product.getUserId(),
                "/products/" + product.getId(),
                product.getActive() ? 1 : 0
        );
    }

    private void handleSales(BillingEntity sale) {
        String invoice = sale.getInvoiceNumber() != null ? sale.getInvoiceNumber() : "Pending";
        String customerPart = sale.getCustomerId() != null ? String.valueOf(sale.getCustomerId()) : "";

        updateSearchIndex(
                sale.getId(),
                "SALES",
                invoice,
                (invoice + " " + customerPart).toLowerCase(),
                sale.getUserId(),
                "/sales/" + sale.getId(),
                1 // Sales are always active
        );
    }

    // ========================================================================
    // DATABASE UPDATE LOGIC
    // ========================================================================

    private void updateSearchIndex(Integer sourceId, String type, String displayName, String searchText, String userId, String relativeUrl, int isActive) {
        if (jdbcTemplate == null) return;

        // Using MERGE (H2/MySQL compatible upsert)
        String sql = """
            MERGE INTO global_search_index (source_id, source_type, display_name, search_text, user_id, relative_url, source_isactive, last_updated)
            KEY (source_id, source_type)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try {
            jdbcTemplate.update(sql, sourceId, type, displayName, searchText, userId, relativeUrl, isActive, LocalDateTime.now());
        } catch (Exception e) {
            System.err.println("Failed to update search index: " + e.getMessage());
        }
    }
}