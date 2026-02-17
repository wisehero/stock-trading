package com.wisehero.stocktrading.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wisehero.stocktrading.order.service.OrderService;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Broker Core + Mock Exchange 주문 흐름 통합 테스트.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderService orderService;

    @Test
    void marketBuyShouldPartialFillThenFullyFillAfterQuoteUpdate() throws Exception {
        long accountId = 1001L;
        String symbol = "TESTA";

        upsertCash(accountId, "1000000.0000");
        upsertQuote(symbol, "1000.0000", "30.0000");

        JsonNode created = createOrder(Map.of(
                "accountId", accountId,
                "idempotencyKey", "market-partial-001",
                "symbol", symbol,
                "side", "BUY",
                "orderType", "LIMIT",
                "tif", "DAY",
                "quantity", "100.0000",
                "limitPrice", "1000.0000"
        ));

        long orderId = created.get("data").get("orderId").asLong();
        assertThat(created.get("data").get("status").asText()).isEqualTo("PARTIALLY_FILLED");
        assertThat(toBigDecimal(created, "data", "filledQuantity")).isEqualByComparingTo("30.0000");
        assertThat(toBigDecimal(created, "data", "remainingQuantity")).isEqualByComparingTo("70.0000");

        upsertQuote(symbol, "1000.0000", "70.0000");

        JsonNode order = getOrder(orderId, accountId);
        assertThat(order.get("data").get("status").asText()).isEqualTo("FILLED");
        assertThat(toBigDecimal(order, "data", "filledQuantity")).isEqualByComparingTo("100.0000");
        assertThat(toBigDecimal(order, "data", "remainingQuantity")).isEqualByComparingTo("0.0000");
    }

    @Test
    void limitBuyShouldStayNewThenFillWhenPriceConditionMatches() throws Exception {
        long accountId = 1002L;
        String symbol = "TESTB";

        upsertCash(accountId, "1000000.0000");
        upsertQuote(symbol, "1000.0000", "100.0000");

        JsonNode created = createOrder(Map.of(
                "accountId", accountId,
                "idempotencyKey", "limit-match-001",
                "symbol", symbol,
                "side", "BUY",
                "orderType", "LIMIT",
                "quantity", "50.0000",
                "limitPrice", "900.0000"
        ));

        long orderId = created.get("data").get("orderId").asLong();
        assertThat(created.get("data").get("status").asText()).isEqualTo("NEW");

        upsertQuote(symbol, "890.0000", "50.0000");

        JsonNode order = getOrder(orderId, accountId);
        assertThat(order.get("data").get("status").asText()).isEqualTo("FILLED");
        assertThat(toBigDecimal(order, "data", "filledQuantity")).isEqualByComparingTo("50.0000");
    }

    @Test
    void partialFilledOrderShouldCancelRemainingQuantity() throws Exception {
        long accountId = 1003L;
        String symbol = "TESTC";

        upsertCash(accountId, "1000000.0000");
        upsertQuote(symbol, "500.0000", "40.0000");

        JsonNode created = createOrder(Map.of(
                "accountId", accountId,
                "idempotencyKey", "cancel-after-partial-001",
                "symbol", symbol,
                "side", "BUY",
                "orderType", "LIMIT",
                "tif", "DAY",
                "quantity", "100.0000",
                "limitPrice", "500.0000"
        ));

        long orderId = created.get("data").get("orderId").asLong();
        assertThat(created.get("data").get("status").asText()).isEqualTo("PARTIALLY_FILLED");

        JsonNode canceled = cancelOrder(orderId, accountId);
        assertThat(canceled.get("data").get("status").asText()).isEqualTo("CANCELED");
        assertThat(toBigDecimal(canceled, "data", "remainingQuantity")).isEqualByComparingTo("60.0000");
    }

    @Test
    void duplicateIdempotencyKeyShouldReturnSameOrder() throws Exception {
        long accountId = 1004L;
        String symbol = "TESTD";

        upsertCash(accountId, "1000000.0000");
        upsertQuote(symbol, "100.0000", "100.0000");

        Map<String, Object> request = Map.of(
                "accountId", accountId,
                "idempotencyKey", "idem-001",
                "symbol", symbol,
                "side", "BUY",
                "orderType", "MARKET",
                "quantity", "10.0000"
        );

        JsonNode first = createOrder(request);
        JsonNode second = createOrder(request);

        assertThat(second.get("data").get("orderId").asLong())
                .isEqualTo(first.get("data").get("orderId").asLong());
    }

    @Test
    void marketOrderShouldRejectNonIocTif() throws Exception {
        long accountId = 1005L;
        String symbol = "TESTE";

        upsertCash(accountId, "1000000.0000");
        upsertQuote(symbol, "1000.0000", "100.0000");

        JsonNode response = createOrderExpectStatus(Map.of(
                "accountId", accountId,
                "idempotencyKey", "market-invalid-tif-001",
                "symbol", symbol,
                "side", "BUY",
                "orderType", "MARKET",
                "tif", "DAY",
                "quantity", "10.0000"
        ), HttpStatus.BAD_REQUEST.value());

        assertThat(response.get("code").asText()).isEqualTo("ORDER-410");
    }

    @Test
    void quantityShouldRejectFractionalValue() throws Exception {
        long accountId = 1006L;
        String symbol = "TESTF";

        upsertCash(accountId, "1000000.0000");
        upsertQuote(symbol, "1000.0000", "100.0000");

        JsonNode response = createOrderExpectStatus(Map.of(
                "accountId", accountId,
                "idempotencyKey", "quantity-unit-001",
                "symbol", symbol,
                "side", "BUY",
                "orderType", "LIMIT",
                "quantity", "10.5000",
                "limitPrice", "1000.0000"
        ), HttpStatus.BAD_REQUEST.value());

        assertThat(response.get("code").asText()).isEqualTo("ORDER-411");
    }

    @Test
    void limitIocShouldCancelRemainingImmediately() throws Exception {
        long accountId = 1007L;
        String symbol = "TESTG";

        upsertCash(accountId, "1000000.0000");
        upsertQuote(symbol, "1000.0000", "30.0000");

        JsonNode created = createOrder(Map.of(
                "accountId", accountId,
                "idempotencyKey", "limit-ioc-001",
                "symbol", symbol,
                "side", "BUY",
                "orderType", "LIMIT",
                "tif", "IOC",
                "quantity", "100.0000",
                "limitPrice", "1100.0000"
        ));

        assertThat(created.get("data").get("status").asText()).isEqualTo("CANCELED");
        assertThat(toBigDecimal(created, "data", "filledQuantity")).isEqualByComparingTo("30.0000");
        assertThat(toBigDecimal(created, "data", "remainingQuantity")).isEqualByComparingTo("70.0000");
    }

    @Test
    void limitFokShouldCancelWhenNotFullyMatchable() throws Exception {
        long accountId = 1008L;
        String symbol = "TESTH";

        upsertCash(accountId, "1000000.0000");
        upsertQuote(symbol, "1000.0000", "30.0000");

        JsonNode created = createOrder(Map.of(
                "accountId", accountId,
                "idempotencyKey", "limit-fok-001",
                "symbol", symbol,
                "side", "BUY",
                "orderType", "LIMIT",
                "tif", "FOK",
                "quantity", "100.0000",
                "limitPrice", "1100.0000"
        ));

        assertThat(created.get("data").get("status").asText()).isEqualTo("CANCELED");
        assertThat(toBigDecimal(created, "data", "filledQuantity")).isEqualByComparingTo("0.0000");
        assertThat(toBigDecimal(created, "data", "remainingQuantity")).isEqualByComparingTo("100.0000");
    }

    @Test
    void amendLimitOrderShouldReduceRemainingAndUpdatePrice() throws Exception {
        long accountId = 1009L;
        String symbol = "TESTI";

        upsertCash(accountId, "1000000.0000");
        upsertQuote(symbol, "1300.0000", "100.0000");

        JsonNode created = createOrder(Map.of(
                "accountId", accountId,
                "idempotencyKey", "amend-001",
                "symbol", symbol,
                "side", "BUY",
                "orderType", "LIMIT",
                "tif", "DAY",
                "quantity", "100.0000",
                "limitPrice", "1000.0000"
        ));
        assertThat(created.get("data").get("status").asText()).isEqualTo("NEW");

        long orderId = created.get("data").get("orderId").asLong();
        JsonNode amended = amendOrder(orderId, Map.of(
                "accountId", accountId,
                "amendedRemainingQuantity", "60.0000",
                "amendedLimitPrice", "1350.0000"
        ));

        assertThat(amended.get("data").get("status").asText()).isEqualTo("FILLED");
        assertThat(toBigDecimal(amended, "data", "quantity")).isEqualByComparingTo("60.0000");
        assertThat(toBigDecimal(amended, "data", "filledQuantity")).isEqualByComparingTo("60.0000");
    }

    @Test
    void expireDayOrdersShouldExpireOpenOrders() throws Exception {
        long accountId = 1010L;
        String symbol = "TESTJ";

        upsertCash(accountId, "1000000.0000");
        upsertQuote(symbol, "1000.0000", "100.0000");

        JsonNode created = createOrder(Map.of(
                "accountId", accountId,
                "idempotencyKey", "expire-day-001",
                "symbol", symbol,
                "side", "BUY",
                "orderType", "LIMIT",
                "tif", "DAY",
                "quantity", "50.0000",
                "limitPrice", "900.0000"
        ));
        long orderId = created.get("data").get("orderId").asLong();
        assertThat(created.get("data").get("status").asText()).isEqualTo("NEW");

        int expiredCount = orderService.expireDayOrders();
        assertThat(expiredCount).isGreaterThanOrEqualTo(1);

        JsonNode expired = getOrder(orderId, accountId);
        assertThat(expired.get("data").get("status").asText()).isEqualTo("EXPIRED");
    }

    private void upsertCash(long accountId, String availableCash) throws Exception {
        mockMvc.perform(put("/api/v1/mock/accounts/{accountId}/cash", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("availableCash", availableCash))))
                .andExpect(status().isOk());
    }

    private void upsertQuote(String symbol, String price, String availableQuantity) throws Exception {
        mockMvc.perform(put("/api/v1/mock/quotes/{symbol}", symbol)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "price", price,
                                "availableQuantity", availableQuantity
                        ))))
                .andExpect(status().isOk());
    }

    private JsonNode createOrder(Map<String, Object> request) throws Exception {
        return createOrderExpectStatus(request, HttpStatus.CREATED.value());
    }

    private JsonNode createOrderExpectStatus(Map<String, Object> request, int expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(expectedStatus))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode cancelOrder(long orderId, long accountId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("accountId", accountId))))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode getOrder(long orderId, long accountId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/orders/{orderId}", orderId)
                        .queryParam("accountId", String.valueOf(accountId)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode amendOrder(long orderId, Map<String, Object> request) throws Exception {
        MvcResult result = mockMvc.perform(patch("/api/v1/orders/{orderId}", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private BigDecimal toBigDecimal(JsonNode node, String objectField, String valueField) {
        return new BigDecimal(node.get(objectField).get(valueField).asText());
    }
}
