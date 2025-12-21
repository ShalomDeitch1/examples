package com.example.localdelivery.optimized.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.localdelivery.optimized.dao.ReadDao;
import com.example.localdelivery.optimized.repository.WarehouseGeoRepository;

@Component
public class WarehouseGeoIndexer implements CommandLineRunner {

    private final ReadDao readDao;
    private final WarehouseGeoRepository geoRepository;

    public WarehouseGeoIndexer(ReadDao readDao, WarehouseGeoRepository geoRepository) {
        this.readDao = readDao;
        this.geoRepository = geoRepository;
    }

    @Override
    public void run(String... args) {
        geoRepository.clearAndIndex(readDao.findWarehouses());
    }
}
