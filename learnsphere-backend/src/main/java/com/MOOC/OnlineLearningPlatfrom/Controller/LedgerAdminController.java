package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.LedgerReconciliationDto;
import com.MOOC.OnlineLearningPlatfrom.Service.LedgerReconciliationService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ledger")
public class LedgerAdminController {

    private final LedgerReconciliationService ledgerReconciliationService;

    public LedgerAdminController(LedgerReconciliationService ledgerReconciliationService) {
        this.ledgerReconciliationService = ledgerReconciliationService;
    }

    @GetMapping("/reconciliation")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LedgerReconciliationDto> getReconciliation() {
        LedgerReconciliationDto response = ledgerReconciliationService.reconcileLedger();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(response);
    }
}

