package com.novel.controller;

import com.novel.exception.GlobalExceptionHandler;
import com.novel.repository.DataRepository;
import com.novel.service.ChapterService;
import com.novel.service.ChapterValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 验证发布接口的服务端兜底校验：
 * 即使请求完全绕过浏览器，不合格章节也无法发布。
 */
class PublishApiTest {

    private MockMvc mockMvc;
    private DataRepository repository;

    @BeforeEach
    void setUp() {
        repository = new DataRepository();
        repository.init();
        ChapterValidator validator = new ChapterValidator();
        ChapterService service = new ChapterService(repository, validator);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new NovelController(repository, service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void publishingTooShortChapterReturns400WithErrors() throws Exception {
        // 先新建一个草稿
        String body = "{\"title\":\"测试章\",\"content\":\"短内容\"}";
        String response = mockMvc.perform(post("/api/author/novels/1/chapters")
                        .contentType("application/json").content(body))
                .andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(response.replaceAll("(?s).*?\"id\":(\\d+).*", "$1"));

        // 直接调发布接口（模拟绕过前端）：正文太短，应被后端拒绝
        mockMvc.perform(post("/api/author/chapters/" + id + "/publish")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0]").exists());
    }

    @Test
    void publishingEmptyTitleIsRejected() throws Exception {
        // 种子章节 1 原本已发布；用空标题重新发布应被拒
        String body = "{\"title\":\"   \",\"content\":\"" + "字".repeat(200) + "\"}";
        mockMvc.perform(post("/api/author/chapters/1/publish")
                        .contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString("标题不能为空")));
    }

    @Test
    void publishingValidChapterReturns200() throws Exception {
        String body = "{\"title\":\"合格章节\",\"content\":\"" + "字".repeat(150) + "\"}";
        mockMvc.perform(post("/api/author/chapters/1/publish")
                        .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.message").value("章节已通过校验并发布"));
    }

    @Test
    void draftChaptersAreHiddenFromReaders() throws Exception {
        String body = "{\"title\":\"草稿章\",\"content\":\"草稿正文\"}";
        mockMvc.perform(post("/api/author/novels/1/chapters")
                        .contentType("application/json").content(body))
                .andExpect(status().isOk());

        // 读者端详情接口不应包含草稿
        mockMvc.perform(get("/api/novels/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapters[?(@.status=='DRAFT')]").doesNotExist());
    }
}
