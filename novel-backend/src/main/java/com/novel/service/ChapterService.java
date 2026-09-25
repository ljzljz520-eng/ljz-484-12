package com.novel.service;

import com.novel.dto.ChapterRequest;
import com.novel.exception.NotFoundException;
import com.novel.model.Chapter;
import com.novel.repository.DataRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ChapterService {

    private final DataRepository dataRepository;

    public ChapterService(DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    /** 新建章节：publish=false 保存草稿；publish=true 校验通过后直接发布。 */
    public Chapter createChapter(Long novelId, ChapterRequest request) {
        if (dataRepository.findNovelById(novelId) == null) {
            throw new NotFoundException("小说不存在");
        }

        Chapter chapter = new Chapter();
        chapter.setNovelId(novelId);
        chapter.setTitle(request.getTitle());
        chapter.setContent(request.getContent());
        chapter.setStatus(Chapter.STATUS_DRAFT);
        chapter.setWordCount(ChapterValidator.countWords(chapter.getContent()));
        chapter.setCreatedAt(LocalDateTime.now());

        if (request.isPublish()) {
            // 服务端重复校验：浏览器校验可能被绕过，这里必须再挡一次
            ChapterValidator.validateForPublish(chapter.getTitle(), chapter.getContent());
            markPublished(chapter);
        }
        return dataRepository.saveChapter(chapter);
    }

    /**
     * 修改已有章节：
     *  - publish=true：任何情况下都必须通过发布校验；已发布章节会保留原发布时间，草稿则转为已发布；
     *  - publish=false：保存为草稿。已发布章节退回草稿同样不需要校验（作者稍后还可以重新发布）。
     */
    public Chapter updateChapter(Long chapterId, ChapterRequest request) {
        Chapter chapter = dataRepository.findChapterById(chapterId);
        if (chapter == null) {
            throw new NotFoundException("章节不存在");
        }

        boolean wasPublished = chapter.isPublished();
        chapter.setTitle(request.getTitle());
        chapter.setContent(request.getContent());

        if (request.isPublish()) {
            ChapterValidator.validateForPublish(chapter.getTitle(), chapter.getContent());
            markPublished(chapter, wasPublished);
        } else {
            chapter.setStatus(Chapter.STATUS_DRAFT);
            chapter.setPublishedAt(null);
            chapter.setWordCount(ChapterValidator.countWords(chapter.getContent()));
        }
        return dataRepository.saveChapter(chapter);
    }

    private void markPublished(Chapter chapter) {
        markPublished(chapter, false);
    }

    private void markPublished(Chapter chapter, boolean alreadyPublished) {
        LocalDateTime now = LocalDateTime.now();
        chapter.setStatus(Chapter.STATUS_PUBLISHED);
        chapter.setWordCount(ChapterValidator.countWords(chapter.getContent()));
        chapter.setPublishedAt(alreadyPublished && chapter.getPublishedAt() != null
                ? chapter.getPublishedAt() : now);
    }
}
