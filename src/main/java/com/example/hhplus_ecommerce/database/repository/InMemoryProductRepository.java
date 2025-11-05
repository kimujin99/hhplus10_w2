package com.example.hhplus_ecommerce.database.repository;

import com.example.hhplus_ecommerce.domain.model.Product;
import com.example.hhplus_ecommerce.domain.repository.ProductRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryProductRepository implements ProductRepository {
    private final Map<Long, Product> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Product findById(Long productId) {
        return storage.get(productId);
    }

    @Override
    public Product save(Product product) {
        if(product.getId() == null){
            product.assignId(idGenerator.getAndIncrement());
            product.onCreate();
            storage.put(product.getId(), product);
        } else {
            product.onUpdate();
            storage.put(product.getId(), product);
        }
        return product;
    }

    @Override
    public List<Product> findAll() {
        return new ArrayList<>(storage.values());
    }
}
