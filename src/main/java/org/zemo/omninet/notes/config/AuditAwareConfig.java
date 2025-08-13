package org.zemo.omninet.notes.config;


import org.springframework.data.domain.AuditorAware;
import org.zemo.omninet.notes.util.CommonUtil;

import java.util.Optional;

public class AuditAwareConfig implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        if (CommonUtil.getLoggedInUser() != null && CommonUtil.getLoggedInUser().getEmail() != null) {
            return Optional.of(CommonUtil.getLoggedInUser().getEmail());
        }
        return Optional.empty();
    }
}

