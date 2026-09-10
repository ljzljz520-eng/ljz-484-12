package com.novel.controller;

import com.novel.model.Chapter;
import com.novel.model.ChapterRequest;
import com.novel.model.Novel;
import com.novel.repository.DataRepository;
import com.novel.service.ChapterValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow frontend to access
@Tag(name = "Novel System API")
public class NovelController {

    private final DataRepository dataRepository;
    private final ChapterValidator chapterValidator;

    public NovelController(DataRepository dataRepository, ChapterValidator chapterValidator) {
        this.dataRepository = dataRepository;
        this.chapterValidator = chapterValidator;
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
            throw new RuntimeException("Novel not found");
        }
        List<Chapter> chapters = dataRepository.findChaptersByNovelId(id);

        Map<String, Object> response = new HashMap<>();
        response.put("novel", novel);
        response.put("chapters", chapters); // Include chapters as requested ("merge directory into detail")
        return response;
    }

    @GetMapping("/novels/{id}/chapters")
    @Operation(summary = "Get Chapters for a Novel")
    public List<Chapter> getChapters(@PathVariable Long id) {
        return dataRepository.findChaptersByNovelId(id);
    }

    @GetMapping("/chapters/{id}")
    @Operation(summary = "Get Chapter Content")
    public Chapter getChapter(@PathVariable Long id) {
        Chapter chapter = dataRepository.findChapterById(id);
        if (chapter == null) {
            throw new RuntimeException("Chapter not found");
        }
        return chapter;
    }

    @PostMapping("/novels/{id}/chapters")
    @Operation(summary = "Create Chapter (draft, unpublished)")
    public ResponseEntity<Chapter> createChapter(@PathVariable Long id, @RequestBody ChapterRequest request) {
        Novel novel = dataRepository.findNovelById(id);
        if (novel == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Novel not found");
        }
        String title = (request.title() == null || request.title().isBlank()) ? "未命名章节" : request.title();
        String content = request.content() == null ? "" : request.content();
        Chapter chapter = dataRepository.createChapter(id, title, content);
        return ResponseEntity.status(HttpStatus.CREATED).body(chapter);
    }

    @PutMapping("/chapters/{id}")
    @Operation(summary = "Update Chapter (save author edits)")
    public Chapter updateChapter(@PathVariable Long id, @RequestBody ChapterRequest request) {
        Chapter chapter = dataRepository.findChapterById(id);
        if (chapter == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found");
        }
        String title = request.title() != null ? request.title() : chapter.getTitle();
        String content = request.content() != null ? request.content() : chapter.getContent();
        return dataRepository.updateChapter(id, title, content);
    }

    @PostMapping("/chapters/{id}/publish")
    @Operation(summary = "Publish Chapter (server-side re-validation)")
    public ResponseEntity<?> publishChapter(@PathVariable Long id) {
        Chapter chapter = dataRepository.findChapterById(id);
        if (chapter == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found");
        }
        // 后端重复校验：不能只依赖浏览器端的字数检查
        List<String> errors = chapterValidator.validateForPublish(chapter.getTitle(), chapter.getContent());
        if (!errors.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "章节未通过发布校验，请先修改");
            response.put("errors", errors);
            response.put("wordCount", chapterValidator.countWords(chapter.getContent()));
            return ResponseEntity.badRequest().body(response);
        }
        chapter.setPublished(true);
        return ResponseEntity.ok(chapter);
    }
}
