package com.xpcjsu.sunshinemall.stock.constant;

public final class StockConstants {

    private StockConstants() {}

    public static final class StockOperationType {
        public static final Integer IN_STOCK = 1;
        public static final Integer DEDUCT = 2;
        public static final Integer LOCK = 3;
        public static final Integer UNLOCK = 4;
        public static final Integer RETURN = 5;
    }

    public static final class Cache {
        public static final String STOCK_PREFIX = "product:stock:";
        public static final long STOCK_CACHE_EXPIRE = 1800L;
    }

    public static final class MQ {
        public static final String STOCK_CHANGE_TOPIC = "stock-change-topic";
    }
}
