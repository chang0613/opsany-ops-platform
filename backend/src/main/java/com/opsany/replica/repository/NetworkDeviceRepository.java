package com.opsany.replica.repository;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.opsany.replica.domain.NetworkDevice;

@Mapper
public interface NetworkDeviceRepository {

    @Insert({
        "insert into monitor_network_devices(",
        "device_code, name, ip, device_type, vendor, protocol, snmp_community, snmp_version,",
        "ssh_username, port, status, description, created_at, updated_at",
        ") values (",
        "#{deviceCode}, #{name}, #{ip}, #{deviceType}, #{vendor}, #{protocol}, #{snmpCommunity}, #{snmpVersion},",
        "#{sshUsername}, #{port}, #{status}, #{description}, now(), now()",
        ")"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(NetworkDevice device);

    @Update({
        "update monitor_network_devices set",
        "name = #{name}, ip = #{ip}, device_type = #{deviceType}, vendor = #{vendor},",
        "protocol = #{protocol}, snmp_community = #{snmpCommunity}, snmp_version = #{snmpVersion},",
        "ssh_username = #{sshUsername}, port = #{port}, status = #{status},",
        "description = #{description}, updated_at = now()",
        "where id = #{id}"
    })
    int update(NetworkDevice device);

    @Update("update monitor_network_devices set status = #{status}, last_collected_at = now(), updated_at = now() where id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Select({
        "select id, device_code, name, ip, device_type, vendor, protocol, snmp_community, snmp_version,",
        "ssh_username, port, status, last_collected_at, description, created_at, updated_at",
        "from monitor_network_devices order by id asc"
    })
    List<NetworkDevice> findAll();

    @Select({
        "select id, device_code, name, ip, device_type, vendor, protocol, snmp_community, snmp_version,",
        "ssh_username, port, status, last_collected_at, description, created_at, updated_at",
        "from monitor_network_devices where id = #{id} limit 1"
    })
    NetworkDevice findById(@Param("id") Long id);

    @Select({
        "select id, device_code, name, ip, device_type, vendor, protocol, snmp_community, snmp_version,",
        "ssh_username, port, status, last_collected_at, description, created_at, updated_at",
        "from monitor_network_devices where device_code = #{deviceCode} limit 1"
    })
    NetworkDevice findByDeviceCode(@Param("deviceCode") String deviceCode);

    @Select("select count(1) from monitor_network_devices")
    long count();

    @Select("select count(1) from monitor_network_devices where status = #{status}")
    long countByStatus(@Param("status") String status);

    @Delete("delete from monitor_network_devices where id = #{id}")
    int deleteById(@Param("id") Long id);
}
