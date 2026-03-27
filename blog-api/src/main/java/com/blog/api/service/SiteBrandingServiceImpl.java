package com.blog.api.service;

import com.blog.api.config.BlogSiteProperties;
import com.blog.api.dto.SiteBrandingResolved;
import com.blog.api.dto.SiteBrandingUpdateRequest;
import com.blog.api.entity.SiteBranding;
import com.blog.api.repository.SiteBrandingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SiteBrandingServiceImpl implements SiteBrandingService {

    private final SiteBrandingRepository siteBrandingRepository;
    private final BlogSiteProperties blogSiteProperties;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onReadyEnsureSingleton() {
        ensureSingletonExists();
    }

    private void ensureSingletonExists() {
        if (siteBrandingRepository.count() == 0) {
            siteBrandingRepository.save(SiteBranding.builder()
                    .id(SiteBranding.SINGLETON_ID)
                    .build());
        }
    }

    private SiteBranding loadOrEmpty() {
        return siteBrandingRepository.findById(SiteBranding.SINGLETON_ID).orElse(null);
    }

    private String pick(String dbVal, String fallback) {
        if (dbVal != null && !dbVal.isBlank()) {
            return dbVal.trim();
        }
        return fallback == null ? "" : fallback;
    }

    @Override
    @Transactional(readOnly = true)
    public SiteBrandingResolved resolvePublic() {
        SiteBranding b = loadOrEmpty();
        return SiteBrandingResolved.builder()
                .navTitle(pick(b != null ? b.getNavTitle() : null, blogSiteProperties.getNavTitle()))
                .navLogoUrl(pick(b != null ? b.getNavLogoUrl() : null, blogSiteProperties.getNavLogoUrl()))
                .heroTitle(pick(b != null ? b.getHeroTitle() : null, blogSiteProperties.getHeroTitle()))
                .heroSubtitle(pick(b != null ? b.getHeroSubtitle() : null, blogSiteProperties.getHeroSubtitle()))
                .authorName(pick(b != null ? b.getAuthorName() : null, blogSiteProperties.getAuthorName()))
                .authorBio(pick(b != null ? b.getAuthorBio() : null, blogSiteProperties.getAuthorBio()))
                .authorAvatar(pick(b != null ? b.getAuthorAvatar() : null, blogSiteProperties.getAuthorAvatar()))
                .footerText(pick(b != null ? b.getFooterText() : null, blogSiteProperties.getFooterText()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SiteBrandingResolved getForAdmin() {
        return resolvePublic();
    }

    @Override
    @Transactional
    public SiteBranding save(SiteBrandingUpdateRequest req) {
        ensureSingletonExists();
        SiteBranding b = siteBrandingRepository.findById(SiteBranding.SINGLETON_ID)
                .orElseGet(() -> SiteBranding.builder().id(SiteBranding.SINGLETON_ID).build());
        if (req.getNavTitle() != null) {
            b.setNavTitle(req.getNavTitle().trim().isEmpty() ? null : req.getNavTitle().trim());
        }
        if (req.getNavLogoUrl() != null) {
            b.setNavLogoUrl(req.getNavLogoUrl().trim().isEmpty() ? null : req.getNavLogoUrl().trim());
        }
        if (req.getHeroTitle() != null) {
            b.setHeroTitle(req.getHeroTitle().trim().isEmpty() ? null : req.getHeroTitle().trim());
        }
        if (req.getHeroSubtitle() != null) {
            b.setHeroSubtitle(req.getHeroSubtitle().trim().isEmpty() ? null : req.getHeroSubtitle().trim());
        }
        if (req.getAuthorName() != null) {
            b.setAuthorName(req.getAuthorName().trim().isEmpty() ? null : req.getAuthorName().trim());
        }
        if (req.getAuthorBio() != null) {
            b.setAuthorBio(req.getAuthorBio().trim().isEmpty() ? null : req.getAuthorBio().trim());
        }
        if (req.getAuthorAvatar() != null) {
            b.setAuthorAvatar(req.getAuthorAvatar().trim().isEmpty() ? null : req.getAuthorAvatar().trim());
        }
        if (req.getFooterText() != null) {
            b.setFooterText(req.getFooterText().trim().isEmpty() ? null : req.getFooterText().trim());
        }
        return siteBrandingRepository.save(b);
    }
}
