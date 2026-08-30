package com.example.birdgame3;

import java.util.function.Supplier;

final class ShopPurchaseTransaction {
    enum Status {
        SUCCESS,
        INSUFFICIENT_FUNDS,
        FAILED
    }

    record Result(Status status, ShopPackResult receipt, RuntimeException failure) {
        static Result success(ShopPackResult receipt) {
            return new Result(Status.SUCCESS, receipt, null);
        }

        static Result insufficientFunds() {
            return new Result(Status.INSUFFICIENT_FUNDS, null, null);
        }

        static Result failed(RuntimeException failure) {
            return new Result(Status.FAILED, null, failure);
        }
    }

    private ShopPurchaseTransaction() {
    }

    static Result execute(BirdCoinLedger ledger, int cost, Supplier<ShopPackResult> purchase) {
        if (ledger == null || purchase == null) {
            return Result.failed(new IllegalArgumentException("A ledger and purchase action are required"));
        }
        int safeCost = Math.max(0, cost);
        if (!ledger.spend(safeCost)) {
            return Result.insufficientFunds();
        }
        try {
            return Result.success(purchase.get());
        } catch (RuntimeException failure) {
            ledger.refundSpend(safeCost);
            return Result.failed(failure);
        }
    }
}
