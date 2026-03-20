package com.ordermgr.service;

import com.ordermgr.dto.StockDto;

public interface StockService {
    StockDto initializeStock(Long productId, Long quantity);
    StockDto getStock(Long stockId);
    StockDto getStockByProductId(Long productId);
    void reserveStock(Long stockId, Long quantity);
    void cancelReservation(Long stockId, Long quantity);
    void decreaseStock(Long stockId, Long quantity);
    void increaseStock(Long stockId, Long quantity);
}
