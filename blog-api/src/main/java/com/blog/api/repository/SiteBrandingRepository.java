package com.blog.api.repository;

import com.blog.api.entity.SiteBranding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SiteBrandingRepository extends JpaRepository<SiteBranding, Long> {
}
