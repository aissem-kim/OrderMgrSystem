package com.ordermgr.service;

import com.ordermgr.domain.entity.Product;
import com.ordermgr.domain.entity.Stock;
import com.ordermgr.dto.StockDto;
import com.ordermgr.repository.ProductRepository;
import com.ordermgr.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;
    private final ProductRepository productRepository;

    @Override
    public StockDto initializeStock(Long productId, Long quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + productId));

        Stock stock = Stock.builder()
                .product(product)
                .quantity(quantity)
                .build();
        Stock savedStock = stockRepository.save(stock);
        return mapToDto(savedStock);
    }

    @Override
    @Transactional(readOnly = true)
    public StockDto getStock(Long stockId) {
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다. ID: " + stockId));
        return mapToDto(stock);
    }

    @Override
    @Transactional(readOnly = true)
    public StockDto getStockByProductId(Long productId) {
        Stock stock = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품에 해당하는 재고가 없습니다. Product ID: " + productId));
        return mapToDto(stock);
    }

    @Override
    public void reserveStock(Long stockId, Long quantity) {
        Stock stock = stockRepository.findByIdWithLock(stockId)
                .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다. ID: " + stockId));
        stock.reserve(quantity);
        stockRepository.save(stock);
    }

    @Override
    public void cancelReservation(Long stockId, Long quantity) {
        Stock stock = stockRepository.findByIdWithLock(stockId)
                .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다. ID: " + stockId));
        stock.cancelReservation(quantity);
        stockRepository.save(stock);
    }

    @Override
    public void decreaseStock(Long stockId, Long quantity) {
        Stock stock = stockRepository.findByIdWithLock(stockId)
                .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다. ID: " + stockId));
        stock.decreaseQuantity(quantity);
        stockRepository.save(stock);
    }

    @Override
    public void increaseStock(Long stockId, Long quantity) {
        Stock stock = stockRepository.findByIdWithLock(stockId)
                .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다. ID: " + stockId));
        stock.increaseQuantity(quantity);
        stockRepository.save(stock);
    }

    private StockDto mapToDto(Stock stock) {
        return new StockDto(
                stock.getId(),
                stock.getProduct().getId(),
                stock.getQuantity(),
                stock.getReservedQuantity(),
                stock.getAvailableQuantity(),
                stock.getCreatedAt(),
                stock.getUpdatedAt()
        );
    }
}
