package com.ordermgr.service;

import com.ordermgr.dto.ProductCreateRequest;
import com.ordermgr.dto.ProductDto;

import java.util.List;

public interface ProductService {
    ProductDto createProduct(ProductCreateRequest request);
    ProductDto getProduct(Long productId);
    List<ProductDto> getAllProducts();
    ProductDto updateProduct(Long productId, ProductCreateRequest request);
    void deleteProduct(Long productId);
}
