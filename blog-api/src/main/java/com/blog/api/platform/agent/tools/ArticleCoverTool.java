package com.blog.api.platform.agent.tools;

import com.blog.api.dto.ArticleCoverGenerateRequest;
import com.blog.api.dto.ArticleCoverPreviewResponse;
import com.blog.api.service.ArticleCoverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 工具：生成文章封面图。
 * <p>复用 {@link ArticleCoverService#generateCoverPreview}，返回预览图地址。
 * 注意：文生图耗时较长（约 30-90 秒）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleCoverTool {

    private final ArticleCoverService articleCoverService;

    @Tool(description = "为文章生成封面图（文生图）。耗时较长约 30-90 秒。"
            + "输入：文章标题、摘要；输出：封面图预览地址。")
    public String generateCover(
            @ToolParam(description = "文章标题") String title,
            @ToolParam(description = "文章摘要，用于生成贴合内容的封面") String summary) {
        log.info("[Tool] generate_cover: title={}", title);
        ArticleCoverGenerateRequest req = new ArticleCoverGenerateRequest();
        req.setTitle(title);
        req.setSummary(summary);
        try {
            ArticleCoverPreviewResponse resp = articleCoverService.generateCoverPreview(req);
            return "封面已生成，预览地址：" + resp;
        } catch (Exception e) {
            log.warn("[Tool] generate_cover 失败: {}", e.getMessage());
            return "封面生成失败：" + e.getMessage() + "（可稍后手动生成）";
        }
    }
}
