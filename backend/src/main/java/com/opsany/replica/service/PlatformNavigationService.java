package com.opsany.replica.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.opsany.replica.domain.PlatformNavigationGroup;
import com.opsany.replica.domain.PlatformNavigationItem;
import com.opsany.replica.dto.NavigationConfigResponse;
import com.opsany.replica.dto.NavigationFavoriteState;
import com.opsany.replica.dto.NavigationGroupDto;
import com.opsany.replica.dto.NavigationGroupSaveRequest;
import com.opsany.replica.dto.NavigationItemDto;
import com.opsany.replica.dto.NavigationItemSaveRequest;
import com.opsany.replica.repository.PlatformNavigationRepository;
import com.opsany.replica.security.SessionUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlatformNavigationService {

    private final PlatformNavigationRepository platformNavigationRepository;

    public NavigationConfigResponse getNavigationConfig(Long userId) {
        return new NavigationConfigResponse(buildGroups(userId, true));
    }

    public List<NavigationGroupDto> getNavigationGroups(Long userId) {
        return buildGroups(userId, false);
    }

    public NavigationGroupDto saveGroup(NavigationGroupSaveRequest request) {
        String groupCode = StringUtils.hasText(request.getGroupCode()) ? request.getGroupCode() : nextGroupCode(request.getTitle());
        PlatformNavigationGroup group = PlatformNavigationGroup.builder()
            .groupCode(groupCode)
            .title(defaultIfBlank(request.getTitle(), "未命名分组"))
            .sortNo(request.getSortNo() == null ? nextGroupSortNo() : request.getSortNo())
            .build();

        if (platformNavigationRepository.countByGroupCode(groupCode) == 0) {
            platformNavigationRepository.insertGroup(group);
        } else {
            platformNavigationRepository.updateGroup(group);
            group = findGroupByCode(groupCode);
        }
        return new NavigationGroupDto(group.getId(), group.getGroupCode(), group.getTitle(), group.getSortNo(), new ArrayList<NavigationItemDto>());
    }

    public NavigationItemDto saveItem(NavigationItemSaveRequest request, SessionUser sessionUser) {
        String itemCode = StringUtils.hasText(request.getItemCode()) ? request.getItemCode() : nextItemCode(request.getName());
        PlatformNavigationItem item = PlatformNavigationItem.builder()
            .itemCode(itemCode)
            .groupCode(defaultIfBlank(request.getGroupCode(), firstGroupCode()))
            .name(defaultIfBlank(request.getName(), "未命名导航"))
            .icon(defaultIfBlank(request.getIcon(), firstCharacter(request.getName(), "导")))
            .creatorUsername(sessionUser.getUsername())
            .creatorDisplayName(sessionUser.getDisplayName())
            .link(defaultIfBlank(request.getLink(), "/"))
            .mobileVisible(request.getMobileVisible() != null && request.getMobileVisible())
            .description(defaultIfBlank(request.getDescription(), "平台导航入口"))
            .sortNo(request.getSortNo() == null ? nextItemSortNo() : request.getSortNo())
            .enabled(request.getEnabled() == null || request.getEnabled())
            .build();

        if (platformNavigationRepository.countByItemCode(itemCode) == 0) {
            platformNavigationRepository.insertItem(item);
        } else {
            platformNavigationRepository.updateItem(item);
            item = findItemByCode(itemCode);
        }
        return toItemDto(item, findGroupByCode(item.getGroupCode()), false);
    }

    public void deleteItem(String itemCode) {
        if (!StringUtils.hasText(itemCode)) {
            return;
        }
        platformNavigationRepository.deleteFavoritesByItemCode(itemCode);
        platformNavigationRepository.deleteItemByCode(itemCode);
    }

    public NavigationFavoriteState toggleFavorite(Long userId, String itemCode) {
        if (platformNavigationRepository.countFavorite(userId, itemCode) > 0) {
            platformNavigationRepository.deleteFavorite(userId, itemCode);
            return new NavigationFavoriteState(itemCode, false);
        }
        Integer maxSortNo = platformNavigationRepository.findMaxFavoriteSortNo(userId);
        platformNavigationRepository.insertFavorite(userId, itemCode, (maxSortNo == null ? 0 : maxSortNo) + 1);
        return new NavigationFavoriteState(itemCode, true);
    }

    private List<NavigationGroupDto> buildGroups(Long userId, boolean includeDisabled) {
        List<PlatformNavigationGroup> groups = platformNavigationRepository.findAllGroups();
        List<NavigationItemDto> items = platformNavigationRepository.findAllItemDtos();
        Set<String> favoriteCodes = new LinkedHashSet<String>(platformNavigationRepository.findFavoriteItemCodesByUserId(userId));
        Map<String, NavigationGroupDto> grouped = new LinkedHashMap<String, NavigationGroupDto>();

        for (PlatformNavigationGroup group : groups) {
            grouped.put(group.getGroupCode(), new NavigationGroupDto(group.getId(), group.getGroupCode(), group.getTitle(), group.getSortNo(), new ArrayList<NavigationItemDto>()));
        }

        for (NavigationItemDto item : items) {
            if (!includeDisabled && !Boolean.TRUE.equals(item.getEnabled())) {
                continue;
            }
            item.setFavorite(favoriteCodes.contains(item.getItemCode()));
            NavigationGroupDto group = grouped.get(item.getGroupCode());
            if (group == null) {
                group = new NavigationGroupDto(null, item.getGroupCode(), defaultIfBlank(item.getGroupTitle(), "未分组"), item.getGroupSortNo(), new ArrayList<NavigationItemDto>());
                grouped.put(item.getGroupCode(), group);
            }
            group.getRows().add(item);
        }

        return new ArrayList<NavigationGroupDto>(grouped.values());
    }

    private PlatformNavigationGroup findGroupByCode(String groupCode) {
        for (PlatformNavigationGroup group : platformNavigationRepository.findAllGroups()) {
            if (groupCode.equals(group.getGroupCode())) {
                return group;
            }
        }
        return PlatformNavigationGroup.builder()
            .groupCode(groupCode)
            .title(groupCode)
            .sortNo(0)
            .build();
    }

    private PlatformNavigationItem findItemByCode(String itemCode) {
        for (NavigationItemDto item : platformNavigationRepository.findAllItemDtos()) {
            if (itemCode.equals(item.getItemCode())) {
                return PlatformNavigationItem.builder()
                    .id(item.getId())
                    .itemCode(item.getItemCode())
                    .groupCode(item.getGroupCode())
                    .name(item.getName())
                    .icon(item.getIcon())
                    .creatorUsername(item.getCreatorUsername())
                    .creatorDisplayName(item.getCreatorDisplayName())
                    .link(item.getLink())
                    .mobileVisible(item.getMobileVisible())
                    .description(item.getDescription())
                    .sortNo(item.getSortNo())
                    .enabled(item.getEnabled())
                    .build();
            }
        }
        return PlatformNavigationItem.builder()
            .itemCode(itemCode)
            .groupCode(firstGroupCode())
            .name(itemCode)
            .icon("导")
            .creatorUsername("system")
            .creatorDisplayName("系统")
            .link("/")
            .mobileVisible(false)
            .description("平台导航入口")
            .sortNo(0)
            .enabled(true)
            .build();
    }

    private NavigationItemDto toItemDto(PlatformNavigationItem item, PlatformNavigationGroup group, boolean favorite) {
        return new NavigationItemDto(
            item.getId(),
            item.getItemCode(),
            item.getGroupCode(),
            group.getTitle(),
            group.getSortNo(),
            item.getName(),
            item.getIcon(),
            item.getCreatorUsername(),
            item.getCreatorDisplayName(),
            item.getLink(),
            item.getMobileVisible(),
            item.getDescription(),
            item.getSortNo(),
            item.getEnabled(),
            favorite
        );
    }

    private Integer nextGroupSortNo() {
        int maxSortNo = 0;
        for (PlatformNavigationGroup group : platformNavigationRepository.findAllGroups()) {
            if (group.getSortNo() != null && group.getSortNo() > maxSortNo) {
                maxSortNo = group.getSortNo();
            }
        }
        return maxSortNo + 1;
    }

    private Integer nextItemSortNo() {
        int maxSortNo = 0;
        for (NavigationItemDto item : platformNavigationRepository.findAllItemDtos()) {
            if (item.getSortNo() != null && item.getSortNo() > maxSortNo) {
                maxSortNo = item.getSortNo();
            }
        }
        return maxSortNo + 1;
    }

    private String firstGroupCode() {
        List<PlatformNavigationGroup> groups = platformNavigationRepository.findAllGroups();
        return groups.isEmpty() ? "DEFAULT_GROUP" : groups.get(0).getGroupCode();
    }

    private String nextGroupCode(String title) {
        return nextCode(title, "NAV_GROUP");
    }

    private String nextItemCode(String name) {
        return nextCode(name, "NAV_ITEM");
    }

    private String nextCode(String input, String fallback) {
        String normalized = input == null ? fallback : input.trim().replaceAll("[^A-Za-z0-9]+", "_").toUpperCase();
        if (!StringUtils.hasText(normalized)) {
            normalized = fallback;
        }
        return normalized + "_" + System.currentTimeMillis();
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String firstCharacter(String value, String fallback) {
        return StringUtils.hasText(value) ? value.substring(0, 1) : fallback;
    }
}
