package com.management.shop.service;

import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.errors.MailjetSocketTimeoutException;
import com.management.shop.dto.AnalyticsRequest;
import com.management.shop.dto.AnalyticsRes;
import com.management.shop.dto.AnalyticsResponse;
import com.management.shop.dto.BillingRequest;
import com.management.shop.dto.BillingResponse;
import com.management.shop.dto.CustomerRequest;
import com.management.shop.dto.CustomerSuccessDTO;
import com.management.shop.dto.DasbboardResponseDTO;
import com.management.shop.dto.GoalData;
import com.management.shop.dto.GoalRequest;
import com.management.shop.dto.InvoiceData;
import com.management.shop.dto.InvoiceDetails;
import com.management.shop.dto.NotificationDTO;
import com.management.shop.dto.NotificationStatusUpdateRequest;
import com.management.shop.dto.OrderItem;
import com.management.shop.dto.PaymentDetails;
import com.management.shop.dto.PieAnalyticsMap;
import com.management.shop.dto.ProductBillDTO;
import com.management.shop.dto.ProductPerformanceProjection;
import com.management.shop.dto.ProductRequest;
import com.management.shop.dto.ProductSearchDto;
import com.management.shop.dto.ProductSuccessDTO;
import com.management.shop.dto.ReportRequest;
import com.management.shop.dto.ReportResponse;
import com.management.shop.dto.SalesResponseDTO;
import com.management.shop.dto.ShopBasicDetailsRequest;
import com.management.shop.dto.ShopFinanceDetailsRequest;
import com.management.shop.dto.ShopInvoiceTerms;
import com.management.shop.dto.ShopNotifications;
import com.management.shop.dto.TopOrdersDto;
import com.management.shop.dto.TopProductDto;
import com.management.shop.dto.UpdateUserDTO;
import com.management.shop.dto.UserProfileDto;
import com.management.shop.dto.WeeklySales;
import com.management.shop.entity.BillingEntity;
import com.management.shop.entity.CustomerEntity;
import com.management.shop.entity.EstimatedGoalsEntity;
import com.management.shop.entity.GlobalSearchIndex;
import com.management.shop.entity.MessageEntity;
import com.management.shop.entity.PaymentEntity;
import com.management.shop.entity.PaymentHistory;
import com.management.shop.entity.ProductEntity;
import com.management.shop.entity.ProductSalesEntity;
import com.management.shop.entity.Report;
import com.management.shop.entity.SelectedInvoiceEntity;
import com.management.shop.entity.ShopBankEntity;
import com.management.shop.entity.ShopBasicEntity;
import com.management.shop.entity.ShopDetailsEntity;
import com.management.shop.entity.ShopFinanceEntity;
import com.management.shop.entity.ShopInvoiceTermsEnity;
import com.management.shop.entity.ShopUPIEntity;
import com.management.shop.entity.UserInfo;
import com.management.shop.entity.UserPaymentModes;
import com.management.shop.entity.UserProfilePicEntity;
import com.management.shop.entity.UserSettingsEntity;
import com.management.shop.repository.BillingGstRepository;
import com.management.shop.repository.BillingRepository;
import com.management.shop.repository.EstimatedGoalRepository;
import com.management.shop.repository.GlobalSearchIndexRepository;
import com.management.shop.repository.NotificationsRepo;
import com.management.shop.repository.ProductRepository;
import com.management.shop.repository.ProductSalesRepository;
import com.management.shop.repository.RegisterUserRepo;
import com.management.shop.repository.ReportDetailsRepo;
import com.management.shop.repository.SalesPaymentRepository;
import com.management.shop.repository.SelectedInvoiceRepository;
import com.management.shop.repository.ShopBankRepository;
import com.management.shop.repository.ShopBasicRepository;
import com.management.shop.repository.ShopDetailsRepo;
import com.management.shop.repository.ShopFinanceRepository;
import com.management.shop.repository.ShopInvoiceTermsRepository;
import com.management.shop.repository.ShopRepository;
import com.management.shop.repository.ShopUPIRepository;
import com.management.shop.repository.UserInfoRepository;
import com.management.shop.repository.UserPaymentModesRepo;
import com.management.shop.repository.UserProfilePicRepo;
import com.management.shop.repository.UserSettingsRepository;
import com.management.shop.scheduler.BillingProcess;
import com.management.shop.service.SalesCacheService;
import com.management.shop.util.CSVUpload;
import com.management.shop.util.CSVUtil;
import com.management.shop.util.EmailSender;
import com.management.shop.util.OTPSender;
import com.management.shop.util.OrderEmailTemplate;
import com.management.shop.util.PDFGSTInvoiceUtil;
import com.management.shop.util.PDFInvoiceUtil;
import com.management.shop.util.ReportsGenerate;
import com.management.shop.util.Utility;
import jakarta.transaction.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ShopService {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(com.management.shop.service.ShopService.class);

    @Autowired
    private Environment environment;

    @Autowired
    private UserInfoRepository userinfoRepo;

    @Autowired
    private ShopRepository shopRepo;

    @Autowired
    private ProductRepository prodRepo;

    @Autowired
    private BillingRepository billRepo;

    @Autowired
    private ProductSalesRepository prodSalesRepo;

    @Autowired
    private SalesPaymentRepository salesPaymentRepo;

    @Autowired
    private ReportDetailsRepo reportDRepo;

    @Autowired
    private ShopDetailsRepo shopDetailsRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserProfilePicRepo userProfilePicRepo;

    @Autowired
    private RegisterUserRepo newUserRepo;

    @Autowired
    private UserPaymentModesRepo paymentModesRepo;

    @Autowired
    private EstimatedGoalRepository estimatedGoalsRepo;

    @Autowired
    private ShopBasicRepository shopBasicRepo;

    @Autowired
    private ShopFinanceRepository shopFinanceRepo;

    @Autowired
    private ShopBankRepository shopBankRepo;

    @Autowired
    private ShopUPIRepository salesUPIRepo;

    @Autowired
    private ShopInvoiceTermsRepository shopInvoiceTermsRepo;

    @Autowired
    private BillingGstRepository billGstRepo;

    @Autowired
    private UserSettingsRepository userSettingsRepo;

    @Autowired
    CSVUpload util;

    @Autowired
    ReportsGenerate repogen;

    @Autowired
    PDFInvoiceUtil pdfutil;

    @Autowired
    PDFGSTInvoiceUtil pdfgstutil;

    @Autowired
    EmailSender email;

    @Autowired
    private OTPSender otpSender;

    @Autowired
    OrderEmailTemplate emailTemplate;

    @Autowired
    SalesCacheService salesCacheService;

    @Autowired
    private NotificationsRepo notiRepo;

    @Autowired
    BillingProcess billingProcess;

    @Autowired
    SelectedInvoiceRepository invoiceRepo;

    @Autowired
    Utility utils;

    @Autowired
    GlobalSearchIndexRepository globalSearchRepo;

    @Autowired
    CSVUtil csvutil;

    private final Random random = new Random();

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${app.username}")
    private String appUsername;

    public String extractUsername(String orderReferenceNumber) {
        String username = "";
        try {
            username = SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            BillingEntity billDetails = this.billRepo.findOrderByJustReference(orderReferenceNumber);
            username = billDetails.getUserId();
        }
        return username;
    }

    public String extractUsername() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return username;
    }

    public List<String> extractRoles() {
        List<String> roles = (List<String>) SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());
        System.out.println("Current user roles: " + roles);
        return roles;
    }

    private static <T, R> R getIfNotNull(T source, Function<T, R> getter) {
        return (source != null) ? getter.apply(source) : null;
    }

    public boolean checkUserStatus(String username) {
        return ((UserInfo) this.userinfoRepo.findByUsername(username).get()).getIsActive().booleanValue();
    }

    public CustomerSuccessDTO saveCustomer(CustomerRequest request) {
        System.out.println("entered into saveCustomer with" + request.toString());
        List<CustomerEntity> existingCustomer = new ArrayList<>();
        if (!request.getPhone().equals("") && request.getPhone() != null && !request.getPhone().equals("0000000000"))
            existingCustomer = this.shopRepo.findByPhone(request.getPhone(), "ACTIVE", extractUsername());
        CustomerEntity ent = null;
        if (existingCustomer.size() > 0) {
            CustomerEntity customerEntity = CustomerEntity.builder().userId(extractUsername()).id(((CustomerEntity) existingCustomer.get(0)).getId()).name(request.getName()).email(request.getEmail()).createdDate(LocalDateTime.now()).gstNumber(request.getGstNumber()).isActive(Boolean.TRUE).state(request.getCustomerState()).city(request.getCity()).phone(request.getPhone()).status("ACTIVE").totalSpent(((CustomerEntity) existingCustomer.get(0)).getTotalSpent()).build();
            ent = (CustomerEntity) this.shopRepo.save(customerEntity);
        } else {
            CustomerEntity customerEntity = CustomerEntity.builder().userId(extractUsername()).name(request.getName()).email(request.getEmail()).createdDate(LocalDateTime.now()).state(request.getCustomerState()).gstNumber(request.getGstNumber()).city(request.getCity()).isActive(Boolean.TRUE).phone(request.getPhone()).status("ACTIVE").totalSpent(Double.valueOf(0.0D)).build();
            ent = (CustomerEntity) this.shopRepo.save(customerEntity);
        }
        this.salesCacheService.evictUserCustomers(extractUsername());
        this.salesCacheService.evictsUserAnalytics(extractUsername());
        if (ent.getId() != null) {
            try {
                this.salesCacheService.evictUserCustomers(extractUsername());
                this.salesCacheService.evictsUserAnalytics(extractUsername());
                this.salesCacheService.evictsReportsCache(extractUsername());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return CustomerSuccessDTO.builder().success(Boolean.valueOf(true)).customer(request).build();
        }
        return CustomerSuccessDTO.builder().id(ent.getId()).success(Boolean.valueOf(false)).customer(request).build();
    }

    @CacheEvict(value = {"customers"}, key = "#root.target.extractUsername()")
    public CustomerEntity saveCustomerForBilling(CustomerRequest request) {
        System.out.println("entered into saveCustomer with" + request.toString());
        List<CustomerEntity> existingCustomer = new ArrayList<>();


        if (request.getPhone() != null)
            existingCustomer = this.shopRepo.findByPhone(request.getPhone(), "ACTIVE", extractUsername());
        CustomerEntity ent = null;
        if (existingCustomer.size() > 0) {
            CustomerEntity customerEntity = CustomerEntity.builder().id(((CustomerEntity) existingCustomer.get(0)).getId()).userId(extractUsername()).name(request.getName()).email(request.getEmail()).state(request.getCustomerState()).gstNumber(request.getGstNumber()).city(request.getCity()).createdDate(LocalDateTime.now()).phone(request.getPhone()).status("ACTIVE").isActive(Boolean.TRUE).totalSpent(((CustomerEntity) existingCustomer.get(0)).getTotalSpent()).build();
            ent = (CustomerEntity) this.shopRepo.save(customerEntity);
        } else {
            CustomerEntity customerEntity = CustomerEntity.builder().name(request.getName()).userId(extractUsername()).email(request.getEmail()).state(request.getCustomerState()).gstNumber(request.getGstNumber()).city(request.getCity()).createdDate(LocalDateTime.now()).phone(request.getPhone()).status("ACTIVE").isActive(Boolean.TRUE).totalSpent(Double.valueOf(0.0D)).build();
            ent = (CustomerEntity) this.shopRepo.save(customerEntity);
        }
        if (ent.getId() != null) {
            try {
                this.salesCacheService.evictUserCustomers(extractUsername());
                this.salesCacheService.evictsUserAnalytics(extractUsername());
                this.salesCacheService.evictsReportsCache(extractUsername());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return ent;
        }
        return ent;
    }

    @Cacheable(value = {"customers"}, key = "#root.target.extractUsername()")
    public List<CustomerEntity> getAllCustomer() {
        System.out.println("The extracted username is " + extractUsername());
        return this.shopRepo.findAllActiveCustomer("ACTIVE", extractUsername());
    }

    @Transactional
    public ProductSuccessDTO saveProduct(ProductRequest request) {
        String status = "In Stock";
        if (request.getStock().intValue() < 0)
            status = "Out of Stock";
        System.out.println("The new request" + request.getTax());
        ProductEntity productEntity = null;
        if (request.getSelectedProductId() != null && request.getSelectedProductId().intValue() != 0) {
            productEntity = ProductEntity.builder().id(request.getSelectedProductId()).name((request.getName() == null) ? "" : request.getName()).category((request.getCategory() == null) ? "" : request.getCategory()).status(status).userId(extractUsername()).stock(request.getStock()).active(Boolean.valueOf(true)).taxPercent(request.getTax()).price(request.getPrice()).costPrice(request.getCostPrice()).hsn((request.getHsn() == null) ? "" : request.getHsn()).updatedDate(LocalDateTime.now()).updatedBy(extractUsername()).build();
        } else {
            productEntity = ProductEntity.builder().name((request.getName() == null) ? "" : request.getName()).userId(extractUsername()).category((request.getCategory() == null) ? "" : request.getCategory()).active(Boolean.valueOf(true)).status(status).stock(request.getStock()).taxPercent(request.getTax()).costPrice(request.getCostPrice()).price(request.getPrice()).hsn((request.getHsn() == null) ? "" : request.getHsn()).createdDate(LocalDateTime.now()).updatedDate(LocalDateTime.now()).updatedBy(extractUsername()).build();
        }
        ProductEntity ent = (ProductEntity) this.prodRepo.save(productEntity);
        if (ent.getId() != null) {
            try {
                this.salesCacheService.evictUserProducts(extractUsername());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return ProductSuccessDTO.builder().success(Boolean.valueOf(true)).product(request).build();
        }
        return ProductSuccessDTO.builder().success(Boolean.valueOf(false)).product(request).build();
    }

    public ProductSuccessDTO updateProduct(ProductRequest request) {
        String status = "In Stock";
        if (request.getStock().intValue() < 1)
            status = "Out of Stock";
        System.out.println("The updated request" + request.getTax());
        ProductEntity productEntity = ProductEntity.builder().id(request.getSelectedProductId()).name(request.getName()).active(Boolean.valueOf(true)).category(request.getCategory()).userId(extractUsername()).status(status).stock(request.getStock()).hsn(request.getHsn()).taxPercent(request.getTax()).price(request.getPrice()).costPrice(request.getCostPrice()).build();
        ProductEntity ent = (ProductEntity) this.prodRepo.save(productEntity);
        if (ent.getId() != null) {
            try {
                this.salesCacheService.evictUserProducts(extractUsername());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return ProductSuccessDTO.builder().success(Boolean.valueOf(true)).product(request).build();
        }
        return ProductSuccessDTO.builder().success(Boolean.valueOf(false)).product(request).build();
    }

    public List<ProductEntity> getAllProducts() {
        return this.prodRepo.findAllActiveProducts(Boolean.TRUE, extractUsername());
    }

    public byte[] exportAllProductAsCSV() {
        List<ProductEntity> productList = this.prodRepo.findAllActiveProducts(Boolean.TRUE, extractUsername());
        try {
            return this.csvutil.exportAllProductAsCSV(productList);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Cacheable(value = {"products"}, keyGenerator = "userScopedKeyGenerator")
    public Page<ProductEntity> getAllProducts(String search, int page, int limit, String sort, String dir) {
        String sortField = sort;
        if ("createdAt".equalsIgnoreCase(sortField))
            sortField = "created_date";
        if ("tax".equalsIgnoreCase(sortField))
            sortField = "tax_percent";
        if ("costPrice".equalsIgnoreCase(sortField))
            sortField = "cost_price";
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortOrder = Sort.by(direction, new String[]{sortField});
        PageRequest pageRequest = PageRequest.of(page - 1, limit, sortOrder);
        String username = extractUsername();
        return this.prodRepo.findAllActiveProductsWithPagination(Boolean.TRUE, username, search, (Pageable) pageRequest);
    }

    @Cacheable(value = {"products"}, keyGenerator = "userScopedKeyGenerator")
    public Page<ProductEntity> getAllProductsForBilling(String search, int page, int limit, String sort, String dir) {
        String sortField = sort;
        if ("createdAt".equalsIgnoreCase(sortField))
            sortField = "created_date";
        if ("tax".equalsIgnoreCase(sortField))
            sortField = "tax_percent";
        if ("costPrice".equalsIgnoreCase(sortField))
            sortField = "cost_price";
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortOrder = Sort.by(direction, new String[]{sortField});
        PageRequest pageRequest = PageRequest.of(page - 1, limit, sortOrder);
        String username = extractUsername();
        return this.prodRepo.findAllActiveProductsWithPaginationForBilling(Boolean.TRUE, username, search, (Pageable) pageRequest);
    }

    @Cacheable(value = {"customers"}, keyGenerator = "userScopedKeyGenerator")
    public Page<CustomerEntity> getCacheableCustomersList(String search, int page, int size, String sort, String dir) {
        String sortField = sort;
        if ("createdAt".equalsIgnoreCase(sortField))
            sortField = "created_date";
        if ("createdDate".equalsIgnoreCase(sortField))
            sortField = "created_date";
        if ("totalSpent".equalsIgnoreCase(sortField))
            sortField = "total_spent";
        Sort.Direction direction = dir.equalsIgnoreCase("desc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortOrder = Sort.by(direction, new String[]{sortField});
        PageRequest pageRequest = PageRequest.of(page - 1, size, sortOrder);
        String username = extractUsername();
        Page<CustomerEntity> response = null;
        try {
            response = this.shopRepo.findAllCustomersWithPagination(username, search, (Pageable) pageRequest, Boolean.TRUE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    public Page<CustomerEntity> getBillingCustomersList(String search, int page, int size, String sort) {
        String sortField = sort;
        if ("createdAt".equalsIgnoreCase(sortField))
            sortField = "created_date";
        if ("createdDate".equalsIgnoreCase(sortField))
            sortField = "created_date";
        if ("totalSpent".equalsIgnoreCase(sortField))
            sortField = "total_spent";
        Sort.Direction direction = Sort.Direction.DESC;
        Sort sortOrder = Sort.by(direction, new String[]{sortField});
        PageRequest pageRequest = PageRequest.of(page - 1, size, sortOrder);
        String username = extractUsername();
        Page<CustomerEntity> response = null;
        try {
            response = this.shopRepo.findAllCustomersWithPagination(username, search, (Pageable) pageRequest, Boolean.TRUE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    @Transactional
    public BillingResponse doPayment(BillingRequest request) throws Exception {
        Integer unitsSold = Integer.valueOf(0);
        for (ProductBillDTO obj : request.getCart())
            unitsSold = Integer.valueOf(unitsSold.intValue() + obj.getQuantity().intValue());
        UserSettingsEntity userSettingsEntity = this.userSettingsRepo.findByUsername(extractUsername());
        Boolean sendInvoice = Boolean.valueOf(true);
        BillingEntity billingEntity = BillingEntity.builder().customerId(request.getSelectedCustomer().getId()).unitsSold(unitsSold).taxAmount(request.getTax()).userId(extractUsername()).totalAmount(request.getTotal()).payingAmount(request.getPayingAmount()).gstin(request.getGstin()).dueReminderCount(Integer.valueOf(0)).remainingAmount(request.getRemainingAmount()).discountPercent(request.getDiscountPercentage()).remarks(request.getRemarks()).subTotalAmount(Double.valueOf(request.getTotal().doubleValue() - request.getTax().doubleValue())).createdDate(LocalDateTime.now()).build();
        BillingEntity billResponse = (BillingEntity) this.billRepo.save(billingEntity);
        if (userSettingsEntity != null) {
            sendInvoice = userSettingsEntity.getAutoSendInvoice();
            String orderPrefix = userSettingsEntity.getSerialNumberPattern();
            String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String sequentialPart = String.format("%04d", new Object[]{billResponse.getId()});
            String invoiceNumber = "FMS-" + datePart + "-" + sequentialPart;
            if (orderPrefix != null) {
                invoiceNumber = orderPrefix + "-" + datePart + "-" + sequentialPart;
                billResponse.setInvoiceNumber(invoiceNumber);
                this.billRepo.save(billResponse);
            } else {
                billResponse.setInvoiceNumber(invoiceNumber);
                this.billRepo.save(billResponse);
            }
        }
        Double[] totalProfitOnCP = {Double.valueOf(0.0D)};
        if (billResponse.getId() != null) {
            request.getCart().stream().forEach(obj -> {
                ProductEntity prodRes = this.prodRepo.findByIdAndUserId(obj.getId(), extractUsername());
                System.out.println("Product details " + prodRes);
                Double tax = Double.valueOf((prodRes.getTaxPercent().intValue() * obj.getQuantity().intValue()) * obj.getPrice().doubleValue() / 100.0D);
                Double discountedTotal = Double.valueOf(0.0D);
                if (obj.getDiscountPercentage().doubleValue() != 0.0D) {
                    discountedTotal = Double.valueOf(obj.getPrice().doubleValue() - obj.getDiscountPercentage().doubleValue() * obj.getPrice().doubleValue() / 100.0D);
                    obj.setPrice(discountedTotal);
                } else {
                    discountedTotal = Double.valueOf(obj.getPrice().doubleValue());
                }
                Double total = Double.valueOf((obj.getQuantity().intValue() * Math.round(discountedTotal.doubleValue())));
                Double profitOnCp = Double.valueOf((discountedTotal.doubleValue() - prodRes.getCostPrice().intValue()) * obj.getQuantity().intValue());
                totalProfitOnCP[0] = Double.valueOf(totalProfitOnCP[0].doubleValue() + Math.round(profitOnCp.doubleValue()));
                ProductSalesEntity gstCalc = getGSTBreakDown(request.getSelectedCustomer(), obj, prodRes, extractUsername());
                ProductSalesEntity productSalesEntity = ProductSalesEntity.builder().billingId(billResponse.getId()).profitOnCP(profitOnCp).sgstPercentage(gstCalc.getSgstPercentage()).sgst(gstCalc.getSgst()).cgstPercentage(gstCalc.getCgstPercentage()).cgst(gstCalc.getCgst()).igstPercentage(gstCalc.getIgstPercentage()).igst(gstCalc.getIgst()).productId(obj.getId()).productDetails(obj.getDetails()).userId(extractUsername()).discountPercentage(obj.getDiscountPercentage()).quantity(obj.getQuantity()).tax(gstCalc.getTax()).subTotal(gstCalc.getSubTotal()).total(total).updatedAt(LocalDateTime.now()).build();
                ProductSalesEntity prodSalesResponse = (ProductSalesEntity) this.prodSalesRepo.save(productSalesEntity);
                try {
                    String str = this.billingProcess.saveGstListing(billResponse.getInvoiceNumber(), extractUsername());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                if (prodSalesResponse.getId() != null)
                    this.prodRepo.updateProductStock(obj.getId(), obj.getQuantity(), extractUsername());
            });
            billResponse.setTotalProfitOnCP(totalProfitOnCP[0]);
            Runnable rn = () -> this.billRepo.save(billResponse);
            rn.run();
            String paymentMethod = "CASH";
            if (request.getPaymentMethod() != null)
                paymentMethod = request.getPaymentMethod();
            String payingStatus = "Paid";
            if (request.getTotal().doubleValue() > request.getPayingAmount().doubleValue())
                payingStatus = "SemiPaid";
            if (request.getPayingAmount().doubleValue() == 0.0D)
                payingStatus = "UnPaid";
            PaymentEntity paymentEntity = PaymentEntity.builder().billingId(billResponse.getId()).createdDate(LocalDateTime.now()).paymentMethod(paymentMethod).status(payingStatus).tax(request.getTax()).userId(extractUsername()).orderNumber(billResponse.getInvoiceNumber()).paid(request.getPayingAmount()).toBePaid(request.getRemainingAmount()).reminderCount(Integer.valueOf(0)).subtotal(Double.valueOf(request.getTotal().doubleValue() - request.getTax().doubleValue())).total(request.getTotal()).build();
            this.salesPaymentRepo.save(paymentEntity);
            try {
                String str = this.utils.asyncSavePaymentHistory(billResponse.getId(), paymentEntity.getId(), request.getPayingAmount(), billResponse.getInvoiceNumber());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            try {
                this.shopRepo.updateCustomerSpentAmount(request.getSelectedCustomer().getId(), request.getTotal(), extractUsername());
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (sendInvoice.booleanValue()) {
                InvoiceDetails order = getOrderDetails(billResponse.getInvoiceNumber());
                try {
                    Map<String, Object> emailContent = this.emailTemplate.generateOrderHtml(order, extractUsername());
                    CompletableFuture<String> futureResult = this.email.sendEmail(order.getCustomerEmail(), billResponse
                                    .getInvoiceNumber(), order.getCustomerName(),
                            generateGSTInvoicePdf(billResponse.getInvoiceNumber()), (String) emailContent.get("htmlTemplate"), (String) emailContent.get("shopName"));
                    System.out.println(futureResult);
                } catch (MailjetException | MailjetSocketTimeoutException e) {
                    e.printStackTrace();
                }
            }
            try {
                this.salesCacheService.evictUserSales(extractUsername());
                this.salesCacheService.evictUserProducts(extractUsername());
                this.salesCacheService.evictUserPayments(extractUsername());
                this.salesCacheService.evictUserCustomers(extractUsername());
                this.salesCacheService.evictUserDasbhoard(extractUsername());
                this.salesCacheService.evictsUserGoals(extractUsername());
                this.salesCacheService.evictsUserAnalytics(extractUsername());
                this.salesCacheService.evictsTopSelling(extractUsername());
                this.salesCacheService.evictsTopOrders(extractUsername());
                this.salesCacheService.evictsReportsCache(extractUsername());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return BillingResponse.builder().paymentReferenceNumber(paymentEntity.getPaymentReferenceNumber())
                    .invoiceNumber(billResponse.getInvoiceNumber()).status("SUCCESS").build();
        }
        return BillingResponse.builder().status("FAILURE").build();
    }

    private ProductSalesEntity getGSTBreakDown(CustomerEntity selectedCustomer, ProductBillDTO obj, ProductEntity prodRes, String username) {
        String customerState = selectedCustomer.getState();
        String shopState = this.shopBasicRepo.findByUserId(username).getShopState();
        double taxPercent = prodRes.getTaxPercent().intValue();
        double qty = obj.getQuantity().intValue();
        double price = obj.getPrice().doubleValue();
        double basePrice = price / (1.0D + taxPercent / 100.0D);
        double totalTax = price - basePrice;
        double cgst = 0.0D, sgst = 0.0D, igst = 0.0D;
        double cgstPercent = 0.0D, sgstPercent = 0.0D, igstPercent = 0.0D;
        if (customerState.equals(shopState)) {
            cgst = totalTax / 2.0D;
            sgst = totalTax / 2.0D;
            cgstPercent = taxPercent / 2.0D;
            sgstPercent = taxPercent / 2.0D;
        } else {
            igst = totalTax;
            igstPercent = taxPercent;
        }
        basePrice *= qty;
        cgst *= qty;
        sgst *= qty;
        igst *= qty;
        totalTax *= qty;
        return ProductSalesEntity.builder()
                .cgstPercentage(Integer.valueOf((int) Math.round(cgstPercent)))
                .cgst(Double.valueOf(round2(cgst)))
                .sgstPercentage(Integer.valueOf((int) Math.round(sgstPercent)))
                .sgst(Double.valueOf(round2(sgst)))
                .igstPercentage(Integer.valueOf((int) Math.round(igstPercent)))
                .igst(Double.valueOf(round2(igst)))
                .tax(Double.valueOf(round2(totalTax)))
                .subTotal(Double.valueOf(round2(basePrice)))
                .build();
    }

    private static double round2(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }

    @Cacheable(value = {"sales"}, keyGenerator = "userScopedKeyGenerator")
    public Page<SalesResponseDTO> getAllSales(int page, int size, String sort, String dir, String searchTerm) {
        String username = extractUsername();
        String sortField = sort;
        if ("date".equalsIgnoreCase(sortField))
            sortField = "created_date";
        if ("id".equalsIgnoreCase(sortField))
            sortField = "invoice_number";
        if ("totalAmount".equalsIgnoreCase(sortField))
            sortField = "total_amount";
        if ("customer".equalsIgnoreCase(sortField))
            sortField = "customer_id";
        if ("paid".equalsIgnoreCase(sortField))
            sortField = "paying_amount";
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortOrder = Sort.by(direction, new String[]{sortField});
        PageRequest pageRequest = PageRequest.of(Math.max(0, page - 1), size, sortOrder);
        Page<BillingEntity> billingPage = null;
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            try {
                billingPage = this.billRepo.findByUserIdAndSearchNative(username, searchTerm.trim(), (Pageable) pageRequest);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            billingPage = this.billRepo.findAllByUserId(username, (Pageable) pageRequest);
        }
        List<SalesResponseDTO> dtoList = billingPage.getContent().stream().map(obj -> {
            String customerName = this.shopRepo.findByIdAndUserId(obj.getCustomerId(), username).getName();
            String paymentStatus = this.salesPaymentRepo.findPaymentDetails(obj.getId(), username).getStatus();
            return SalesResponseDTO.builder().customer(customerName).remarks(obj.getRemarks()).date(obj.getCreatedDate().toString()).id(obj.getInvoiceNumber()).total(obj.getTotalAmount()).paid(obj.getPayingAmount()).status(paymentStatus).gstin(obj.getGstin()).reminderCount(obj.getDueReminderCount()).build();
        }).toList();
        return (Page<SalesResponseDTO>) new PageImpl(dtoList, (Pageable) pageRequest, billingPage.getTotalElements());
    }

    @Cacheable(value = {"sales"}, keyGenerator = "userScopedKeyGenerator")
    public List<SalesResponseDTO> getLastNSales(int count) {
        String username = extractUsername();
        List<BillingEntity> billingDetails = this.billRepo.findNNumberWithUserId(username, count);
        List<SalesResponseDTO> dtoList = billingDetails.stream().map(obj -> {
            String customerName = this.shopRepo.findByIdAndUserId(obj.getCustomerId(), username).getName();
            String paymentStatus = this.salesPaymentRepo.findPaymentDetails(obj.getId(), username).getStatus();
            return SalesResponseDTO.builder().customer(customerName).remarks(obj.getRemarks()).date(obj.getCreatedDate().toString()).id(obj.getInvoiceNumber()).total(obj.getTotalAmount()).status(paymentStatus).build();
        }).toList();
        return dtoList;
    }

    @Cacheable(value = {"sales"}, keyGenerator = "userScopedKeyGenerator")
    public Page<SalesResponseDTO> getAllSalesWithPagination(Integer page, Integer size, String sort, String dir) {
        String sortField = sort;
        if ("createdAt".equalsIgnoreCase(sortField))
            sortField = "created_date";
        if ("total".equalsIgnoreCase(sortField))
            sortField = "total_amount";
        if ("invoiceNumber".equalsIgnoreCase(sortField) || "invoice".equalsIgnoreCase(sortField))
            sortField = "invoice_number";
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortOrder = Sort.by(direction, new String[]{sortField});
        PageRequest pageRequest = PageRequest.of(Math.max(0, page.intValue() - 1), size.intValue(), sortOrder);
        String username = extractUsername();
        Page<BillingEntity> billingPage = this.billRepo.findAllByUserId(username, (Pageable) pageRequest);
        List<SalesResponseDTO> dtoList = billingPage.getContent().stream().map(obj -> {
            String customerName = this.shopRepo.findByIdAndUserId(obj.getCustomerId(), username).getName();
            String paymentStatus = this.salesPaymentRepo.findPaymentDetails(obj.getId(), username).getStatus();
            return SalesResponseDTO.builder().customer(customerName).remarks(obj.getRemarks()).date(obj.getCreatedDate().toString()).id(obj.getInvoiceNumber()).total(obj.getTotalAmount()).gstin(obj.getGstin()).status(paymentStatus).build();
        }).toList();
        return (Page<SalesResponseDTO>) new PageImpl(dtoList, (Pageable) pageRequest, billingPage.getTotalElements());
    }

    @Cacheable(value = {"dashboard"}, keyGenerator = "userScopedKeyGenerator")
    public DasbboardResponseDTO getDashBoardDetails(String range) {
        System.out.println("selected day range" + range);
        List<BillingEntity> billList = new ArrayList<>();
        List<ProductEntity> prodList = new ArrayList<>();
        List<String> roles = extractRoles();
        System.out.println("The user roles" + roles);
        Integer days = Integer.valueOf(0);
        if (!range.equals("today")) {
            if (range.equals("lastYear"))
                days = Integer.valueOf(365);
            if (range.equals("lastMonth"))
                days = Integer.valueOf(30);
            if (range.equals("lastWeek"))
                days = Integer.valueOf(7);
            billList = this.billRepo.findAllByDayRange(LocalDateTime.now().minusDays(days.intValue()), extractUsername());
        } else if (range.equals("today")) {
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            LocalDateTime endOfDay = startOfDay.plusDays(1L);
            billList = this.billRepo.findAllCreatedToday(startOfDay, endOfDay, extractUsername());
        }
        prodList = this.prodRepo.findAllByStatus(Boolean.TRUE, extractUsername());
        Integer monthlyRevenue = Integer.valueOf(0);
        Integer taxCollected = Integer.valueOf(0);
        Integer totalUnitsSold = Integer.valueOf(0);
        Integer outOfStockCount = Integer.valueOf(0);
        Integer countOfOrders = Integer.valueOf(0);
        for (BillingEntity obj : billList) {
            monthlyRevenue = Integer.valueOf((int) (monthlyRevenue.intValue() + obj.getTotalAmount().doubleValue()));
            taxCollected = Integer.valueOf((int) (taxCollected.intValue() + obj.getTaxAmount().doubleValue()));
            totalUnitsSold = Integer.valueOf(totalUnitsSold.intValue() + obj.getUnitsSold().intValue());
            countOfOrders = Integer.valueOf(countOfOrders.intValue() + 1);
        }
        for (ProductEntity obj : prodList) {
            if (obj.getStock().intValue() < 1)
                outOfStockCount = Integer.valueOf(outOfStockCount.intValue() + 1);
        }
        return DasbboardResponseDTO.builder().monthlyRevenue(monthlyRevenue).outOfStockCount(outOfStockCount)
                .taxCollected(taxCollected).totalUnitsSold(totalUnitsSold).countOfSales(countOfOrders).build();
    }

    @Cacheable(value = {"payments"}, keyGenerator = "userScopedKeyGenerator")
    public List<PaymentDetails> getPaymentList(String fromDate, String toDate) {
        LocalDateTime startDate = LocalDate.parse(fromDate).atStartOfDay();
        LocalDateTime endDate = LocalDate.parse(toDate).atTime(LocalTime.MAX);
        List<PaymentEntity> paymentList = this.salesPaymentRepo.getPaymentList(startDate, endDate, extractUsername());
        paymentList.sort(Comparator.<PaymentEntity, Comparable>comparing(PaymentEntity::getCreatedDate).reversed());
        List<PaymentDetails> response = new ArrayList<>();
        paymentList.stream().forEach(obj -> response.add(PaymentDetails.builder().id(obj.getPaymentReferenceNumber()).amount(obj.getTotal()).date(String.valueOf(obj.getCreatedDate())).saleId(obj.getOrderNumber()).reminderCount(obj.getReminderCount()).method(obj.getPaymentMethod()).paid(Double.valueOf((obj.getPaid() != null) ? obj.getPaid().doubleValue() : 0.0D)).due(Double.valueOf((obj.getToBePaid() != null) ? obj.getToBePaid().doubleValue() : 0.0D)).status(obj.getStatus()).build()));
        return response;
    }

    public ProductSuccessDTO uploadProduct(File request) {
        return null;
    }

    public List<ProductRequest> uploadBulkProduct(MultipartFile file) {
        try {
            List<ProductRequest> prodList = this.util.parseCsv(file);
            System.out.println(prodList);
            prodList.stream().forEach(obj -> {
                ProductSuccessDTO prodsaveResponse = saveProduct(obj);
                System.out.println(prodsaveResponse);
            });
            try {
                this.salesCacheService.evictUserProducts(extractUsername());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return prodList;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public InvoiceDetails getOrderDetails(String orderReferenceNumber) {
        String username = "";
        if (orderReferenceNumber != null) {
            BillingEntity billingEntity = this.billRepo.findOrderByJustReference(orderReferenceNumber);
            username = billingEntity.getUserId();
        }
        BillingEntity billDetails = this.billRepo.findOrderByReference(orderReferenceNumber, username);
        PaymentEntity paymentEntity = this.salesPaymentRepo.findPaymentDetails(billDetails.getId(), username);
        boolean paid = false;
        if (paymentEntity.getStatus().equalsIgnoreCase("Paid"))
            paid = true;
        CustomerEntity customerEntity = this.shopRepo.findByIdAndUserId(billDetails.getCustomerId(), username);
        List<ProductSalesEntity> prodSales = this.prodSalesRepo.findByOrderId(billDetails.getId(), username);
        Double gst = Double.valueOf(0.0D);
        for (ProductSalesEntity orders : prodSales)
            gst = Double.valueOf(gst.doubleValue() + orders.getTax().doubleValue());
        List<OrderItem> items = (List<OrderItem>) prodSales.stream().map(obj -> {
            String username2 = "";
            if (orderReferenceNumber != null) {
                BillingEntity billDetails2 = this.billRepo.findOrderByJustReference(orderReferenceNumber);
                username2 = billDetails.getUserId();
            }
            System.out.println("The productId is " + obj.getProductId());
            ProductEntity prodRes = this.prodRepo.findByIdAndUserId(obj.getProductId(), username2);
            return OrderItem.builder().productName(prodRes.getName()).unitPrice(obj.getTotal().doubleValue()).gst(obj.getTax().doubleValue()).sgst(obj.getSgst().doubleValue()).sgstPercentage(obj.getSgstPercentage()).cgst(obj.getCgst().doubleValue()).cgstPercentage(obj.getCgstPercentage()).igst(obj.getIgst().doubleValue()).igstPercentage(obj.getIgstPercentage()).details(obj.getProductDetails()).discount(obj.getDiscountPercentage()).quantity(obj.getQuantity().intValue()).build();
        }).collect(Collectors.toList());
        InvoiceDetails response = InvoiceDetails.builder().discountRate(billDetails.getDiscountPercent().doubleValue()).invoiceId(orderReferenceNumber).paymentReferenceNumber(paymentEntity.getPaymentReferenceNumber()).items(items).gstRate(gst.doubleValue()).gstNumber(billDetails.getGstin()).customerPhone(customerEntity.getPhone()).customerEmail(customerEntity.getEmail()).orderedDate(String.valueOf(billDetails.getCreatedDate()).substring(0, 10)).totalAmount(billDetails.getTotalAmount().doubleValue()).customerName(customerEntity.getName()).paid(paid).build();
        return response;
    }

    public InvoiceDetails getOrderDetailsNew(String orderReferenceNumber) {
        BillingEntity billDetails = this.billRepo.findOrderByReference(orderReferenceNumber, extractUsername());
        PaymentEntity paymentEntity = this.salesPaymentRepo.findPaymentDetails(billDetails.getId(), extractUsername());
        boolean paid = false;
        if (paymentEntity.getStatus().equalsIgnoreCase("Paid"))
            paid = true;
        CustomerEntity customerEntity = this.shopRepo.findByIdAndUserId(billDetails.getCustomerId(), extractUsername());
        List<ProductSalesEntity> prodSales = this.prodSalesRepo.findByOrderId(billDetails.getId(), extractUsername());
        Double gst = Double.valueOf(0.0D);
        for (ProductSalesEntity orders : prodSales)
            gst = Double.valueOf(gst.doubleValue() + orders.getTax().doubleValue());
        List<OrderItem> items = (List<OrderItem>) prodSales.stream().map(obj -> {
            System.out.println("The productId is " + obj.getProductId());
            ProductEntity prodRes = this.prodRepo.findByIdAndUserId(obj.getProductId(), extractUsername());
            return OrderItem.builder().productName(prodRes.getName()).unitPrice(obj.getTotal().doubleValue()).gst(obj.getTax().doubleValue()).details(obj.getProductDetails()).quantity(obj.getQuantity().intValue()).build();
        }).collect(Collectors.toList());
        InvoiceDetails response = InvoiceDetails.builder().discountRate(0.0D).invoiceId(orderReferenceNumber).paymentReferenceNumber(paymentEntity.getPaymentReferenceNumber()).items(items).gstRate(gst.doubleValue()).customerPhone(customerEntity.getPhone()).customerEmail(customerEntity.getEmail()).orderedDate(String.valueOf(billDetails.getCreatedDate()).substring(0, 10)).totalAmount(billDetails.getTotalAmount().doubleValue()).customerName(customerEntity.getName()).paid(paid).build();
        return response;
    }

    @Cacheable(value = {"reports"}, keyGenerator = "userScopedKeyGenerator")
    public byte[] generateReport(ReportRequest request) {
        LocalDate fromDate = LocalDate.parse(request.getFromDate());
        LocalDateTime fromDateTime = fromDate.atStartOfDay();
        LocalDate toDate = LocalDate.parse(request.getToDate());
        LocalDateTime toDateTime = toDate.atTime(LocalTime.MAX);
        System.out.println(toDateTime);
        byte[] fileBytes = null;
        try {
            fileBytes = this.repogen.downloadReport(request.getReportType(), request.getFormat(), fromDateTime, toDateTime, extractUsername());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return fileBytes;
    }

    public String saveReportDetails(Report request) {
        request.setStatus("READY");
        request.setUserId(extractUsername());
        this.reportDRepo.save(request);
        return "Success";
    }

    public List<ReportResponse> getReportsList(Integer limit) {
        List<Report> reportList = this.reportDRepo.findByLimit(limit, extractUsername());
        return (List<ReportResponse>) reportList.stream().map(obj -> ReportResponse.builder().name(obj.getName()).createdAt(obj.getCreatedAt()).fileName(obj.getFileName()).fromDate(obj.getFromDate()).toDate(obj.getToDate()).id(obj.getId()).status(obj.getStatus()).build())

                .collect(Collectors.toList());
    }

    public String updatePassword(UserInfo userInfo) {
        System.out.println("entered updatePassword with request " + userInfo);
        if (userInfo.getUsername() == null)
            userInfo.setUsername(extractUsername());
        System.out.println("updated updatePassword with request " + userInfo);
        UserInfo userRes = this.userinfoRepo.findByUsername(userInfo.getUsername()).get();
        userRes.setPassword(this.passwordEncoder.encode(userInfo.getPassword()));
        userRes.setUpdatedAt(LocalDateTime.now());
        this.userinfoRepo.save(userRes);
        return "success";
    }

    public UpdateUserDTO saveEditableUser(UpdateUserDTO request, String username) throws IOException {
        System.out.println("entered saveEditableUser with request " + request + " and username " + username);
        request.setUsername(username);
        UserInfo userinfo = this.userinfoRepo.findByUsername(username).get();
        userinfo.setName(request.getName());
        userinfo.setPhoneNumber(request.getPhone());
        userinfo.setEmail(request.getEmail());
        this.userinfoRepo.save(userinfo);
        ShopDetailsEntity shopDetails = this.shopDetailsRepo.findbyUsername(request.getUsername());
        if (shopDetails != null) {
            shopDetails.setAddresss(request.getAddress());
            shopDetails.setOwnerName(request.getShopOwner());
            shopDetails.setGstNumber(request.getGstNumber());
            shopDetails.setName(request.getName());
            shopDetails.setShopEmail(request.getShopEmail());
            shopDetails.setShopPhone(request.getShopPhone());
            shopDetails.setShopName(request.getShopName());
            this.shopDetailsRepo.save(shopDetails);
        } else {
            ShopDetailsEntity shopDetailsNew = new ShopDetailsEntity();
            shopDetailsNew.setUsername(request.getUsername());
            shopDetailsNew.setAddresss(request.getAddress());
            shopDetailsNew.setOwnerName(request.getShopOwner());
            shopDetailsNew.setName(request.getName());
            shopDetailsNew.setGstNumber(request.getGstNumber());
            shopDetailsNew.setShopEmail(request.getShopEmail());
            shopDetailsNew.setShopPhone(request.getShopPhone());
            shopDetailsNew.setShopName(request.getShopName());
            this.shopDetailsRepo.save(shopDetailsNew);
        }
        return request;
    }

    public String saveEditableUserProfilePic(MultipartFile profilePic, String username) throws IOException {
        System.out.println("entered saveEditableUserProfilePic with username " + username);
        String baseDir = System.getProperty("user.home") + File.separator + "MyBillingApp_Data" + File.separator + "ProfilePics";
        System.out.println("The base directory for profile pics is " + baseDir);
        File directory = new File(baseDir);
        if (!directory.exists())
            directory.mkdirs();
        String originalFilename = (new File(profilePic.getOriginalFilename())).getName();
        String newFileName = username + "_" + username + "_" + System.currentTimeMillis();
        File serverFile = new File(directory.getAbsolutePath() + File.separator + newFileName);
        profilePic.transferTo(serverFile);
        UserProfilePicEntity picRes = this.userProfilePicRepo.findByUsername(username);
        if (picRes != null && picRes.getProfilePic() != null) {
            File oldFile = new File(directory.getAbsolutePath() +  File.separator+picRes.getProfilePic());
            if (oldFile.exists())
                oldFile.delete();
        }
        if (picRes != null) {
            picRes.setProfilePic(newFileName);
            picRes.setUpdated_date(LocalDateTime.now());
            this.userProfilePicRepo.save(picRes);
        } else {
            UserProfilePicEntity picResNew = new UserProfilePicEntity();
            picResNew.setUpdated_date(LocalDateTime.now());
            picResNew.setUsername(username);
            picResNew.setProfilePic(newFileName);
            this.userProfilePicRepo.save(picResNew);
        }
        return "ok";
    }

    @Transactional
    public void deleteCustomer(Integer id) {
        this.shopRepo.updateStatus(id, "IN-ACTIVE", extractUsername(), Boolean.FALSE);
        try {
            this.salesCacheService.evictUserCustomers(extractUsername());
            this.salesCacheService.evictsUserAnalytics(extractUsername());
            this.salesCacheService.evictsReportsCache(extractUsername());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] generateInvoicePdf(String orderId) throws Exception {
        System.out.println(orderId);
        InvoiceDetails order = getOrderDetails(orderId);
        LocalDate orderedDate = LocalDate.parse(order.getOrderedDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        UpdateUserDTO userProfile = getUserProfile(extractUsername());
        String shopEmail = "";
        String gstNumber = "";
        String shopAddress = "";
        String shopPhone = "";
        String shopName = "";
        if (userProfile != null) {
            gstNumber = (userProfile.getGstNumber() != null) ? userProfile.getGstNumber() : "sample gst number";
            shopEmail = (userProfile.getShopEmail() != null) ? userProfile.getShopEmail() : "sample shop email";
            shopPhone = (userProfile.getShopPhone() != null) ? userProfile.getShopPhone() : "sample shop phone";
            shopAddress = (userProfile.getShopLocation() != null) ? userProfile.getShopLocation() : "sample shop address";
            shopName = (userProfile.getShopName() != null) ? userProfile.getShopName() : "sample shop name";
        }
        String formattedDate = orderedDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        byte[] response = this.pdfutil.generateInvoice(order.getCustomerName(), order.getCustomerEmail(), order
                .getCustomerPhone(), order.getInvoiceId(), order.getItems(), formattedDate, order.getTotalAmount(), order.isPaid(), order.getGstRate(), shopName, shopAddress, shopEmail, shopPhone, gstNumber);
        return response;
    }

    public byte[] generateGSTInvoicePdf(String orderId) throws Exception {
        System.out.println("Generating invoice for orderNumber-->" + orderId);
        String username = "";
        if (orderId != null) {
            BillingEntity billDetails = this.billRepo.findOrderByJustReference(orderId);
            username = billDetails.getUserId();
        }
        InvoiceData invoiceData = this.utils.getFullInvoiceDetails(username, orderId);
        String invoiceTemplateName = "gstinvoice";
        try {
            SelectedInvoiceEntity repoEntity = this.invoiceRepo.findByUsername(username);
            if (repoEntity != null)
                invoiceTemplateName = repoEntity.getTemplateName();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        byte[] response = this.pdfgstutil.generateGSTInvoice(invoiceData, invoiceTemplateName);
        System.out.println("The full invoice Data is " + invoiceData);
        return response;
    }

    public UpdateUserDTO getUserProfile(String username) {
        username = extractUsername();
        ShopBasicEntity shopBasicEntity = this.shopBasicRepo.findByUserId(username);
        ShopFinanceEntity shopFinanceEntity = this.shopFinanceRepo.findByUserId(username);
        ShopBankEntity shopBankEntity = new ShopBankEntity();
        ShopUPIEntity shopUPIEntity = new ShopUPIEntity();
        if (shopFinanceEntity != null) {
            shopBankEntity = this.shopBankRepo.findByShopFinanceId(username);
            shopUPIEntity = this.salesUPIRepo.findByShopFinanceId(username);
        }
        ShopInvoiceTermsEnity shopInvoiceTermsEntity = this.shopInvoiceTermsRepo.findByUserId(username);
        System.out.println("entered getUserProfile with request  username " + username);
        UserInfo userinfo = this.userinfoRepo.findByUsername(username).get();
        if (shopBasicEntity != null) {
            UpdateUserDTO updateUserDTO = UpdateUserDTO.builder().username(username).email(getIfNotNull(userinfo, UserInfo::getEmail)).name(getIfNotNull(userinfo, UserInfo::getName)).phone(getIfNotNull(userinfo, UserInfo::getPhoneNumber)).userSource(getIfNotNull(userinfo, UserInfo::getSource)).address(getIfNotNull(shopBasicEntity, ShopBasicEntity::getAddress)).shopLocation(getIfNotNull(shopBasicEntity, ShopBasicEntity::getAddress)).shopAddress(getIfNotNull(shopBasicEntity, ShopBasicEntity::getAddress)).shopEmail(getIfNotNull(shopBasicEntity, ShopBasicEntity::getShopEmail)).shopPhone(getIfNotNull(shopBasicEntity, ShopBasicEntity::getShopPhone)).shopName(getIfNotNull(shopBasicEntity, ShopBasicEntity::getShopName)).shopPincode(getIfNotNull(shopBasicEntity, ShopBasicEntity::getShopPincode)).shopCity(getIfNotNull(shopBasicEntity, ShopBasicEntity::getShopCity)).shopState(getIfNotNull(shopBasicEntity, ShopBasicEntity::getShopState)).shopSlogan(getIfNotNull(shopBasicEntity, ShopBasicEntity::getShopSlogan)).gstNumber(getIfNotNull(shopFinanceEntity, ShopFinanceEntity::getGstin)).pan(getIfNotNull(shopFinanceEntity, ShopFinanceEntity::getPanNumber)).gstin(getIfNotNull(shopFinanceEntity, ShopFinanceEntity::getGstin)).upi(getIfNotNull(shopUPIEntity, ShopUPIEntity::getUpiId)).terms1(getIfNotNull(shopInvoiceTermsEntity, ShopInvoiceTermsEnity::getTerm)).bankAccount(getIfNotNull(shopBankEntity, ShopBankEntity::getAccountNumber)).bankAddress(getIfNotNull(shopBankEntity, ShopBankEntity::getBranchName)).bankHolder(getIfNotNull(shopBankEntity, ShopBankEntity::getAccountHolderName)).bankName(getIfNotNull(shopBankEntity, ShopBankEntity::getBankName)).bankIfsc(getIfNotNull(shopBankEntity, ShopBankEntity::getIfscCode)).build();
            return updateUserDTO;
        }
        UpdateUserDTO response = UpdateUserDTO.builder().address("").email(userinfo.getEmail()).gstNumber("").name(userinfo.getName()).phone(userinfo.getPhoneNumber()).shopLocation("").shopOwner("").username(username).userSource(userinfo.getSource()).build();
        return response;
    }

    public byte[] getProfilePic(String username) throws IOException {
        System.out.println("entered getProfilePic with request username " + username);
        UserInfo res = this.userinfoRepo.findByUsername(username).get();
        byte[] content = null;
        if (!res.getSource().equals("google")) {
            try {
                UserProfilePicEntity picRes = this.userProfilePicRepo.findByUsername(username);
                if (picRes != null && picRes.getProfilePic() != null) {
                    String baseDir = System.getProperty("user.home") + File.separator + "MyBillingApp_Data" + File.separator + "ProfilePics";
                    File file = new File(baseDir  + File.separator+ picRes.getProfilePic());
                    if (file.exists()) {
                        content = Files.readAllBytes(file.toPath());
                    } else {
                        System.err.println("Profile pic file not found at: " + file.getAbsolutePath());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                content = null;
            }
        } else if (res.getProfilePiclink() != null) {
            String imageUrl = res.getProfilePiclink();
            try {
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                InputStream inputStream = connection.getInputStream();
                try {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    try {
                        byte[] data = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(data, 0, data.length)) != -1)
                            buffer.write(data, 0, bytesRead);
                        content = buffer.toByteArray();
                        buffer.close();
                    } catch (Throwable throwable) {
                        try {
                            buffer.close();
                        } catch (Throwable throwable1) {
                            throwable.addSuppressed(throwable1);
                        }
                        throw throwable;
                    }
                    if (inputStream != null)
                        inputStream.close();
                } catch (Throwable throwable) {
                    if (inputStream != null)
                        try {
                            inputStream.close();
                        } catch (Throwable throwable1) {
                            throwable.addSuppressed(throwable1);
                        }
                    throw throwable;
                }
            } catch (IOException e) {
                e.printStackTrace();
                content = null;
            }
        }
        return content;
    }

    @Transactional
    public void deleteProduct(Integer id) {
        System.out.println("endtered deleteProduct with productId " + id);
        this.prodRepo.deActivateProduct(id, Boolean.FALSE, extractUsername());
        try {
            this.salesCacheService.evictUserProducts(extractUsername());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Cacheable(value = {"analytics"}, keyGenerator = "userScopedKeyGenerator")
    public AnalyticsResponse getAnalytics(AnalyticsRequest request) {
        AnalyticsResponse response = new AnalyticsResponse();
        String userId = extractUsername();
        List<String> labels = new ArrayList<>();
        List<Long> sales = new ArrayList<>();
        List<Long> stocks = new ArrayList<>();
        List<Integer> taxes = new ArrayList<>();
        List<Integer> customers = new ArrayList<>();
        List<Integer> onlinePaymentCounts = new ArrayList<>();
        List<Long> profits = new ArrayList<>();
        LocalDateTime startDate = LocalDate.parse(request.getStartDate()).atStartOfDay();
        LocalDateTime endDate = LocalDate.parse(request.getEndDate()).atTime(LocalTime.MAX);
        List<Object[]> resultsSales = this.billRepo.getMonthlySalesSummary(startDate, endDate, userId);
        for (Object[] row : resultsSales) {
            String month = (String) row[0];
            labels.add(month);
            Long count = Long.valueOf(((Number) row[1]).longValue());
            sales.add(count);
        }
        try {
            List<Object[]> resultsStocks = this.billRepo.getMonthlyStocksSold(startDate, endDate, userId);
            for (Object[] row : resultsStocks) {
                Long count = Long.valueOf(((Number) row[1]).longValue());
                stocks.add(count);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<Object[]> resultsTaxes = this.billRepo.getMonthlyTaxesSummary(startDate, endDate, userId);
        for (Object[] row : resultsTaxes) {
            Integer count = Integer.valueOf(((Number) row[1]).intValue());
            taxes.add(count);
        }
        try {
            List<Object[]> resultsCustomers = this.shopRepo.getMonthlyCustomerCount(startDate, endDate, userId);
            for (Object[] row : resultsCustomers) {
                Integer count = Integer.valueOf(((Number) row[1]).intValue());
                customers.add(count);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<Object[]> resultsOnlinePaymentCount = this.salesPaymentRepo.getMonthlyPaymentCounts(startDate, endDate, userId);
        for (Object[] row : resultsOnlinePaymentCount) {
            Integer count = Integer.valueOf(((Number) row[1]).intValue());
            onlinePaymentCounts.add(count);
        }
        for (Object[] row : resultsSales) {
            double percentage = 0.08D + 0.12000000000000001D * this.random.nextDouble();
            System.out.println("The profits on cp are " + ((Number) row[2]).longValue());
            Long count = Long.valueOf(((Number) row[1]).longValue());
            Long estimatedProfit = Long.valueOf(((Number) row[1]).longValue());
            profits.add(Long.valueOf(((Number) row[2]).longValue()));
        }
        response.setCustomers(customers);
        response.setLabels(labels);
        response.setProfits(profits);
        response.setSales(onlinePaymentCounts);
        response.setStocks(stocks);
        response.setTaxes(taxes);
        response.setRevenues(sales);
        return response;
    }

    public Map<String, String> getUserProfileDetails() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("Current user: " + username);
        Map<String, String> response = new HashMap<>();
        response.put("username", username);
        return response;
    }

    public UserProfileDto getUserProfileWithRoles() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserProfileDto response = UserProfileDto.builder().username(username).email("na@na.com").roles(extractRoles()).build();
        System.out.println("getUserProfileWithRoles: " + response);
        return response;
    }

    @Cacheable(value = {"notifications"}, keyGenerator = "userScopedKeyGenerator")
    public NotificationDTO getAllNotifications(int page, int limit, String sort, String domain, String seen, String s) {
        NotificationDTO response = new NotificationDTO();
        List<ShopNotifications> notifications = new ArrayList<>();
        String sortField = "updated_date";
        if ("createdAt".equalsIgnoreCase(sortField))
            sortField = "created_date";
        Sort.Direction direction = "asc".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortOrder = Sort.by(direction, new String[]{sortField});
        PageRequest pageRequest = PageRequest.of(page - 1, limit, sortOrder);
        String username = extractUsername();
        Page<MessageEntity> notificationsList = null;
        if (seen != null && !seen.isEmpty() && !seen.equals("all")) {
            Boolean isRead = Boolean.valueOf(false);
            if (seen.equals("seen")) {
                isRead = Boolean.valueOf(true);
            } else {
                isRead = Boolean.valueOf(false);
            }
            if (seen.equals("flagged")) {
                Boolean isFlagged = Boolean.valueOf(true);
                notificationsList = this.notiRepo.findAllNotificationsByFlaggedStatus(extractUsername(), domain, isFlagged, Boolean.FALSE, (Pageable) pageRequest);
            } else {
                notificationsList = this.notiRepo.findAllNotificationsByReadStatus(extractUsername(), domain, isRead, Boolean.FALSE, (Pageable) pageRequest);
            }
        } else {
            notificationsList = this.notiRepo.findAllNotifications(extractUsername(), domain, Boolean.FALSE, (Pageable) pageRequest);
        }
        for (MessageEntity obj : notificationsList)
            notifications.add(ShopNotifications.builder().createdAt(obj.getCreatedDate()).title(obj.getTitle()).id(String.valueOf(obj.getId())).subject(obj.getSubject()).message(obj.getDetails()).seen(obj.getIsRead().booleanValue()).domain(obj.getDomain()).searchKey(obj.getSearchKey()).isFlagged(obj.getIsFlagged()).build());
        return NotificationDTO.builder().count(Integer.valueOf(notifications.size())).notifications(notifications).build();
    }

    @Transactional
    public void updateNotificationStatus(NotificationStatusUpdateRequest request) {
        request.getNotificationIds().stream().forEach(notificationId -> this.notiRepo.updateNotificationStatus(notificationId, extractUsername(), Boolean.TRUE));
    }

    @Transactional
    public Map<String, Object> flagNotifications(Integer notificationId, Boolean flag) {
        this.notiRepo.updateNotificationFlaggedStatus(notificationId, extractUsername(), flag);
        Map<String, Object> response = new HashMap<>();
        response.put("id", notificationId);
        response.put("flagged", Boolean.TRUE);
        return response;
    }

    @Transactional
    public Map<String, Object> deleteNotifications(Integer notificationId) {
        this.notiRepo.updateNotificationDeleteStatus(notificationId, extractUsername(), Boolean.TRUE);
        Map<String, Object> response = new HashMap<>();
        response.put("id", notificationId);
        response.put("deleted", Boolean.TRUE);
        return response;
    }

    public Map<String, Boolean> getAvailablePaymentMethods() {
        UserPaymentModes paymentModes = this.paymentModesRepo.getUserPaymentModes(extractUsername());
        Map<String, Boolean> response = new HashMap<>();
        System.out.println("The paymentModes are " + paymentModes);
        if (paymentModes != null) {
            if (paymentModes.getCard().booleanValue()) {
                response.put("card", Boolean.valueOf(true));
            } else {
                response.put("card", Boolean.valueOf(false));
            }
            if (paymentModes.getCash().booleanValue()) {
                response.put("cash", Boolean.valueOf(true));
            } else {
                response.put("cash", Boolean.valueOf(false));
            }
            if (paymentModes.getUpi().booleanValue()) {
                response.put("upi", Boolean.valueOf(true));
            } else {
                response.put("upi", Boolean.valueOf(false));
            }
        }
        System.out.println("the getAvailablePaymentMethods response is " + response);
        return response;
    }

    @Transactional
    public void updatePaymentReferenceNumber(String paymentRef, String orderRef) {
        this.salesPaymentRepo.updatePaymentReferenceNumber(paymentRef, orderRef, extractUsername());
    }

    @Cacheable(value = {"dashboard"}, keyGenerator = "userScopedKeyGenerator")
    public List<WeeklySales> getWeeklyAnalytics(String range) {
        String userId = extractUsername();
        List<WeeklySales> response = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<Long> sales = new ArrayList<>();
        List<Long> stocks = new ArrayList<>();
        List<Integer> taxes = new ArrayList<>();
        List<Integer> customers = new ArrayList<>();
        List<Integer> onlinePaymentCounts = new ArrayList<>();
        List<Long> profits = new ArrayList<>();
        LocalDateTime startDate = LocalDateTime.now().minusDays(7L);
        List<Object[]> resultsSales = new ArrayList();
        LocalDateTime endDate = LocalDateTime.now();
        try {
            if (range.equals("today"))
                resultsSales = this.billRepo.getSalesAndStocksToday(endDate, userId);
            if (range.equals("lastWeek"))
                resultsSales = this.billRepo.getWeeklySalesAndStocks(endDate, userId);
            if (range.equals("lastMonth"))
                resultsSales = this.billRepo.getSalesAndStocksMonthly(endDate, userId);
            if (range.equals("lastYear")) {
                startDate = LocalDateTime.now().minusDays(365L);
                resultsSales = this.billRepo.getSalesAndStocksYearly(endDate, userId);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        for (Object[] row : resultsSales) {
            WeeklySales weeklysales = new WeeklySales();
            String day = (String) row[0];
            labels.add(day);
            Long count = Long.valueOf(((Number) row[1]).longValue());
            Integer stocksCount = Integer.valueOf(((Number) row[3]).intValue());
            sales.add(count);
            weeklysales.setDay(day);
            weeklysales.setUnitsSold(stocksCount.intValue());
            weeklysales.setTotalSales(count.longValue());
            response.add(weeklysales);
        }
        return response;
    }

    @Cacheable(value = {"dashboard"}, keyGenerator = "userScopedKeyGenerator")
    public List<SalesResponseDTO> getTopNSales(int count, String range) {
        String username = extractUsername();
        List<BillingEntity> billingDetails = new ArrayList<>();
        if (range.equals("today"))
            billingDetails = this.billRepo.findTopNSalesForToday(username, count);
        if (range.equals("lastWeek"))
            billingDetails = this.billRepo.findTopNSalesForLastWeek(username, count);
        if (range.equals("lastMonth"))
            billingDetails = this.billRepo.findTopNSalesForLastMonth(username, count);
        if (range.equals("lastYear"))
            billingDetails = this.billRepo.findTopNSalesForLastYear(username, count);
        List<SalesResponseDTO> dtoList = billingDetails.stream().map(obj -> {
            String customerName = this.shopRepo.findByIdAndUserId(obj.getCustomerId(), username).getName();
            String paymentStatus = this.salesPaymentRepo.findPaymentDetails(obj.getId(), username).getStatus();
            return SalesResponseDTO.builder().customer(customerName).remarks(obj.getRemarks()).date(obj.getCreatedDate().toString()).id(obj.getInvoiceNumber()).total(obj.getTotalAmount()).status(paymentStatus).build();
        }).toList();
        return dtoList;
    }

    public String updateEstimatedGoals(GoalRequest request) {
        EstimatedGoalsEntity existingGoals = this.estimatedGoalsRepo.findByUserId(extractUsername());
        if (existingGoals != null) {
            existingGoals.setId(existingGoals.getId());
            existingGoals.setUserId(extractUsername());
            existingGoals.setSales(request.getEstimatedSales());
            existingGoals.setFromDate(request.getFromDate().atStartOfDay());
            existingGoals.setToDate(request.getToDate().atTime(LocalTime.MAX));
            existingGoals.setUpdatedBy(extractUsername());
            existingGoals.setUpdatedDate(LocalDateTime.now());
            this.estimatedGoalsRepo.save(existingGoals);
            this.salesCacheService.evictsUserGoals(extractUsername());
        } else {
            EstimatedGoalsEntity newGoals = EstimatedGoalsEntity.builder().sales(request.getEstimatedSales()).userId(extractUsername()).fromDate(request.getFromDate().atStartOfDay()).toDate(request.getToDate().atTime(LocalTime.MAX)).createdBy(extractUsername()).createdDate(LocalDateTime.now()).updatedDate(LocalDateTime.now()).updatedBy(extractUsername()).build();
            this.estimatedGoalsRepo.save(newGoals);
            this.salesCacheService.evictsUserGoals(extractUsername());
        }
        return "Success";
    }

    @Cacheable(value = {"goals"}, keyGenerator = "userScopedKeyGenerator")
    public GoalData getTimeRangeGoalData(String range) {
        EstimatedGoalsEntity existingGoals = this.estimatedGoalsRepo.findByUserId(extractUsername());
        System.out.println("The existing goals are " + existingGoals);
        String username = extractUsername();
        List<BillingEntity> billingDetails = new ArrayList<>();
        if (range.equals("today"))
            billingDetails = this.billRepo.findSalesNDays(username, LocalDateTime.now().toLocalDate().atTime(LocalTime.MIN), LocalDateTime.now().toLocalDate().atTime(LocalTime.MAX));
        if (range.equals("lastWeek"))
            billingDetails = this.billRepo.findSalesNDays(username, LocalDateTime.now().minusWeeks(1L), LocalDateTime.now());
        if (range.equals("lastMonth"))
            billingDetails = this.billRepo.findSalesNDays(username, LocalDateTime.now().minusMonths(1L), LocalDateTime.now());
        if (range.equals("lastYear"))
            billingDetails = this.billRepo.findSalesNDays(username, LocalDateTime.now().minusYears(1L), LocalDateTime.now());
        Double[] actualSalesList = {Double.valueOf(0.0D)};
        billingDetails.stream().forEach(obj -> actualSalesList[0] = Double.valueOf(actualSalesList[0].doubleValue() + obj.getTotalAmount().doubleValue()));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
        String fromDateStr = (existingGoals != null && existingGoals.getFromDate() != null) ? existingGoals.getFromDate().format(formatter) : null;
        String toDateStr = (existingGoals != null && existingGoals.getToDate() != null) ? existingGoals.getToDate().format(formatter) : null;
        GoalData response = GoalData.builder().actualSales(actualSalesList[0]).estimatedSales(Double.valueOf((existingGoals != null) ? existingGoals.getSales().doubleValue() : 0.0D)).fromDate(fromDateStr).toDate(toDateStr).build();
        System.out.println("response for the goals-->" + response);
        return response;
    }

    @Cacheable(value = {"topSellings"}, keyGenerator = "userScopedKeyGenerator")
    public List<TopProductDto> getTopProducts(int count, String timeRange, String factor) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = LocalDateTime.now();
        List<ProductPerformanceProjection> topProducts = new ArrayList<>();
        List<TopProductDto> response = new ArrayList<>();
        if (timeRange.equals("lastWeek"))
            startDate = LocalDateTime.now().minusDays(7L);
        if (timeRange.equals("lastMonth"))
            startDate = LocalDateTime.now().minusMonths(1L);
        if (timeRange.equals("lastYear"))
            startDate = LocalDateTime.now().minusYears(1L);
        if (timeRange.equals("today")) {
            startDate = LocalDateTime.now().toLocalDate().atTime(LocalTime.MIN);
            endDate = LocalDateTime.now().toLocalDate().atTime(LocalTime.MAX);
        }
        if (factor.equals("mostSelling"))
            topProducts = this.prodSalesRepo.findMostSellingProducts(extractUsername(), startDate, endDate, count);
        if (factor.equals("topGrossing"))
            topProducts = this.prodSalesRepo.findTopGrossingProducts(extractUsername(), startDate, endDate, count);
        if (topProducts.size() > 0)
            response = (List<TopProductDto>) topProducts.stream().map(obj -> TopProductDto.builder().category(obj.getCategory()).currentStock(obj.getCurrentStock().intValue()).productName(obj.getProductName()).amount(obj.getRevenue().doubleValue()).count(obj.getUnitsSold().longValue()).build()).collect(Collectors.toList());
        System.out.println("The top products are " + response);
        return response;
    }

    @Cacheable(value = {"topOrders"}, keyGenerator = "userScopedKeyGenerator")
    public List<TopOrdersDto> getTopOrders(int count, String timeRange) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = LocalDateTime.now();
        List<ProductPerformanceProjection> topProducts = new ArrayList<>();
        List<TopOrdersDto> response = new ArrayList<>();
        if (timeRange.equals("lastWeek"))
            startDate = LocalDateTime.now().minusDays(7L);
        if (timeRange.equals("lastMonth"))
            startDate = LocalDateTime.now().minusMonths(1L);
        if (timeRange.equals("lastYear"))
            startDate = LocalDateTime.now().minusYears(1L);
        if (timeRange.equals("today"))
            startDate = LocalDateTime.now().toLocalDate().atStartOfDay();
        List<BillingEntity> billList = this.billRepo.findTopNSalesForGivenRange(extractUsername(), startDate, endDate, count);
        if (billList.size() > 0)
            response = (List<TopOrdersDto>) billList.stream().map(obj -> {
                String customerName = this.shopRepo.findByIdAndUserId(obj.getCustomerId(), extractUsername()).getName();
                String paymentStatus = this.salesPaymentRepo.findPaymentDetails(obj.getId(), extractUsername()).getStatus();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                String date = obj.getCreatedDate().format(formatter);
                return TopOrdersDto.builder().customer(customerName).orderId(obj.getInvoiceNumber()).total(obj.getTotalAmount().doubleValue()).date(date).build();
            }).collect(Collectors.toList());
        return response;
    }

    @Cacheable(value = {"paymentBreakdowns"}, keyGenerator = "userScopedKeyGenerator")
    public Map<String, Double> getPaymentBreakdown(String timeRange) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = LocalDateTime.now();
        List<ProductPerformanceProjection> topProducts = new ArrayList<>();
        List<TopOrdersDto> response = new ArrayList<>();
        if (timeRange.equals("lastWeek"))
            startDate = LocalDateTime.now().minusDays(7L);
        if (timeRange.equals("lastMonth"))
            startDate = LocalDateTime.now().minusMonths(1L);
        if (timeRange.equals("lastYear"))
            startDate = LocalDateTime.now().minusYears(1L);
        if (timeRange.equals("today"))
            startDate = LocalDateTime.now().toLocalDate().atStartOfDay();
        List<Map<String, Object>> rawData = this.salesPaymentRepo.getPaymentBreakdown(extractUsername(), startDate, endDate);
        Map<String, Double> result = new HashMap<>();
        for (Map<String, Object> row : rawData) {
            String method = (String) row.get("paymentMethod");
            Number count = (Number) row.get("count");
            result.put(method.toLowerCase(), Double.valueOf(count.doubleValue()));
        }
        return result;
    }

    public String updateShopLogo(MultipartFile shopLogo) throws IOException {
        String username = extractUsername();
        System.out.println("entered updateShopLogo with username " + username);
        String baseDir = System.getProperty("user.home") + File.separator + "MyBillingApp_Data" + File.separator + "ShopLogos";
        File directory = new File(baseDir);
        if (!directory.exists())
            directory.mkdirs();
        String originalFilename = (new File(shopLogo.getOriginalFilename())).getName();
        String newFileName = username + "_shop_" + username + "_" + System.currentTimeMillis();
        File serverFile = new File(directory.getAbsolutePath() + File.separator+newFileName);
        shopLogo.transferTo(serverFile);
        UserProfilePicEntity picRes = this.userProfilePicRepo.findByUsername(username);
        if (picRes != null && picRes.getShopLogo() != null) {
            File oldFile = new File(directory.getAbsolutePath() + File.separator+picRes.getShopLogo());
            if (oldFile.exists())
                oldFile.delete();
        }
        if (picRes != null) {
            picRes.setShopLogo(newFileName);
            picRes.setUpdated_date(LocalDateTime.now());
            this.userProfilePicRepo.save(picRes);
        } else {
            UserProfilePicEntity picResNew = new UserProfilePicEntity();
            picResNew.setUpdated_date(LocalDateTime.now());
            picResNew.setUsername(username);
            picResNew.setShopLogo(newFileName);
            this.userProfilePicRepo.save(picResNew);
        }
        return "ok";
    }

    private String safe(String value) {
        return (value != null) ? value.trim() : "";
    }

    @Transactional
    public String updateBasicDetails(ShopBasicDetailsRequest request) {
        System.out.println("ShopBasicDetailsRequest with request->" + request);
        this.shopBasicRepo.removeExistingBasicDetails(extractUsername());
        ShopBasicEntity shopEntity = ShopBasicEntity.builder().shopName(safe(request.getShopName())).shopSlogan(safe(request.getShopSlogan())).shopPhone(safe(request.getShopPhone())).shopEmail(safe(request.getShopEmail())).address(safe(request.getShopAddress())).shopPincode(safe(request.getShopPincode())).shopCity(safe(request.getShopCity())).shopState(safe(request.getShopState())).userId(extractUsername()).updatedBy(extractUsername()).updatedAt(LocalDateTime.now()).build();
        ShopBasicEntity res = (ShopBasicEntity) this.shopBasicRepo.save(shopEntity);
        if (res != null) {
            ShopFinanceEntity shopFinanceEntity = ShopFinanceEntity.builder().gstin(safe(request.getGstin())).panNumber(safe(request.getPanNumber())).userId(extractUsername()).updatedBy(extractUsername()).updatedAt(LocalDateTime.now()).build();
            this.shopFinanceRepo.removeShopFinanceEntities(extractUsername());
            ShopFinanceEntity shopFinanceEntity1 = (ShopFinanceEntity) this.shopFinanceRepo.save(shopFinanceEntity);
        }
        return (res != null) ? "Success" : "Not Successful";
    }

    @Transactional
    public String updateFinanceDetails(ShopFinanceDetailsRequest request) {
        ShopFinanceEntity shopFinanceEntity = ShopFinanceEntity.builder().gstin(safe(request.getGstin())).panNumber(safe(request.getPan())).userId(extractUsername()).updatedBy(extractUsername()).updatedAt(LocalDateTime.now()).build();
        this.shopFinanceRepo.removeShopFinanceEntities(extractUsername());
        ShopFinanceEntity finRes = (ShopFinanceEntity) this.shopFinanceRepo.save(shopFinanceEntity);
        if (finRes != null) {
            ShopBankEntity shopBankEntity = ShopBankEntity.builder().accountHolderName(safe(request.getBankHolder())).accountNumber(safe(request.getBankAccount())).ifscCode(safe(request.getBankIfsc())).bankName(safe(request.getBankName())).branchName(safe(request.getBankAddress())).shopFinanceId(finRes.getId()).userId(extractUsername()).updatedBy(extractUsername()).updatedAt(LocalDateTime.now()).build();
            this.shopBankRepo.removeBankDetails(extractUsername());
            this.shopBankRepo.save(shopBankEntity);
            ShopUPIEntity shopUPIEntity = ShopUPIEntity.builder().upiId(safe(request.getUpi())).upiProvider("google").shopFinanceId(finRes.getId()).userId(extractUsername()).updatedBy(extractUsername()).updatedAt(LocalDateTime.now()).build();
            this.salesUPIRepo.removeUpiId(extractUsername());
            this.salesUPIRepo.save(shopUPIEntity);
            return "Success";
        }
        return "Not Successful";
    }

    public String updateOtherDetails(ShopInvoiceTerms request) {
        ShopInvoiceTermsEnity shopInvoiceTermsEntity = ShopInvoiceTermsEnity.builder().term(safe(request.getTerms1())).userId(extractUsername()).updatedBy(extractUsername()).updatedAt(LocalDateTime.now()).build();
        ShopInvoiceTermsEnity res = (ShopInvoiceTermsEnity) this.shopInvoiceTermsRepo.save(shopInvoiceTermsEntity);
        return (res != null) ? "Success" : "Not Successful";
    }

    public byte[] getShopLogo(String username) throws IOException {
        System.out.println("entered getShopLogo with request username " + username);
        UserProfilePicEntity picRes = this.userProfilePicRepo.findByUsername(username);
        if (picRes == null || picRes.getShopLogo() == null)
            return null;
        String baseDir = System.getProperty("user.home") + File.separator + "MyBillingApp_Data" + File.separator + "ShopLogos";
        File file = new File(baseDir +  File.separator+picRes.getShopLogo());
        byte[] content = null;
        try {
            if (file.exists()) {
                content = Files.readAllBytes(file.toPath());
            } else {
                System.err.println("Shop logo file not found at: " + file.getAbsolutePath());
            }
        } catch (IOException e) {
            e.printStackTrace();
            content = null;
        }
        return content;
    }

    public List<ProductSearchDto> findProductsByQuery(String query, int limit) {
        List<ProductSearchDto> response = new ArrayList<>();
        String username = extractUsername();
        List<ProductEntity> productList = this.prodRepo.findAllActiveProductsForGSTBilling(Boolean.TRUE, username, query, limit);
        productList.stream().forEach(obj -> {
            ProductSearchDto prodSearch = ProductSearchDto.builder().id(Long.valueOf(obj.getId().intValue())).name(obj.getName()).hsn(obj.getHsn()).price(BigDecimal.valueOf(obj.getPrice().intValue())).costPrice(BigDecimal.valueOf(obj.getCostPrice().intValue())).tax(obj.getTaxPercent().intValue()).stock(obj.getStock().intValue()).build();
            response.add(prodSearch);
        });
        System.out.println("The result list for the query " + query + " is " + response);
        return response;
    }

    public Map<String, String> sendPaymentReminder(Map<String, Object> request) {
        String orderNo = (String) request.get("orderId");
        BillingEntity billDetails = this.billRepo.findByInvoiceNumber(orderNo);
        CustomerEntity customer = this.shopRepo.findByIdAndUserId(billDetails.getCustomerId(), extractUsername(orderNo));
        Double totalAmount = billDetails.getTotalAmount();
        Double paidAmount = billDetails.getPayingAmount();
        Double dueAmout = billDetails.getRemainingAmount();
        String customerName = customer.getName();
        String customerEmail = customer.getEmail();
        String message = (String) request.get("message");
        String htmlTemplate = this.emailTemplate.getPaymentReminderEmailContent(orderNo, totalAmount.doubleValue(), paidAmount.doubleValue(), dueAmout.doubleValue(), customerName, message);
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        ShopBasicEntity shopBasic = this.shopBasicRepo.findByUserId(extractUsername(orderNo));
        try {
            CompletableFuture<String> futureResult = this.email.sendEmailForPaymentReminder(customerEmail, orderNo, customerName, htmlTemplate, shopBasic.getShopName());
            this.billRepo.updateReminderCount(orderNo, extractUsername(orderNo), LocalDateTime.now());
            this.salesPaymentRepo.updateReminderCount(orderNo, extractUsername(orderNo), LocalDateTime.now());
            this.salesCacheService.evictUserSales(extractUsername(orderNo));
            this.salesCacheService.evictUserPayments(extractUsername(orderNo));
        } catch (MailjetException e) {
            throw new RuntimeException(e);
        } catch (MailjetSocketTimeoutException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Transactional
    public Map<String, Object> updateDuePayments(Map<String, Object> request) {
        String orderNo = (String) request.get("invoiceId");
        Double amount = Double.valueOf(Double.parseDouble((String) request.get("amount")));
        try {
            this.billRepo.updateDuePayment(orderNo, extractUsername(), LocalDateTime.now(), amount);
            this.salesPaymentRepo.updateDueAmount(orderNo, extractUsername(), LocalDateTime.now(), amount);
            this.salesCacheService.evictUserSales(extractUsername());
            this.salesCacheService.evictUserPayments(extractUsername());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        PaymentEntity paymentDetails = this.salesPaymentRepo.findByOrderNumber(orderNo);
        try {
            String status = "SemiPaid";
            if (paymentDetails.getToBePaid().doubleValue() <= 0.0D)
                status = "Paid";
            this.salesPaymentRepo.updatePaymentStatus(orderNo, extractUsername(), status);
            this.utils.asyncSavePaymentHistory(paymentDetails.getBillingId(), paymentDetails.getId(), amount, orderNo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("paid", paymentDetails.getPaid());
        response.put("due", paymentDetails.getToBePaid());
        response.put("status", paymentDetails.getStatus());
        log.info("The response updateDuePayments is " + response);
        return response;
    }

    public List<Map<String, Object>> getPaymentHistory(Map<String, Object> request) {
        String orderNo = (String) request.get("orderNumber");
        String paymentRefereceNumber = (String) request.get("PaymentReferenceNumber");
        List<Map<String, Object>> response = new ArrayList<>();
        List<PaymentHistory> historyList = this.utils.getPaymentHistory(orderNo);
        historyList.stream().forEach(obj -> {
            Map<String, Object> historyMap = new HashMap<>();
            historyMap.put("date", obj.getUpdatedDate());
            historyMap.put("amount", obj.getPaidAmount());
            historyMap.put("tokenNumber", obj.getTokenNo());
            response.add(historyMap);
        });
        log.info("The response getPaymentHistory is " + response);
        return response;
    }

    public Map<String, Object> sendInvoiceOverEmail(String invoiceNumber) {
        System.out.println("Entered sending email by listner with refrenece Number " + invoiceNumber);
        InvoiceDetails order = getOrderDetails(invoiceNumber);
        Map<String, Object> response = new HashMap<>();
        try {
            String username = "";
            if (invoiceNumber != null) {
                BillingEntity billDetails = this.billRepo.findOrderByJustReference(invoiceNumber);
                username = billDetails.getUserId();
            }
            Map<String, Object> emailContent = this.emailTemplate.generateOrderHtml(order, username);
            CompletableFuture<String> futureResult = this.email.sendEmail(order.getCustomerEmail(), invoiceNumber, order
                            .getCustomerName(),
                    generateGSTInvoicePdf(invoiceNumber), (String) emailContent.get("htmlTemplate"), (String) emailContent.get("shopName"));
            System.out.println(futureResult);
        } catch (Exception e) {
            e.printStackTrace();
        }
        response.put("errorData", "success");
        return response;
    }

    public Map<String, Object> sendInvoiceOverEmailByListner(String invoiceNumber) {
        System.out.println("Entered sending email by listner with refrenece Number " + invoiceNumber);
        InvoiceDetails order = getOrderDetails(invoiceNumber);
        Map<String, Object> response = new HashMap<>();
        try {
            String username = "";
            if (invoiceNumber != null) {
                BillingEntity billDetails = this.billRepo.findOrderByJustReference(invoiceNumber);
                username = billDetails.getUserId();
            }
            Map<String, Object> emailContent = this.emailTemplate.generateOrderHtml(order, username);
            CompletableFuture<String> futureResult = this.email.sendEmail(order.getCustomerEmail(), invoiceNumber, order
                            .getCustomerName(),
                    generateGSTInvoicePdf(invoiceNumber), (String) emailContent.get("htmlTemplate"), (String) emailContent.get("shopName"));
            System.out.println(futureResult);
        } catch (Exception e) {
            e.printStackTrace();
        }
        response.put("errorData", "success");
        return response;
    }

    public List<Map<String, Object>> globalSearch(String globalSearchTerms, Integer limit) {
        List<GlobalSearchIndex> searchList = this.globalSearchRepo.findActiveEntities(extractUsername(), globalSearchTerms, Boolean.TRUE);
        List<Map<String, Object>> response = (List<Map<String, Object>>) searchList.stream().map(obj -> {
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("id", obj.getSourceId());
            responseMap.put("displayName", obj.getDisplayName());
            responseMap.put("sourceType", obj.getSourceType());
            return responseMap;
        }).collect(Collectors.toList());
        System.out.println("The global search response is " + response);
        return response;
    }

    public String clearServerSideCache() {
        try {
            this.salesCacheService.evictUserSales(extractUsername());
            this.salesCacheService.evictUserProducts(extractUsername());
            this.salesCacheService.evictUserPayments(extractUsername());
            this.salesCacheService.evictUserCustomers(extractUsername());
            this.salesCacheService.evictUserDasbhoard(extractUsername());
            this.salesCacheService.evictsUserGoals(extractUsername());
            this.salesCacheService.evictsUserAnalytics(extractUsername());
            this.salesCacheService.evictsTopSelling(extractUsername());
            this.salesCacheService.evictsTopOrders(extractUsername());
            this.salesCacheService.evictsReportsCache(extractUsername());
            return "success";
        } catch (Exception e) {
            e.printStackTrace();
            return "not success";
        }
    }

    public Map<String, Integer> getOrderCountForDay() {
        Map<String, Integer> response = new HashMap<>();
        String username = extractUsername();
        try {
            Integer totalOrders = this.billRepo.countOrdersForToday(username, LocalDateTime.now().toLocalDate().atStartOfDay(), LocalDateTime.now().toLocalDate().atTime(LocalTime.MAX));
            response.put("count", totalOrders);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return response;
    }

    public Map<String, Integer> addSubscriptions() {
        this.userinfoRepo.updateUserRole(extractUsername(), "ROLE_PREMIUM");
        return null;
    }

    @Cacheable(value = {"analytics"}, keyGenerator = "userScopedKeyGenerator")
    public AnalyticsRes getSuperAnalytics(AnalyticsRequest request) {
        String userId = extractUsername();
        YearMonth startYm = YearMonth.parse(request.getStartDate());
        YearMonth endYm = YearMonth.parse(request.getEndDate());
        LocalDateTime startDate = startYm.atDay(1).atStartOfDay();
        LocalDateTime endDate = endYm.atEndOfMonth().atTime(LocalTime.MAX);
        AnalyticsRes superResponse = new AnalyticsRes();
        List<Object[]> combinedResults = this.salesPaymentRepo.getCombinedPaymentSummary(startDate, endDate, userId);
        List<PieAnalyticsMap> paymentStatusList = new ArrayList<>();
        List<PieAnalyticsMap> invoiceStatusList = new ArrayList<>();
        if (combinedResults != null)
            for (Object[] row : combinedResults) {
                if (row == null || row[0] == null)
                    continue;
                String statusName = row[0].toString();
                double paymentValue = 0.0D;
                if (row[1] != null)
                    paymentValue = ((Number) row[1]).doubleValue();
                PieAnalyticsMap paymentMap = new PieAnalyticsMap();
                paymentMap.setName(statusName);
                paymentMap.setValue(Double.valueOf(paymentValue));
                paymentStatusList.add(paymentMap);
                double invoiceCount = 0.0D;
                if (row[2] != null)
                    invoiceCount = ((Number) row[2]).doubleValue();
                PieAnalyticsMap invoiceMap = new PieAnalyticsMap();
                invoiceMap.setName(statusName);
                invoiceMap.setValue(Double.valueOf(invoiceCount));
                invoiceStatusList.add(invoiceMap);
            }
        superResponse.setPaymentStatus(paymentStatusList);
        superResponse.setInvoiceStatus(invoiceStatusList);
        List<Object[]> billingResults = this.billRepo.getMonthlyBillingSummary(startDate, endDate, userId);
        List<PieAnalyticsMap> monthlyProfitsList = new ArrayList<>();
        List<PieAnalyticsMap> monthlyRevenueList = new ArrayList<>();
        List<PieAnalyticsMap> monthlyStockList = new ArrayList<>();
        List<PieAnalyticsMap> monthlySalesList = new ArrayList<>();
        double totalProfit = 0.0D;
        double totalRevenue = 0.0D;
        Double totalStockSold = Double.valueOf(0.0D);
        Double totalSales = Double.valueOf(0.0D);
        if (billingResults != null)
            for (Object[] row : billingResults) {
                if (row == null || row[0] == null)
                    continue;
                String month = row[0].toString();
                double profitValue = 0.0D;
                if (row[1] != null)
                    profitValue = ((Number) row[1]).doubleValue();
                PieAnalyticsMap profitMap = new PieAnalyticsMap();
                profitMap.setName(month);
                profitMap.setValue(Double.valueOf(profitValue));
                monthlyProfitsList.add(profitMap);
                totalProfit += profitValue;
                double revenueValue = 0.0D;
                if (row[2] != null)
                    revenueValue = ((Number) row[2]).doubleValue();
                PieAnalyticsMap revenueMap = new PieAnalyticsMap();
                revenueMap.setName(month);
                revenueMap.setValue(Double.valueOf(revenueValue));
                monthlyRevenueList.add(revenueMap);
                totalRevenue += revenueValue;
                double stockValue = 0.0D;
                if (row[3] != null)
                    stockValue = ((Number) row[3]).doubleValue();
                PieAnalyticsMap stockMap = new PieAnalyticsMap();
                stockMap.setName(month);
                stockMap.setValue(Double.valueOf(stockValue));
                monthlyStockList.add(stockMap);
                totalStockSold = Double.valueOf(totalStockSold.doubleValue() + stockValue);
                double salesCountValue = 0.0D;
                if (row[4] != null)
                    salesCountValue = ((Number) row[4]).doubleValue();
                PieAnalyticsMap salesMap = new PieAnalyticsMap();
                salesMap.setName(month);
                salesMap.setValue(Double.valueOf(salesCountValue));
                monthlySalesList.add(salesMap);
                totalSales = Double.valueOf(totalSales.doubleValue() + salesCountValue);
            }
        superResponse.setMonthlyProfits(monthlyProfitsList);
        superResponse.setTotalProfit(Double.valueOf(totalProfit));
        superResponse.setSalesAndRevenue(monthlyRevenueList);
        superResponse.setTotalRevenue(Double.valueOf(totalRevenue));
        superResponse.setMonthlyStockSold(monthlyStockList);
        superResponse.setTotalStockSold(totalStockSold);
        superResponse.setMonthlySales(monthlySalesList);
        superResponse.setTotalSales(totalSales);
        Integer n = Integer.valueOf(6);
        List<Object[]> topProductsResults = this.prodSalesRepo.getTopSoldProducts(startDate, endDate, userId, n);
        List<PieAnalyticsMap> topProductsList = new ArrayList<>();
        if (topProductsResults != null)
            for (Object[] row : topProductsResults) {
                if (row == null || row[0] == null)
                    continue;
                double value = 0.0D;
                if (row[1] != null)
                    value = ((Number) row[1]).doubleValue();
                PieAnalyticsMap pieMap = new PieAnalyticsMap();
                pieMap.setName(row[0].toString());
                pieMap.setValue(Double.valueOf(value));
                topProductsList.add(pieMap);
            }
        superResponse.setTopProducts(topProductsList);
        List<Object[]> gstResults = this.shopRepo.getCustomerGstSummary(startDate, endDate, userId);
        List<PieAnalyticsMap> customerGstList = new ArrayList<>();
        if (gstResults != null)
            for (Object[] row : gstResults) {
                if (row == null || row[0] == null)
                    continue;
                double value = 0.0D;
                if (row[1] != null)
                    value = ((Number) row[1]).doubleValue();
                PieAnalyticsMap pieMap = new PieAnalyticsMap();
                pieMap.setName(row[0].toString());
                pieMap.setValue(Double.valueOf(value));
                customerGstList.add(pieMap);
            }
        superResponse.setCustomerGst(customerGstList);
        log.info("The response getSuperAnalytics is " + superResponse);
        return superResponse;
    }

    public void sendReportEmail(MultipartFile file, String subject, List<String> emailList) {
        try {
            byte[] fileBytes = file.getBytes();
            emailList.stream().forEach(emailObj -> {
                String template = this.emailTemplate.getReportEmailContent("Sir", file.getOriginalFilename(), "monthly");
                CompletableFuture<String> futureResult = null;
                try {
                    futureResult = this.email.sendEmailReportWithAttachment(emailObj, subject, file.getOriginalFilename(), fileBytes, template, "Clear Bill");
                } catch (MailjetException e) {
                    throw new RuntimeException(e);
                } catch (MailjetSocketTimeoutException e) {
                    throw new RuntimeException(e);
                }
                System.out.println(futureResult);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
