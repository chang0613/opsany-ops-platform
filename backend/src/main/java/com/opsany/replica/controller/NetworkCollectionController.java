package com.opsany.replica.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.opsany.replica.domain.CollectionTemplate;
import com.opsany.replica.domain.MetricSnapshot;
import com.opsany.replica.domain.NetworkDevice;
import com.opsany.replica.dto.SaveCollectionTemplateRequest;
import com.opsany.replica.dto.SaveNetworkDeviceRequest;
import com.opsany.replica.dto.TriggerDiscoveryRequest;
import com.opsany.replica.service.NetworkCollectionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/monitor/collection")
@RequiredArgsConstructor
public class NetworkCollectionController {

    private final NetworkCollectionService networkCollectionService;

    // ---- Network Devices ----

    @GetMapping("/devices")
    public List<NetworkDevice> listDevices() {
        return networkCollectionService.listDevices();
    }

    @PostMapping("/devices")
    public NetworkDevice saveDevice(@RequestBody SaveNetworkDeviceRequest request) {
        return networkCollectionService.saveDevice(request);
    }

    @DeleteMapping("/devices/{id}")
    public void deleteDevice(@PathVariable Long id) {
        networkCollectionService.deleteDevice(id);
    }

    @PostMapping("/devices/discover")
    public List<NetworkDevice> discoverDevices(@RequestBody TriggerDiscoveryRequest request) {
        return networkCollectionService.discoverDevices(request);
    }

    @PostMapping("/devices/{id}/collect")
    public NetworkDevice triggerCollection(@PathVariable Long id) {
        return networkCollectionService.triggerCollection(id);
    }

    @GetMapping("/devices/{id}/metrics")
    public List<MetricSnapshot> getDeviceMetrics(@PathVariable Long id) {
        return networkCollectionService.getDeviceMetrics(id);
    }

    @GetMapping("/metrics/recent")
    public List<MetricSnapshot> getRecentMetrics() {
        return networkCollectionService.getRecentMetrics();
    }

    // ---- Collection Templates ----

    @GetMapping("/templates")
    public List<CollectionTemplate> listTemplates() {
        return networkCollectionService.listTemplates();
    }

    @PostMapping("/templates")
    public CollectionTemplate saveTemplate(@RequestBody SaveCollectionTemplateRequest request) {
        return networkCollectionService.saveTemplate(request);
    }

    @DeleteMapping("/templates/{id}")
    public void deleteTemplate(@PathVariable Long id) {
        networkCollectionService.deleteTemplate(id);
    }
}
