package com.ts.quotesystem.controller;

import com.ts.quotesystem.entity.RateMaster;
import com.ts.quotesystem.service.MasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 料率マスタAPIコントローラー
 */
@Tag(name = "料率マスタAPI", description = "管理者用料率設定参照API")
@RestController
@RequestMapping("/api/master")
public class MasterController {

    private final MasterService masterService;

    public MasterController(MasterService masterService) {
        this.masterService = masterService;
    }

    /**
     * 料率マスタ参照 (API-007)
     * 【設計考量】画面側でのセレクトボックス選択肢構築やリアルタイムチェックを支えるため、現在有効なマスタ一覧をHTTP 200 OKで返却する。
     */
    @Operation(summary = "料率設定一覧参照", description = "保険料計算で使用される有効なすべての料率および加算設定を取得する")
    @GetMapping("/rates")
    public ResponseEntity<List<RateMaster>> getActiveRates() {
        List<RateMaster> rates = masterService.getActiveRates();
        return ResponseEntity.ok(rates);
    }
}
