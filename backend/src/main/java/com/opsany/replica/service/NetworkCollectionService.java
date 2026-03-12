package com.opsany.replica.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.opsany.replica.domain.CollectionTemplate;
import com.opsany.replica.domain.MetricSnapshot;
import com.opsany.replica.domain.NetworkDevice;
import com.opsany.replica.dto.SaveCollectionTemplateRequest;
import com.opsany.replica.dto.SaveNetworkDeviceRequest;
import com.opsany.replica.dto.TriggerDiscoveryRequest;
import com.opsany.replica.repository.CollectionTemplateRepository;
import com.opsany.replica.repository.MetricSnapshotRepository;
import com.opsany.replica.repository.NetworkDeviceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NetworkCollectionService {

    private final NetworkDeviceRepository networkDeviceRepository;
    private final CollectionTemplateRepository collectionTemplateRepository;
    private final MetricSnapshotRepository metricSnapshotRepository;

    public List<NetworkDevice> listDevices() {
        return networkDeviceRepository.findAll();
    }

    public NetworkDevice saveDevice(SaveNetworkDeviceRequest request) {
        NetworkDevice existing = request.getId() == null ? null : networkDeviceRepository.findById(request.getId());
        String deviceCode = StringUtils.hasText(request.getDeviceCode())
            ? request.getDeviceCode()
            : (existing != null ? existing.getDeviceCode() : "DEV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        NetworkDevice device = NetworkDevice.builder()
            .id(existing == null ? null : existing.getId())
            .deviceCode(deviceCode)
            .name(defaultIfBlank(request.getName(), "未命名设备"))
            .ip(defaultIfBlank(request.getIp(), "0.0.0.0"))
            .deviceType(defaultIfBlank(request.getDeviceType(), "交换机"))
            .vendor(defaultIfBlank(request.getVendor(), "未知"))
            .protocol(defaultIfBlank(request.getProtocol(), "SNMP"))
            .snmpCommunity(request.getSnmpCommunity())
            .snmpVersion(defaultIfBlank(request.getSnmpVersion(), "v2c"))
            .sshUsername(request.getSshUsername())
            .port(request.getPort() == null ? defaultPort(request.getProtocol()) : request.getPort())
            .status(defaultIfBlank(request.getStatus(), "待采集"))
            .description(request.getDescription())
            .build();

        if (existing == null) {
            networkDeviceRepository.insert(device);
            return device;
        }
        networkDeviceRepository.update(device);
        return networkDeviceRepository.findById(device.getId());
    }

    public void deleteDevice(Long id) {
        NetworkDevice device = networkDeviceRepository.findById(id);
        if (device == null) {
            throw new IllegalArgumentException("设备不存在");
        }
        networkDeviceRepository.deleteById(id);
    }

    public List<NetworkDevice> discoverDevices(TriggerDiscoveryRequest request) {
        // Simulated discovery: in production this would initiate SNMP/SSH scans
        String subnet = defaultIfBlank(request.getSubnet(), "192.168.1.0/24");
        String protocol = defaultIfBlank(request.getProtocol(), "SNMP");
        String community = defaultIfBlank(request.getSnmpCommunity(), "public");
        int port = request.getPort() == null ? defaultPort(protocol) : request.getPort();

        List<NetworkDevice> discovered = new ArrayList<>();
        String[] sampleIps = { subnet.replaceAll("/\\d+", ".1"), subnet.replaceAll("/\\d+", ".2"), subnet.replaceAll("/\\d+", ".254") };
        String[] sampleTypes = { "路由器", "交换机", "防火墙" };
        String[] sampleVendors = { "Cisco", "Huawei", "H3C" };

        for (int i = 0; i < sampleIps.length; i++) {
            String ip = sampleIps[i];
            if (networkDeviceRepository.findAll().stream().anyMatch(d -> ip.equals(d.getIp()))) {
                continue;
            }
            NetworkDevice device = NetworkDevice.builder()
                .deviceCode("DISC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .name(sampleVendors[i] + "-" + sampleTypes[i])
                .ip(ip)
                .deviceType(sampleTypes[i])
                .vendor(sampleVendors[i])
                .protocol(protocol)
                .snmpCommunity(community)
                .snmpVersion("v2c")
                .port(port)
                .status("已发现")
                .description("自动发现于子网 " + subnet)
                .build();
            networkDeviceRepository.insert(device);
            discovered.add(device);
        }
        return discovered;
    }

    public NetworkDevice triggerCollection(Long deviceId) {
        NetworkDevice device = networkDeviceRepository.findById(deviceId);
        if (device == null) {
            throw new IllegalArgumentException("设备不存在");
        }
        // Simulated metric collection
        String[] metricTypes = { "interface_traffic", "error_rate", "cpu_usage", "mem_usage", "bgp_status", "ospf_status" };
        String[] metricNames = { "接口流量(Mbps)", "错包率(%)", "CPU利用率(%)", "内存利用率(%)", "BGP邻居状态", "OSPF邻居状态" };
        String[] metricValues = { "125.6", "0.02", "38", "62", "Established", "Full" };

        for (int i = 0; i < metricTypes.length; i++) {
            MetricSnapshot snapshot = MetricSnapshot.builder()
                .deviceId(device.getId())
                .deviceCode(device.getDeviceCode())
                .metricType(metricTypes[i])
                .metricName(metricNames[i])
                .metricValue(metricValues[i])
                .build();
            metricSnapshotRepository.insert(snapshot);
        }

        networkDeviceRepository.updateStatus(deviceId, "采集正常");
        return networkDeviceRepository.findById(deviceId);
    }

    public List<MetricSnapshot> getDeviceMetrics(Long deviceId) {
        return metricSnapshotRepository.findRecentByDeviceId(deviceId);
    }

    public List<MetricSnapshot> getRecentMetrics() {
        return metricSnapshotRepository.findRecent();
    }

    public List<CollectionTemplate> listTemplates() {
        return collectionTemplateRepository.findAll();
    }

    public CollectionTemplate saveTemplate(SaveCollectionTemplateRequest request) {
        CollectionTemplate existing = request.getId() == null ? null : collectionTemplateRepository.findById(request.getId());
        String templateCode = StringUtils.hasText(request.getTemplateCode())
            ? request.getTemplateCode()
            : (existing != null ? existing.getTemplateCode() : "TPL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        CollectionTemplate template = CollectionTemplate.builder()
            .id(existing == null ? null : existing.getId())
            .templateCode(templateCode)
            .name(defaultIfBlank(request.getName(), "未命名模板"))
            .deviceType(defaultIfBlank(request.getDeviceType(), "通用"))
            .protocol(defaultIfBlank(request.getProtocol(), "SNMP"))
            .metricsJson(StringUtils.hasText(request.getMetricsJson()) ? request.getMetricsJson() : defaultMetricsJson())
            .intervalSeconds(request.getIntervalSeconds() == null ? 300 : request.getIntervalSeconds())
            .enabled(request.getEnabled() == null ? true : request.getEnabled())
            .description(request.getDescription())
            .build();

        if (existing == null) {
            collectionTemplateRepository.insert(template);
            return template;
        }
        collectionTemplateRepository.update(template);
        return collectionTemplateRepository.findById(template.getId());
    }

    public void deleteTemplate(Long id) {
        CollectionTemplate template = collectionTemplateRepository.findById(id);
        if (template == null) {
            throw new IllegalArgumentException("模板不存在");
        }
        collectionTemplateRepository.deleteById(id);
    }

    public long countDevices() {
        return networkDeviceRepository.count();
    }

    public long countDevicesByStatus(String status) {
        return networkDeviceRepository.countByStatus(status);
    }

    public long countTemplates() {
        return collectionTemplateRepository.count();
    }

    private int defaultPort(String protocol) {
        if ("SSH".equalsIgnoreCase(protocol)) {
            return 22;
        }
        return 161;
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String defaultMetricsJson() {
        return "[{\"oid\":\"1.3.6.1.2.1.2.2.1.10\",\"name\":\"接口流量(Mbps)\",\"type\":\"interface_traffic\"},"
            + "{\"oid\":\"1.3.6.1.2.1.2.2.1.14\",\"name\":\"错包率(%)\",\"type\":\"error_rate\"},"
            + "{\"oid\":\"1.3.6.1.4.1.9.9.109.1.1.1.1.6\",\"name\":\"CPU利用率(%)\",\"type\":\"cpu_usage\"},"
            + "{\"oid\":\"1.3.6.1.4.1.9.9.48.1.1.1.5\",\"name\":\"内存利用率(%)\",\"type\":\"mem_usage\"},"
            + "{\"oid\":\"1.3.6.1.2.1.15.3.1.2\",\"name\":\"BGP邻居状态\",\"type\":\"bgp_status\"},"
            + "{\"oid\":\"1.3.6.1.2.1.14.10.1.6\",\"name\":\"OSPF邻居状态\",\"type\":\"ospf_status\"}]";
    }
}
