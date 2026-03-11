package com.example.ecommerce.payment.iyzico;

import com.example.ecommerce.auth.model.User;
import com.example.ecommerce.order.model.Order;
import com.example.ecommerce.order.model.OrderItem;
import com.iyzipay.Options;
import com.iyzipay.model.Address;
import com.iyzipay.model.BasketItem;
import com.iyzipay.model.BasketItemType;
import com.iyzipay.model.Buyer;
import com.iyzipay.model.CheckoutForm;
import com.iyzipay.model.CheckoutFormInitialize;
import com.iyzipay.model.Currency;
import com.iyzipay.model.PaymentGroup;
import com.iyzipay.request.CreateCheckoutFormInitializeRequest;
import com.iyzipay.request.RetrieveCheckoutFormRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class IyzicoGatewayClient {
    private final IyzicoProperties properties;

    public IyzicoGatewayClient(IyzicoProperties properties) {
        this.properties = properties;
    }

    public IyzicoInitializeResult initializeCheckoutForm(Order order,
                                                         User user,
                                                         String locale,
                                                         String conversationId,
                                                         String callbackUrl) {
        CreateCheckoutFormInitializeRequest request = new CreateCheckoutFormInitializeRequest();
        request.setLocale(normalizeLocale(locale));
        request.setConversationId(conversationId);
        request.setPrice(normalizeMoney(order.getTotalAmount()));
        request.setPaidPrice(normalizeMoney(order.getTotalAmount()));
        request.setCurrency(Currency.TRY.name());
        request.setBasketId(String.valueOf(order.getId()));
        request.setPaymentGroup(PaymentGroup.PRODUCT.name());
        request.setCallbackUrl(callbackUrl);
        request.setBuyer(buildBuyer(order, user));
        request.setShippingAddress(buildShippingAddress(order, user));
        request.setBillingAddress(buildBillingAddress(order, user));
        request.setBasketItems(buildBasketItems(order));

        CheckoutFormInitialize response = CheckoutFormInitialize.create(request, buildOptions());
        boolean success = "success".equalsIgnoreCase(response.getStatus());
        return new IyzicoInitializeResult(
                success,
                response.getStatus(),
                response.getConversationId(),
                response.getToken(),
                response.getPaymentPageUrl(),
                response.getCheckoutFormContent(),
                response.getTokenExpireTime(),
                response.getErrorCode(),
                response.getErrorMessage()
        );
    }

    public IyzicoRetrieveResult retrieveCheckoutForm(String token) {
        RetrieveCheckoutFormRequest request = new RetrieveCheckoutFormRequest();
        request.setToken(token);
        CheckoutForm response = CheckoutForm.retrieve(request, buildOptions());
        boolean success = "success".equalsIgnoreCase(response.getStatus())
                && "SUCCESS".equalsIgnoreCase(response.getPaymentStatus());
        return new IyzicoRetrieveResult(
                success,
                response.getStatus(),
                response.getPaymentStatus(),
                response.getConversationId(),
                response.getPaymentId(),
                response.getErrorCode(),
                response.getErrorMessage()
        );
    }

    private Options buildOptions() {
        Options options = new Options();
        options.setApiKey(properties.getApiKey());
        options.setSecretKey(properties.getSecretKey());
        options.setBaseUrl(properties.getApiBaseUrl());
        return options;
    }

    private Buyer buildBuyer(Order order, User user) {
        Buyer buyer = new Buyer();
        buyer.setId(String.valueOf(user.getId()));
        buyer.setName(defaultIfBlank(user.getFirstName(), "Customer"));
        buyer.setSurname(defaultIfBlank(user.getLastName(), "User"));
        buyer.setGsmNumber(defaultIfBlank(order.getShippingPhone(), "+905555555555"));
        buyer.setEmail(defaultIfBlank(order.getShippingEmail(), user.getEmail()));
        buyer.setIdentityNumber(properties.getDefaultIdentityNumber());
        buyer.setRegistrationAddress(defaultIfBlank(order.getShippingAddressLine(), "N/A"));
        buyer.setIp(defaultIfBlank(properties.getDefaultBuyerIp(), null));
        buyer.setCity(defaultIfBlank(order.getShippingCity(), "Istanbul"));
        buyer.setCountry(defaultIfBlank(order.getShippingCountry(), "Turkey"));
        buyer.setZipCode(defaultIfBlank(order.getShippingPostalCode(), "34000"));
        return buyer;
    }

    private Address buildShippingAddress(Order order, User user) {
        Address address = new Address();
        address.setContactName(resolveContactName(order, user));
        address.setCity(defaultIfBlank(order.getShippingCity(), "Istanbul"));
        address.setCountry(defaultIfBlank(order.getShippingCountry(), "Turkey"));
        address.setAddress(defaultIfBlank(order.getShippingAddressLine(), "N/A"));
        address.setZipCode(defaultIfBlank(order.getShippingPostalCode(), "34000"));
        return address;
    }

    private Address buildBillingAddress(Order order, User user) {
        Address address = new Address();
        address.setContactName(resolveContactName(order, user));
        address.setCity(defaultIfBlank(order.getShippingCity(), "Istanbul"));
        address.setCountry(defaultIfBlank(order.getShippingCountry(), "Turkey"));
        address.setAddress(defaultIfBlank(order.getShippingAddressLine(), "N/A"));
        address.setZipCode(defaultIfBlank(order.getShippingPostalCode(), "34000"));
        return address;
    }

    private List<BasketItem> buildBasketItems(Order order) {
        List<OrderItem> orderItems = order.getItems();
        if (orderItems == null || orderItems.isEmpty()) {
            return List.of();
        }

        long targetTotalMinor = Math.max(0L, toMinorUnits(order.getTotalAmount()));
        List<Long> rawLineTotalsMinor = new ArrayList<>();
        long rawTotalMinor = 0L;

        for (OrderItem orderItem : orderItems) {
            BigDecimal unitPrice = normalizeMoney(orderItem.getUnitPrice());
            BigDecimal lineTotal = unitPrice
                    .multiply(BigDecimal.valueOf(orderItem.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            long minor = Math.max(0L, toMinorUnits(lineTotal));
            rawLineTotalsMinor.add(minor);
            rawTotalMinor += minor;
        }

        List<Long> allocatedLineTotalsMinor = new ArrayList<>();
        for (int index = 0; index < orderItems.size(); index++) {
            allocatedLineTotalsMinor.add(0L);
        }

        if (rawTotalMinor > 0L) {
            long distributedMinor = 0L;
            for (int index = 0; index < orderItems.size() - 1; index++) {
                long proportioned = (targetTotalMinor * rawLineTotalsMinor.get(index)) / rawTotalMinor;
                allocatedLineTotalsMinor.set(index, proportioned);
                distributedMinor += proportioned;
            }
            allocatedLineTotalsMinor.set(orderItems.size() - 1, Math.max(0L, targetTotalMinor - distributedMinor));
        } else if (targetTotalMinor > 0L) {
            allocatedLineTotalsMinor.set(0, targetTotalMinor);
        }

        List<BasketItem> basketItems = new ArrayList<>();
        int sequence = 1;
        for (int index = 0; index < orderItems.size(); index++) {
            OrderItem orderItem = orderItems.get(index);
            BasketItem item = new BasketItem();
            item.setId(order.getId() + "-" + sequence++);
            item.setName(defaultIfBlank(orderItem.getProductNameSnapshot(), "Product"));
            String category = orderItem.getProduct() != null ? orderItem.getProduct().getCategory() : null;
            item.setCategory1(defaultIfBlank(category, "General"));
            item.setItemType(BasketItemType.PHYSICAL.name());
            item.setPrice(fromMinorUnits(allocatedLineTotalsMinor.get(index)));
            basketItems.add(item);
        }
        return basketItems;
    }

    private String resolveContactName(Order order, User user) {
        String shippingFullName = trimToNull(order.getShippingFullName());
        if (shippingFullName != null) {
            return shippingFullName;
        }
        String firstName = defaultIfBlank(user.getFirstName(), "");
        String lastName = defaultIfBlank(user.getLastName(), "");
        String combined = (firstName + " " + lastName).trim();
        return combined.isEmpty() ? "Customer User" : combined;
    }

    private String normalizeLocale(String locale) {
        if ("en".equalsIgnoreCase(locale)) {
            return "en";
        }
        return "tr";
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private long toMinorUnits(BigDecimal value) {
        return normalizeMoney(value).movePointRight(2).longValue();
    }

    private BigDecimal fromMinorUnits(long value) {
        return BigDecimal.valueOf(value, 2).setScale(2, RoundingMode.HALF_UP);
    }

    private String defaultIfBlank(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed == null ? fallback : trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
