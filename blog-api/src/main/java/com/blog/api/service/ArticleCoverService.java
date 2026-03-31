package com.blog.api.service;

import com.blog.api.dto.ArticleCoverConfirmRequest;
import com.blog.api.dto.ArticleCoverGenerateRequest;
import com.blog.api.dto.ArticleCoverGenerateResponse;
import com.blog.api.dto.ArticleCoverPreviewResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * MiniMax 文生图使用 OkHttp POST（与官方/Apifox 示例一致）；下载与图床仍用 {@link RestTemplate}。
 */
@Service
@RequiredArgsConstructor
public class ArticleCoverService {

    private static final Logger log = LoggerFactory.getLogger(ArticleCoverService.class);
    private static final okhttp3.MediaType JSON_UTF8 = okhttp3.MediaType.get("application/json; charset=utf-8");

    private static final int SUMMARY_PROMPT_MAX = 800;
    /** 官方：prompt 最长 1500 字符 */
    private static final int PROMPT_MAX_CHARS = 1500;

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

    @Qualifier("articleCoverRestTemplate")
    private final RestTemplate restTemplate;

    @Value("${spring.ai.minimax.api-key:}")
    private String minimaxApiKey;

    @Value("${blog.ai-cover.minimax-image-url:https://api.minimaxi.com/v1/image_generation}")
    private String minimaxImageUrl;

    @Value("${blog.ai-cover.image-model:image-01}")
    private String imageModel;

    @Value("${blog.ai-cover.aspect-ratio:16:9}")
    private String aspectRatio;

    @Value("${blog.ai-cover.width:1280}")
    private int imageWidth;

    @Value("${blog.ai-cover.height:720}")
    private int imageHeight;

    @Value("${blog.ai-cover.response-format:url}")
    private String responseFormat;

    @Value("${blog.ai-cover.image-n:1}")
    private int imageN;

    @Value("${blog.ai-cover.prompt-optimizer:false}")
    private boolean promptOptimizer;

    @Value("${blog.ai-cover.aigc-watermark:false}")
    private boolean aigcWatermark;

    @Value("${blog.boltp.upload-url:https://www.boltp.com/api/v2/upload}")
    private String boltpUploadUrl;

    @Value("${blog.boltp.token:}")
    private String boltpToken;

    @Value("${blog.boltp.auth-mode:bearer}")
    private String boltpAuthMode;

    @Value("${blog.boltp.storage-id:2}")
    private int boltpStorageId;

    @Value("${blog.boltp.album-id:282}")
    private int boltpAlbumId;

    @Value("${blog.ai-cover.http-connect-timeout-ms:10000}")
    private long coverHttpConnectTimeoutMs;

    @Value("${blog.ai-cover.http-read-timeout-ms:90000}")
    private long coverHttpReadTimeoutMs;

    /** 下载 OSS 预签名图：用 JDK HttpClient + URI.create 原串，避免 RestTemplate 规范化路径/query 导致 SignatureDoesNotMatch */
    private HttpClient imageDownloadHttpClient;

    @PostConstruct
    void initImageDownloadHttpClient() {
        imageDownloadHttpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(coverHttpConnectTimeoutMs))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /** 仅文生图并返回 Base64，供前端预览；不要求配置图床 Token */
    public ArticleCoverPreviewResponse generateCoverPreview(ArticleCoverGenerateRequest req) {
        if (minimaxApiKey == null || minimaxApiKey.isBlank()) {
            throw new IllegalStateException("未配置 MiniMax：请设置 MINIMAX_API_KEY");
        }

        String prompt = buildImagePrompt(req.getTitle().trim(), Optional.ofNullable(req.getSummary()).map(String::trim).orElse(""));
        log.info("generateCoverPreview: MiniMax 文生图 prompt 预览(前200字): {}", preview(prompt, 200));

        byte[] imageBytes = callMiniMaxImage(prompt);
        String mime = sniffImageMimeType(imageBytes);
        String b64 = Base64.getEncoder().encodeToString(imageBytes);
        return new ArticleCoverPreviewResponse(b64, mime);
    }

    /** 将用户确认的预览图上传到图床 */
    public ArticleCoverGenerateResponse confirmCoverUpload(ArticleCoverConfirmRequest req) {
        if (boltpToken == null || boltpToken.isBlank()) {
            throw new IllegalStateException("未配置闪电图床：请设置 BOLTP_TOKEN（用户中心生成 API Token）");
        }
        byte[] imageBytes = decodeBase64ImagePayload(req.getImageBase64());
        if (imageBytes.length == 0) {
            throw new IllegalArgumentException("图片数据为空");
        }
        String url = uploadToBoltp(imageBytes);
        return new ArticleCoverGenerateResponse(url);
    }

    static byte[] decodeBase64ImagePayload(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("图片数据不能为空");
        }
        String s = raw.trim();
        int comma = s.indexOf(',');
        if (s.regionMatches(true, 0, "data:", 0, 5) && comma > 0) {
            s = s.substring(comma + 1).trim();
        }
        try {
            return Base64.getDecoder().decode(s);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("图片 Base64 无效", e);
        }
    }

    private static String sniffImageMimeType(byte[] bytes) {
        if (bytes == null || bytes.length < 3) {
            return "image/jpeg";
        }
        if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8) {
            return "image/jpeg";
        }
        if (bytes.length >= 8 && bytes[0] == (byte) 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return "image/png";
        }
        if (bytes.length >= 6 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
            return "image/gif";
        }
        return "image/jpeg";
    }

    /**
     * 与 OkHttp 示例一致：科技感横版封面，图中包含标题相关文字。
     */
    private static String buildImagePrompt(String title, String summary) {
        StringBuilder sb = new StringBuilder();
        sb.append("为中文技术博客生成一张围绕当前博客标题的科技感的横版封面图。注意，图中应该有标题相关的文字。标题：")
                .append(title)
                .append("。");
        if (summary != null && !summary.isBlank()) {
            String s = summary.length() > SUMMARY_PROMPT_MAX ? summary.substring(0, SUMMARY_PROMPT_MAX) + "…" : summary;
            sb.append(" 摘要参考：").append(s);
        }
        String p = sb.toString();
        if (p.length() > PROMPT_MAX_CHARS) {
            return p.substring(0, PROMPT_MAX_CHARS);
        }
        return p;
    }

    private byte[] callMiniMaxImage(String prompt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", imageModel);
        body.put("prompt", prompt);
        putSizeOrAspect(body);
        body.put("response_format", responseFormat.trim().toLowerCase());
        body.put("n", imageN);
        body.put("prompt_optimizer", promptOptimizer);
        body.put("aigc_watermark", aigcWatermark);

        final String json;
        try {
            json = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new IllegalStateException("MiniMax 请求体序列化失败: " + e.getMessage(), e);
        }

        RequestBody requestBody = RequestBody.create(json, JSON_UTF8);
        Request request = new Request.Builder()
                .url(minimaxImageUrl)
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + minimaxApiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        log.info("generateCover: OkHttp POST MiniMax image_generation（{}），超时见 blog.ai-cover.http-*-timeout-ms", minimaxImageUrl);
        try (Response response = okHttpClient.newCall(request).execute()) {
            ResponseBody rb = response.body();
            String respBody = rb != null ? rb.string() : "";
            if (!response.isSuccessful()) {
                throw new IllegalStateException("MiniMax HTTP " + response.code() + ": " + preview(respBody, 800));
            }
            JsonNode root = objectMapper.readTree(respBody);
            JsonNode baseResp = root.path("base_resp");
            if (baseResp.has("status_code") && baseResp.get("status_code").asInt() != 0) {
                String msg = baseResp.path("status_msg").asText("unknown");
                throw new IllegalStateException("MiniMax 文生图失败: " + msg);
            }

            JsonNode data = root.path("data");
            String fmt = responseFormat.trim().toLowerCase();
            if ("url".equals(fmt)) {
                return loadFromImageUrlsOrThrow(data, root);
            }

            String b64 = extractFirstBase64Payload(data);
            if (b64 != null && !b64.isEmpty()) {
                return Base64.getDecoder().decode(b64);
            }
            log.warn("generateCover: data.image_base64 无可用元素，尝试 image_urls 兜底");
            return loadFromImageUrlsOrThrow(data, root);
        } catch (IOException e) {
            log.warn("MiniMax 文生图请求失败: {}", e.getMessage());
            throw new IllegalStateException("MiniMax 文生图调用失败（超时或网络见 blog.ai-cover.http-read-timeout-ms）: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("MiniMax 返回的 base64 无效: " + e.getMessage(), e);
        }
    }

    private void putSizeOrAspect(Map<String, Object> body) {
        if (imageWidth > 0 && imageHeight > 0) {
            if (!isValidDimension(imageWidth) || !isValidDimension(imageHeight)) {
                log.warn("generateCover: width/height 需在 [512,2048] 且为 8 的倍数，当前 {}×{}，改用 aspect_ratio={}",
                        imageWidth, imageHeight, aspectRatio);
                body.put("aspect_ratio", aspectRatio);
                return;
            }
            body.put("width", imageWidth);
            body.put("height", imageHeight);
            log.info("generateCover: 使用 width×height={}×{}", imageWidth, imageHeight);
            return;
        }
        body.put("aspect_ratio", aspectRatio);
        log.info("generateCover: 使用 aspect_ratio={}", aspectRatio);
    }

    private static boolean isValidDimension(int v) {
        return v >= 512 && v <= 2048 && (v % 8 == 0);
    }

    private byte[] loadFromImageUrlsOrThrow(JsonNode data, JsonNode root) {
        JsonNode urls = data.path("image_urls");
        if (urls.isArray() && !urls.isEmpty()) {
            String imageUrl = urls.get(0).asText("");
            if (!imageUrl.isEmpty()) {
                log.info("generateCover: 使用 image_urls[0] 下载（{}）", preview(imageUrl, 100));
                return downloadGeneratedImage(imageUrl);
            }
        }
        throw new IllegalStateException("MiniMax 未返回可用图片（无 image_urls / image_base64）: " + root);
    }

    private static String extractFirstBase64Payload(JsonNode data) {
        if (data == null || data.isMissingNode()) {
            return null;
        }
        JsonNode n = data.path("image_base64");
        if (n.isTextual()) {
            return n.asText();
        }
        if (n.isArray()) {
            for (JsonNode el : n) {
                if (el == null || el.isNull()) {
                    continue;
                }
                if (el.isTextual()) {
                    String t = el.asText("");
                    if (!t.isEmpty()) {
                        return t;
                    }
                }
                if (el.isObject()) {
                    if (el.has("base64") && el.get("base64").isTextual()) {
                        return el.get("base64").asText();
                    }
                    if (el.has("image_base64") && el.get("image_base64").isTextual()) {
                        return el.get("image_base64").asText();
                    }
                }
            }
        }
        return null;
    }

    private byte[] downloadGeneratedImage(String imageUrl) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(imageUrl))
                .timeout(Duration.ofMillis(coverHttpReadTimeoutMs))
                .GET()
                .header("Accept", "*/*")
                .build();
        try {
            HttpResponse<byte[]> response = imageDownloadHttpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            int code = response.statusCode();
            if (code / 100 != 2) {
                String err = new String(response.body(), StandardCharsets.UTF_8);
                throw new IllegalStateException("下载生成图失败 HTTP " + code + ": " + preview(err, 500));
            }
            byte[] bytes = response.body();
            if (bytes == null || bytes.length == 0) {
                throw new IllegalStateException("下载生成图失败：空内容");
            }
            log.info("generateCover: 已下载生成图 {} bytes", bytes.length);
            return bytes;
        } catch (IOException e) {
            throw new IllegalStateException("下载 MiniMax 图片失败: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("下载 MiniMax 图片被中断: " + e.getMessage(), e);
        }
    }

    private String uploadToBoltp(byte[] imageBytes) {
        ByteArrayResource file = new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return "cover.jpg";
            }
        };

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", file);
        form.add("storage_id", Integer.toString(boltpStorageId));
        form.add("album_id", Integer.toString(boltpAlbumId));
        if ("form-token".equalsIgnoreCase(boltpAuthMode.trim())) {
            form.add("token", boltpToken);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        if (!"form-token".equalsIgnoreCase(boltpAuthMode.trim())) {
            headers.setBearerAuth(boltpToken);
        }

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(form, headers);
        try {
            ResponseEntity<JsonNode> resp = restTemplate.exchange(
                    boltpUploadUrl,
                    HttpMethod.POST,
                    entity,
                    JsonNode.class);
            JsonNode root = resp.getBody();
            String url = extractImageHostUrl(root);
            if (url == null || url.isBlank()) {
                throw new IllegalStateException("图床响应中未解析到 URL: " + root);
            }
            log.info("generateCover: 图床上传成功 url={}", url);
            return url.trim();
        } catch (RestClientException e) {
            log.warn("图床上传失败: {}", e.getMessage());
            throw new IllegalStateException("图床上传失败: " + e.getMessage()
                    + "（请核对 BOLTP_UPLOAD_URL 与 BOLTP_TOKEN）", e);
        }
    }

    static String extractImageHostUrl(JsonNode json) {
        if (json == null) {
            return null;
        }
        JsonNode data = json.get("data");
        if (data != null && data.isObject()) {
            if (data.has("public_url") && data.get("public_url").isTextual()) {
                return data.get("public_url").asText();
            }
            if (data.has("url") && data.get("url").isTextual()) {
                return data.get("url").asText();
            }
            JsonNode links = data.get("links");
            if (links != null && links.has("url") && links.get("url").isTextual()) {
                return links.get("url").asText();
            }
            if (data.has("path") && data.get("path").isTextual()) {
                String path = data.get("path").asText();
                if (path.startsWith("http")) {
                    return path;
                }
            }
        }
        if (json.has("url") && json.get("url").isTextual()) {
            return json.get("url").asText();
        }
        return null;
    }

    private static String preview(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
