package com.novel.controller;

import com.novel.dto.ChapterPublishRequest;
import com.novel.dto.ChapterSaveRequest;
import com.novel.exception.NotFoundException;
import com.novel.model.Chapter;
import com.novel.model.Novel;
import com.novel.repository.DataRepository;
import com.novel.service.ChapterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow frontend to access
@Tag(name = "Novel System API")
public class NovelController {

    private final DataRepository dataRepository;
    private final ChapterService chapterService;

    public NovelController(DataRepository dataRepository, ChapterService chapterService) {
        this.dataRepository = dataRepository;
        this.chapterService = chapterService;
    }

    @GetMapping("/novels")
    @Operation(summary = "Get Novel List")
    public Map<String, Object> getNovels(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        List<Novel> list = dataRepository.findAllNovels(keyword, page, size);
        long total = dataRepository.countNovels(keyword);

        Map<String, Object> response = new HashMap<>();
        response.put("data", list);
        response.put("total", total);
        response.put("page", page);
        response.put("size", size);
        return response;
    }

    @GetMapping("/novels/{id}")
    @Operation(summary = "Get Novel Details (with published chapters)")
    public Map<String, Object> getNovelDetail(@PathVariable Long id) {
        Novel novel = dataRepository.findNovelById(id);
        if (novel == null) {
            throw new NotFoundException("Novel not found");
        }
        // 读者视角：只看到已发布章节，草稿不会出现在目录中
        List<Chapter> chapters = dataRepository.findChaptersByNovelId(id, false);

        Map<String, Object> response = new HashMap<>();
        response.put("novel", novel);
        response.put("chapters", chapters); // Include chapters as requested ("merge directory into detail")
        return response;
    }

    @GetMapping("/novels/{id}/chapters")
    @Operation(summary = "Get Chapters for a Novel")
    public List<Chapter> getChapters(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean includeAll) {
        // includeAll=true 供作者管理台查看草稿使用；默认仅返回已发布章节
        return dataRepository.findChaptersByNovelId(id, includeAll);
    }

    @GetMapping("/chapters/{id}")
    @Operation(summary = "Get Chapter Content")
    public Chapter getChapter(@PathVariable Long id) {
        Chapter chapter = dataRepository.findChapterById(id);
        if (chapter == null) {
            throw new NotFoundException("Chapter not found");
        }
        return chapter;
    }

    // ===================== 作者端：章节创作与发布 =====================

    @GetMapping("/author/novels/{id}/chapters")
    @Operation(summary = "Author: list all chapters (drafts included)")
    public List<Chapter> getAuthorChapters(@PathVariable Long id) {
        if (dataRepository.findNovelById(id) == null) {
            throw new NotFoundException("Novel not found");
        }
        return dataRepository.findChaptersByNovelId(id, true);
    }

    @PostMapping("/author/novels/{id}/chapters")
    @Operation(summary = "Author: create a new draft chapter")
    public Map<String, Object> createChapter(@PathVariable Long id, @RequestBody ChapterSaveRequest request) {
        Chapter chapter = chapterService.createChapter(id, request);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "草稿已保存");
        response.put("data", chapter);
        return response;
    }

    @PutMapping("/author/chapters/{id}")
    @Operation(summary = "Author: save chapter as draft")
    public Map<String, Object> saveChapter(@PathVariable Long id, @RequestBody ChapterSaveRequest request) {
        Chapter chapter = chapterService.saveChapter(id, request);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "草稿已保存，发布前还需通过字数与占位符校验");
        response.put("data", chapter);
        return response;
    }

    @PostMapping("/author/chapters/{id}/publish")
    @Operation(summary = "Author: validate and publish a chapter")
    public Map<String, Object> publishChapter(@PathVariable Long id,
                                              @RequestBody(required = false) ChapterPublishRequest request) {
        // 服务端在此重新做完整校验，不能只依赖浏览器
        Chapter chapter = chapterService.publishChapter(id, request);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "章节已通过校验并发布");
        response.put("data", chapter);
        return response;
    }
}
