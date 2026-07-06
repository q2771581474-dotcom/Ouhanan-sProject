package com.ts.quotesystem.controller;

import com.ts.quotesystem.dto.QuoteRequest;
import com.ts.quotesystem.dto.QuoteResponse;
import com.ts.quotesystem.service.QuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 一般利用者向け見積APIコントローラー
 */
@Tag(name = "見積API", description = "一般利用者向けの見積作成および取得API")
@RestController
@RequestMapping("/api/quotes")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    /**
     * 見積作成 (API-001)
     * 【設計考量】@ValidによりリクエストDTOの入力値整合性を事前に検証し、作成成功時にはRESTful標準に則りHTTP 201 Createdを返却する。
     */
    @Operation(summary = "新規見積作成", description = "入力パラメータを検証し、年間・月額保険料を算出して保存する")
    @PostMapping
    public ResponseEntity<QuoteResponse> createQuote(@Valid @RequestBody QuoteRequest request) {
        QuoteResponse response = quoteService.createQuote(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 見積結果取得 (API-002)
     * 【設計考量】Optionalの関数型アプローチ(map/orElseGet)を活用し、存在する場合はHTTP 200 OK、未存在時は安全にHTTP 404 Not Foundへ振る舞いを分離する。
     */
    @Operation(summary = "見積結果取得", description = "見積番号を指定して見積計算結果を取得する")
    @GetMapping("/{quoteNo}")
    public ResponseEntity<QuoteResponse> getQuoteByNo(@PathVariable String quoteNo) {
        return quoteService.getQuoteByNo(quoteNo)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
