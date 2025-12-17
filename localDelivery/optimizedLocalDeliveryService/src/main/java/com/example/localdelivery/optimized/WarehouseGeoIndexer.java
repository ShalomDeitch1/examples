package com.example.localdelivery.optimized;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

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
