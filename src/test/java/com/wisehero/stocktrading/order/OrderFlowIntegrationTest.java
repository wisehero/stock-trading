package com.wisehero.stocktrading.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
                "orderType", "MARKET",
                "quantity", "100.0000"
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
                "orderType", "MARKET",
                "quantity", "100.0000"
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
        MvcResult result = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
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

    private BigDecimal toBigDecimal(JsonNode node, String objectField, String valueField) {
        return new BigDecimal(node.get(objectField).get(valueField).asText());
    }
}
