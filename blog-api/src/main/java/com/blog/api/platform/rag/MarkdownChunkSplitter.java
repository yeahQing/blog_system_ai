package com.blog.api.platform.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Markdown 分块器：按代码块 / 标题 / 段落切分文章。
 * <p>用逐行扫描而非正则，更可靠：
 * <ul>
 *   <li>CODE：{@code ```} 包裹的代码块整体成一块</li>
 *   <li>SECTION：两个代码块之间、或以标题开头的文本段，超长按段落二次切分</li>
 * </ul>
 * 单块不超过 {@code maxChars}（默认 800）。TITLE/SUMMARY 块由调用方添加。</p>
 */
@Component
public class MarkdownChunkSplitter {

    private final int maxChars;

    public MarkdownChunkSplitter(@Value("${rag.chunk-max-chars:800}") int maxChars) {
        this.maxChars = maxChars;
    }

    /**
     * 将文章正文切分为多个块。
     *
     * @param content Markdown 正文（不含标题/摘要）
     * @return 块列表（index 从 0 开始）
     */
    public List<Chunk> split(String content) {
        List<Chunk> chunks = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return chunks;
        }

        String[] lines = content.split("\n", -1);
        StringBuilder buf = new StringBuilder();
        boolean inCode = false;
        int idx = 0;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("```")) {
                // 代码块开始/结束
                if (!inCode) {
                    // 先把缓冲的文本块 flush
                    if (buf.length() > 0) {
                        idx = flushText(buf.toString(), chunks, idx);
                        buf.setLength(0);
                    }
                    buf.append(line);
                    inCode = true;
                } else {
                    // 代码块结束
                    buf.append("\n").append(line);
                    chunks.add(new Chunk(idx++, "CODE", buf.toString()));
                    buf.setLength(0);
                    inCode = false;
                }
            } else if (inCode) {
                buf.append("\n").append(line);
            } else {
                buf.append(buf.length() > 0 ? "\n" : "").append(line);
            }
        }
        // 尾部残留
        if (buf.length() > 0) {
            if (inCode) {
                // 未闭合的代码块兜底
                chunks.add(new Chunk(idx++, "CODE", buf.toString()));
            } else {
                flushText(buf.toString(), chunks, idx);
            }
        }
        return chunks;
    }

    /** 把一段文本切分后加入 chunks，返回新的 idx */
    private int flushText(String text, List<Chunk> chunks, int idx) {
        if (text == null || text.isBlank()) {
            return idx;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return idx;
        }
        if (trimmed.length() <= maxChars) {
            chunks.add(new Chunk(idx++, "SECTION", trimmed));
            return idx;
        }
        // 超长按段落切分
        for (String part : splitByParagraph(trimmed, maxChars)) {
            if (!part.isBlank()) {
                chunks.add(new Chunk(idx++, "SECTION", part.trim()));
            }
        }
        return idx;
    }

    /** 按空行切分段落，累积到 maxChars 则输出一块 */
    static List<String> splitByParagraph(String text, int maxChars) {
        List<String> result = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        for (String para : text.split("\n\n+")) {
            if (buf.length() + para.length() + 2 > maxChars && buf.length() > 0) {
                result.add(buf.toString());
                buf.setLength(0);
            }
            if (para.length() > maxChars) {
                if (buf.length() > 0) {
                    result.add(buf.toString());
                    buf.setLength(0);
                }
                result.addAll(splitBySentence(para, maxChars));
            } else {
                if (buf.length() > 0) {
                    buf.append("\n\n");
                }
                buf.append(para);
            }
        }
        if (buf.length() > 0) {
            result.add(buf.toString());
        }
        return result;
    }

    /** 按句号/换行切分超长单段 */
    static List<String> splitBySentence(String text, int maxChars) {
        List<String> result = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        for (String sent : text.split("(?<=[。.!！?？\\n])")) {
            if (buf.length() + sent.length() > maxChars && buf.length() > 0) {
                result.add(buf.toString());
                buf.setLength(0);
            }
            buf.append(sent);
        }
        if (buf.length() > 0) {
            result.add(buf.toString());
        }
        return result;
    }
}
