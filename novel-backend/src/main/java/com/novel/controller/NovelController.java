package com.novel.controller;

import com.novel.dto.ChapterRequest;
import com.novel.exception.NotFoundException;
import com.novel.model.Chapter;
import com.novel.model.Novel;
import com.novel.repository.DataRepository;
import com.novel.service.ChapterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    @Operation(summary = "Get Novel Details (with chapters)")
    public Map<String, Object> getNovelDetail(@PathVariable Long id) {
        Novel novel = dataRepository.findNovelById(id);
        if (novel == null) {
            throw new NotFoundException("Novel not found");
        }
        List<Chapter> chapters = dataRepository.findChaptersByNovelId(id);

        Map<String, Object> response = new HashMap<>();
        response.put("novel", novel);
        response.put("chapters", chapters); // 含草稿章节（带 status），供作者在写作页编辑；阅读接口只暴露已发布章节
        return response;
    }

    @GetMapping("/novels/{id}/chapters")
    @Operation(summary = "Get Chapters for a Novel")
    public List<Chapter> getChapters(@PathVariable Long id) {
        return dataRepository.findChaptersByNovelId(id);
    }

    @GetMapping("/chapters/{id}")
    @Operation(summary = "Get Chapter Content (published only)")
    public Chapter getChapter(@PathVariable Long id) {
        Chapter chapter = dataRepository.findChapterById(id);
        if (chapter == null || !chapter.isPublished()) {
            // 草稿章节对读者不可见，避免未通过发布校验的内容被直接访问
            throw new NotFoundException("章节不存在或尚未发布");
        }
        return chapter;
    }

    @PostMapping("/novels/{id}/chapters")
    @Operation(summary = "Create Chapter (save draft or publish after server-side validation)")
    public ResponseEntity<Chapter> createChapter(@PathVariable Long id, @RequestBody ChapterRequest request) {
        Chapter chapter = chapterService.createChapter(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(chapter);
    }

    @PutMapping("/chapters/{id}")
    @Operation(summary = "Update Chapter (save draft or publish after server-side validation)")
    public Chapter updateChapter(@PathVariable Long id, @RequestBody ChapterRequest request) {
        return chapterService.updateChapter(id, request);
    }
}
