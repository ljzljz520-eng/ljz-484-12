package com.novel.service;

import com.novel.dto.ChapterPublishRequest;
import com.novel.dto.ChapterSaveRequest;
import com.novel.exception.NotFoundException;
import com.novel.exception.ValidationException;
import com.novel.model.Chapter;
import com.novel.repository.DataRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChapterService {

    private final DataRepository dataRepository;
    private final ChapterValidator chapterValidator;

    public ChapterService(DataRepository dataRepository, ChapterValidator chapterValidator) {
        this.dataRepository = dataRepository;
        this.chapterValidator = chapterValidator;
    }

    /**
     * 保存草稿：不做发布级别的字数 / 占位符校验，
     * 仅保证标题、正文字段存在，方便作者随时保存未完成的内容。
     * 已发布章节被修改后会退回草稿，需要重新通过发布校验。
     */
    public Chapter saveChapter(Long chapterId, ChapterSaveRequest request) {
        Chapter chapter = dataRepository.findChapterById(chapterId);
        if (chapter == null) {
            throw new NotFoundException("章节不存在，无法保存");
        }
        if (request.getTitle() == null || request.getContent() == null) {
            throw new ValidationException(List.of("标题和正文字段不能为空"));
        }
        chapter.setTitle(request.getTitle());
        chapter.setContent(request.getContent());
        chapter.setStatus("DRAFT");
        return chapter;
    }

    /**
     * 新建章节：初始状态始终为草稿，必须再走一次发布流程。
     */
    public Chapter createChapter(Long novelId, ChapterSaveRequest request) {
        if (dataRepository.findNovelById(novelId) == null) {
            throw new NotFoundException("小说不存在，无法新建章节");
        }
        if (request.getTitle() == null || request.getContent() == null) {
            throw new ValidationException(List.of("标题和正文字段不能为空"));
        }
        return dataRepository.createChapter(novelId, request.getTitle(), request.getContent());
    }

    /**
     * 发布章节。
     * 这里在服务端重新执行完整校验（标题非空、字数下限、占位符），
     * 不相信浏览器的判断，任何绕过前端的请求都无法发布。
     * 请求中若携带了最新的标题 / 正文则一并保存后再校验。
     */
    public Chapter publishChapter(Long chapterId, ChapterPublishRequest request) {
        Chapter chapter = dataRepository.findChapterById(chapterId);
        if (chapter == null) {
            throw new NotFoundException("章节不存在，无法发布");
        }
        if (request != null) {
            if (request.getTitle() != null) {
                chapter.setTitle(request.getTitle());
            }
            if (request.getContent() != null) {
                chapter.setContent(request.getContent());
            }
        }

        List<String> errors = chapterValidator.validate(chapter.getTitle(), chapter.getContent());
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        chapter.setStatus("PUBLISHED");
        return chapter;
    }
}
