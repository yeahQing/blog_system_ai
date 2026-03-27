package com.blog.api.service;

import com.blog.api.entity.Tag;
import com.blog.api.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    public List<Tag> listAll() {
        return tagRepository.findAll();
    }

    @Transactional
    public Tag save(Tag tag) {
        if (tag.getName() == null || tag.getName().isBlank()) {
            throw new IllegalArgumentException("标签名不能为空");
        }
        String slug = tag.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = slugify(tag.getName());
        }
        tag.setSlug(uniqueSlug(slug, tag.getId()));
        return tagRepository.save(tag);
    }

    @Transactional
    public void deleteById(Long id) {
        tagRepository.unlinkTagFromAllArticles(id);
        tagRepository.deleteById(id);
    }

    private String slugify(String name) {
        String trimmed = name.trim().toLowerCase(Locale.ROOT);
        String ascii = trimmed.replaceAll("[^a-z0-9]+", "-").replaceAll("-+", "-").replaceAll("^-|-$", "");
        if (!ascii.isEmpty()) {
            return ascii;
        }
        return "tag-" + (Integer.toHexString(name.hashCode()));
    }

    private String uniqueSlug(String base, Long excludeId) {
        String s = base;
        int i = 0;
        while (true) {
            var opt = tagRepository.findBySlug(s);
            if (opt.isEmpty()) {
                return s;
            }
            if (excludeId != null && opt.get().getId().equals(excludeId)) {
                return s;
            }
            s = base + "-" + (++i);
        }
    }
}
