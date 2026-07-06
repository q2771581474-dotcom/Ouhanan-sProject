package com.ts.quotesystem.controller;

import com.ts.quotesystem.dto.LoginRequest;
import com.ts.quotesystem.dto.LoginResponse;
import com.ts.quotesystem.dto.QuoteResponse;
import com.ts.quotesystem.service.AdminService;
import com.ts.quotesystem.service.QuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 管理者向け管理機能APIコントローラー
 */
@Tag(name = "管理API", description = "管理者認証、見積履歴の検索・詳細・CSV出力API")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final QuoteService quoteService;

    public AdminController(AdminService adminService, QuoteService quoteService) {
        this.adminService = adminService;
        this.quoteService = quoteService;
    }

    /**
     * 管理者ログイン (API-003)
     * 【設計考量】IDとパスワードの認証に成功した場合にJWTを発行し、クライアント側へ返却することで安全なセッション管理を実現する。
     */
    @Operation(summary = "管理者ログイン", description = "IDとパスワードを検証し、JWTトークンを返却する")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = adminService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 見積一覧検索 (API-004)
     * 【設計考量】オプショナルな各種検索クエリ(車名、年齢範囲等)を受け付け、動的フィルタリングを適用した履歴一覧を提供する。
     */
    @Operation(summary = "見積一覧検索", description = "検索条件を指定して見積履歴の一覧を降順で取得する")
    @GetMapping("/quotes")
    public ResponseEntity<List<QuoteResponse>> searchQuotes(
            @RequestParam(required = false) String quoteNo,
            @RequestParam(required = false) String maker,
            @RequestParam(required = false) String carName,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge) {
        List<QuoteResponse> list = quoteService.searchQuotes(quoteNo, maker, carName, minAge, maxAge);
        return ResponseEntity.ok(list);
    }

    /**
     * 管理者用見積詳細 (API-005)
     * 【設計考量】管理者による監査や電話対応を想定し、見積の全入力項目および計算内訳明細を正確に取得する。
     */
    @Operation(summary = "見積詳細参照", description = "見積番号を指定して詳細な入力条件および計算内訳を取得する")
    @GetMapping("/quotes/{quoteNo}")
    public ResponseEntity<QuoteResponse> getAdminQuoteByNo(@PathVariable String quoteNo) {
        return quoteService.getQuoteByNo(quoteNo)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * 見積一覧CSV出力 (API-006)
     * 【設計考量】Excelでの文字化けを防止するためUTF-8 BOMを明示的に付与し、ストリーム出力により大容量データでもメモリ枯渇を防ぐ。
     */
    @Operation(summary = "見積一覧CSVエクスポート", description = "検索条件に合致する見積履歴をCSV形式でダウンロードする")
    @GetMapping("/quotes.csv")
    public void exportQuotesCsv(
            @RequestParam(required = false) String quoteNo,
            @RequestParam(required = false) String maker,
            @RequestParam(required = false) String carName,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            HttpServletResponse response) throws IOException {

        List<QuoteResponse> quotes = quoteService.searchQuotes(quoteNo, maker, carName, minAge, maxAge);

        // ファイル名の生成
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "quotes_" + timestamp + ".csv";

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);

        // 日本語環境(Excel等)での文字化け防止のため、UTF-8 BOMを付与
        response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

        try (OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
            // ヘッダー書き込み
            writer.write("見積番号,メーカー,車名,運転者年齢,免許証色,使用目的,走行距離,運転者範囲,現在加入有無,等級,事故有期間,年間保険料,月額保険料,作成日時\n");

            // データ行書き込み
            for (QuoteResponse q : quotes) {
                String hasInsuranceText = Boolean.TRUE.equals(q.getHasCurrentInsurance()) ? "あり" : "なし";
                String gradeText = q.getGrade() != null ? q.getGrade().toString() + "等級" : "-";
                String termText = q.getAccidentTerm() != null ? q.getAccidentTerm().toString() + "年" : "-";

                writer.write(String.format("%s,%s,%s,%d,%s,%s,%d,%s,%s,%s,%s,%d,%d,%s\n",
                        escapeCsvField(q.getQuoteNo()),
                        escapeCsvField(q.getMaker()),
                        escapeCsvField(q.getCarName()),
                        q.getDriverAge(),
                        escapeCsvField(q.getLicenseColor()),
                        escapeCsvField(q.getUsageType()),
                        q.getAnnualMileage(),
                        escapeCsvField(q.getDriverRange()),
                        hasInsuranceText,
                        gradeText,
                        termText,
                        q.getAnnualPremium(),
                        q.getMonthlyPremium(),
                        q.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                ));
            }
            writer.flush();
        }
    }

    /**
     * 【設計考量】CSV仕様(RFC 4180)に基づき、カンマや改行、引用符を含む文字列を適切にエスケープ処理し、ファイル破損を防止する。
     */
    private String escapeCsvField(String field) {
        if (field == null) return "";
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}
