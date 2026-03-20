package com.ordermgr.controller;

import com.ordermgr.dto.StockDto;
import com.ordermgr.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @PostMapping("/{productId}")
    public ResponseEntity<StockDto> initializeStock(
            @PathVariable Long productId,
            @RequestParam Long quantity) {
        StockDto stock = stockService.initializeStock(productId, quantity);
        return ResponseEntity.status(HttpStatus.CREATED).body(stock);
    }

    @GetMapping("/{stockId}")
    public ResponseEntity<StockDto> getStock(@PathVariable Long stockId) {
        StockDto stock = stockService.getStock(stockId);
        return ResponseEntity.ok(stock);
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<StockDto> getStockByProductId(@PathVariable Long productId) {
        StockDto stock = stockService.getStockByProductId(productId);
        return ResponseEntity.ok(stock);
    }

    @PostMapping("/{stockId}/reserve")
    public ResponseEntity<Void> reserveStock(
            @PathVariable Long stockId,
            @RequestParam Long quantity) {
        stockService.reserveStock(stockId, quantity);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{stockId}/cancel-reservation")
    public ResponseEntity<Void> cancelReservation(
            @PathVariable Long stockId,
            @RequestParam Long quantity) {
        stockService.cancelReservation(stockId, quantity);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{stockId}/decrease")
    public ResponseEntity<Void> decreaseStock(
            @PathVariable Long stockId,
            @RequestParam Long quantity) {
        stockService.decreaseStock(stockId, quantity);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{stockId}/increase")
    public ResponseEntity<Void> increaseStock(
            @PathVariable Long stockId,
            @RequestParam Long quantity) {
        stockService.increaseStock(stockId, quantity);
        return ResponseEntity.ok().build();
    }
}
