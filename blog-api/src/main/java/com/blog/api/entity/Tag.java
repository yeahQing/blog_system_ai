package com.blog.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "blog_tag")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    /** 显式声明便于未启用 Lombok 注解处理的 IDE 解析；与 @Data 生成的方法等价 */
    public Long getId() {
        return id;
    }

    @Column(nullable = false, unique = true, length = 64)
    @ToString.Include
    private String name;

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @JsonIgnore
    @ManyToMany(mappedBy = "tags")
    @Builder.Default
    private Set<Article> articles = new HashSet<>();
}
