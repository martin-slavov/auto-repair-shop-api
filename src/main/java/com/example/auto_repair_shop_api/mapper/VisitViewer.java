package com.example.auto_repair_shop_api.mapper;

import com.example.auto_repair_shop_api.model.AppUser;
import com.example.auto_repair_shop_api.model.enums.Role;

public record VisitViewer(Role role, Long userId) {

    public static final VisitViewer ADMIN = new VisitViewer(Role.ADMIN, null);

    public static VisitViewer of(AppUser user) {
        return new VisitViewer(user.getRole(), user.getId());
    }
}
