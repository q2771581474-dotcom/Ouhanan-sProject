package com.ts.quotesystem.service;

import com.ts.quotesystem.entity.RateMaster;
import com.ts.quotesystem.repository.RateMasterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 料率マスタ参照サービス
 */
@Service
@Transactional(readOnly = true)
public class MasterService {

    private final RateMasterRepository rateMasterRepository;

    public MasterService(RateMasterRepository rateMasterRepository) {
        this.rateMasterRepository = rateMasterRepository;
    }

    /**
     * 有効なすべての料率マスタレコードを取得する
     * 【設計考量】フロントエンドの初期化時等に最新の有効な選択肢リストを効率的に提供し、無効化されたマスタによる誤入力を防止する。
     */
    public List<RateMaster> getActiveRates() {
        return rateMasterRepository.findByActiveTrue();
    }
}
