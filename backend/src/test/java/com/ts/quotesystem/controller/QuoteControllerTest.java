package com.ts.quotesystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ts.quotesystem.dto.LoginRequest;
import com.ts.quotesystem.dto.QuoteRequest;
import com.ts.quotesystem.dto.QuoteResponse;
import com.ts.quotesystem.service.QuoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class QuoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QuoteService quoteService;

    private String adminToken;

    @BeforeEach
    public void setup() throws Exception {
        // IT-005 管理者ログイン成功の事前準備
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("admin123");

        MvcResult result = mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        Map<String, String> responseMap = objectMapper.readValue(responseBody, Map.class);
        adminToken = responseMap.get("token");
        assertNotNull(adminToken);
    }

    /**
     * IT-001: POST /api/quotes 正常な見積条件を送信
     */
    @Test
    public void testIt001_CreateQuoteSuccess() throws Exception {
        QuoteRequest request = QuoteRequest.builder()
                .driverAge(30)
                .licenseColor("BLUE")
                .usageType("COMMUTE")
                .annualMileage(7500)
                .driverRange("COUPLE")
                .hasCurrentInsurance(false)
                .maker("スズキ")
                .carName("ハスラー")
                .firstRegistrationYearMonth("2021-08")
                .vehicleType("KEI")
                .vehicleInsurance(true)
                .propertyDamageLimit("UNLIMITED")
                .personalInjuryAmount("FIFTY_MILLION")
                .lawyerOption(true)
                .roadService(true)
                .build();

        MvcResult result = mockMvc.perform(post("/api/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        QuoteResponse response = objectMapper.readValue(content, QuoteResponse.class);
        assertNotNull(response.getQuoteNo());
        assertNotNull(response.getAnnualPremium());
    }

    /**
     * IT-002: POST /api/quotes 必須項目・相関項目バリデーションエラー
     */
    @Test
    public void testIt002_CreateQuoteValidationError() throws Exception {
        // 年齢17歳 (18歳未満) + 必須項目欠落
        QuoteRequest request = QuoteRequest.builder()
                .driverAge(17) // エラー
                .licenseColor("GOLD")
                .usageType("PRIVATE")
                .annualMileage(12000)
                .driverRange("SELF")
                .hasCurrentInsurance(true)
                // gradeとaccidentTermが未指定 (hasCurrentInsurance=true なのに欠落) -> エラー
                .maker("トヨタ")
                .carName("") // エラー
                .firstRegistrationYearMonth("2020-01")
                .vehicleType("SEDAN")
                .vehicleInsurance(false)
                .propertyDamageLimit("UNLIMITED")
                .personalInjuryAmount("UNLIMITED")
                .lawyerOption(false)
                .roadService(false)
                .build();

        mockMvc.perform(post("/api/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // 400 Bad Request
    }

    /**
     * IT-003 / IT-004: GET /api/quotes/{quoteNo} 正常検索と404検出
     */
    @Test
    public void testIt003AndIt004_GetQuoteByNo() throws Exception {
        // 事前にデータを1件作成
        QuoteRequest request = QuoteRequest.builder()
                .driverAge(40)
                .licenseColor("GOLD")
                .usageType("PRIVATE")
                .annualMileage(2000)
                .driverRange("SELF")
                .hasCurrentInsurance(false)
                .maker("日産")
                .carName("ノート")
                .firstRegistrationYearMonth("2019-12")
                .vehicleType("COMPACT")
                .vehicleInsurance(false)
                .propertyDamageLimit("THIRTY_MILLION")
                .personalInjuryAmount("THIRTY_MILLION")
                .lawyerOption(false)
                .roadService(false)
                .build();

        QuoteResponse created = quoteService.createQuote(request);
        String quoteNo = created.getQuoteNo();

        // IT-003: 存在する見積番号の取得 (200 OK)
        mockMvc.perform(get("/api/quotes/" + quoteNo))
                .andExpect(status().isOk());

        // IT-004: 存在しない見積番号の取得 (404 Not Found)
        mockMvc.perform(get("/api/quotes/EST999999990000"))
                .andExpect(status().isNotFound());
    }

    /**
     * IT-006: GET /api/admin/quotes 未ログイン (401 Unauthorized)
     */
    @Test
    public void testIt006_GetQuotesUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/quotes"))
                .andExpect(status().isUnauthorized()); // 401
    }

    /**
     * IT-007: GET /api/admin/quotes ログイン済み・検索条件付き (200 OK)
     */
    @Test
    public void testIt007_GetQuotesWithQuery() throws Exception {
        mockMvc.perform(get("/api/admin/quotes")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("maker", "トヨタ"))
                .andExpect(status().isOk());
    }

    /**
     * IT-008: GET /api/admin/quotes.csv ログイン済み CSVダウンロード (200 OK)
     */
    @Test
    public void testIt008_ExportCsvSuccess() throws Exception {
        mockMvc.perform(get("/api/admin/quotes.csv")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
    }
}
