package com.blog.api.service;

import com.blog.api.dto.SiteBrandingResolved;
import com.blog.api.dto.SiteBrandingUpdateRequest;
import com.blog.api.entity.SiteBranding;

public interface SiteBrandingService {

    SiteBrandingResolved resolvePublic();

    SiteBrandingResolved getForAdmin();

    SiteBranding save(SiteBrandingUpdateRequest req);
}
