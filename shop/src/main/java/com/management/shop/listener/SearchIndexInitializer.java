package com.management.shop.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SearchIndexInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SearchIndexInitializer.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        logger.info("Starting Global Search Index Backfill...");

        try {
            // 1. Backfill CUSTOMERS
            // Inserts only if the customer ID is not already in the search index
            String customerSql = """
                INSERT INTO global_search_index (
                    source_id, source_isactive, source_type, search_text, 
                    display_name, user_id, relative_url, last_updated
                )
                SELECT 
                    sc.id, 
                    IFNULL(sc.is_active, 1), 
                    'CUSTOMER', 
                    LOWER(CONCAT_WS(' ', sc.name, IFNULL(sc.email, ''), IFNULL(sc.phone, ''))), 
                    sc.name, 
                    sc.user_id, 
                    CONCAT('/customers/', sc.id), 
                    NOW()
                FROM shop_customer sc
                LEFT JOIN global_search_index gsi 
                    ON gsi.source_id = sc.id AND gsi.source_type = 'CUSTOMER'
                WHERE gsi.id IS NULL
            """;
            int customersAdded = jdbcTemplate.update(customerSql);
            logger.info("Indexed {} new customers.", customersAdded);

            // 2. Backfill PRODUCTS
            String productSql = """
                INSERT INTO global_search_index (
                    source_id, source_isactive, source_type, search_text, 
                    display_name, user_id, relative_url, last_updated
                )
                SELECT 
                    sp.id, 
                    IFNULL(sp.active, 1), 
                    'PRODUCT', 
                    LOWER(CONCAT_WS(' ', sp.name, IFNULL(sp.category, ''), IFNULL(sp.hsn, ''))), 
                    sp.name, 
                    sp.user_id, 
                    CONCAT('/products/', sp.id), 
                    NOW()
                FROM shop_product sp
                LEFT JOIN global_search_index gsi 
                    ON gsi.source_id = sp.id AND gsi.source_type = 'PRODUCT'
                WHERE gsi.id IS NULL
            """;
            int productsAdded = jdbcTemplate.update(productSql);
            logger.info("Indexed {} new products.", productsAdded);

            // 3. Backfill SALES (Billing Details)
            // Fixed the relative_url to point to /sales/ instead of /products/
            String salesSql = """
                INSERT INTO global_search_index (
                    source_id, source_isactive, source_type, search_text, 
                    display_name, user_id, relative_url, last_updated
                )
                SELECT 
                    bd.id, 
                    1, 
                    'SALES', 
                    LOWER(CONCAT(IFNULL(bd.invoice_number, ''), ' ', IFNULL(bd.customer_id, ''))), 
                    IFNULL(bd.invoice_number, 'Pending'), 
                    bd.user_id, 
                    CONCAT('/sales/', bd.id), 
                    NOW()
                FROM billing_details bd
                LEFT JOIN global_search_index gsi 
                    ON gsi.source_id = bd.id AND gsi.source_type = 'SALES'
                WHERE gsi.id IS NULL
            """;
            int salesAdded = jdbcTemplate.update(salesSql);
            logger.info("Indexed {} new sales records.", salesAdded);

        } catch (Exception e) {
            logger.error("Failed to populate Global Search Index during startup: {}", e.getMessage());
            // We catch the exception so the app doesn't crash if the DB isn't fully ready
        }
    }
}