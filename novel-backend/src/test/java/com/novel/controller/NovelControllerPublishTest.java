package com.novel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 发布接口的服务端校验测试：即使绕过浏览器，后端也必须拦截不合格章节。
 */
@SpringBootTest
@AutoConfigureMockMvc
class NovelControllerPublishTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String json(String title, String content) throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("title", title);
        body.put("content", content);
        return objectMapper.writeValueAsString(body);
    }

    private long createDraft(String title, String content) throws Exception {
        String response = mockMvc.perform(post("/api/novels/1/chapters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(title, content)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.published").value(false))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void publishRejectsShortChapterThenSucceedsAfterFix() throws Exception {
        long id = createDraft("测试章节", "太短了");

        // 正文太短 → 后端拒绝发布
        mockMvc.perform(post("/api/chapters/" + id + "/publish"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("章节未通过发布校验，请先修改"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.wordCount").value(3));

        // 修改后正文够长但含占位符 → 继续拒绝
        mockMvc.perform(put("/api/chapters/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("测试章节", "主角名叫{{名字}}，" + "他走进城里。".repeat(10))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/chapters/" + id + "/publish"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem(containsString("占位符"))));

        // 修改合格 → 发布成功并标记为已发布
        mockMvc.perform(put("/api/chapters/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("测试章节", "他走进城里，".repeat(20))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/chapters/" + id + "/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(true));
    }

    @Test
    void publishRejectsEmptyTitle() throws Exception {
        long id = createDraft("可改的标题", "他走进城里，".repeat(20));

        // 标题清空后发布 → 拒绝
        mockMvc.perform(put("/api/chapters/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("  ", "他走进城里，".repeat(20))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/chapters/" + id + "/publish"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem(containsString("标题"))));
    }

    @Test
    void publishReturns404ForMissingChapter() throws Exception {
        mockMvc.perform(post("/api/chapters/99999/publish"))
                .andExpect(status().isNotFound());
    }
}
