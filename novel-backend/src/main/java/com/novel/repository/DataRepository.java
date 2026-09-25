package com.novel.repository;

import com.novel.model.Chapter;
import com.novel.model.Novel;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class DataRepository {
        private final Map<Long, Novel> novels = new ConcurrentHashMap<>();
        private final Map<Long, Chapter> chapters = new ConcurrentHashMap<>();
        private final AtomicLong novelIdGenerator = new AtomicLong(1);
        private final AtomicLong chapterIdGenerator = new AtomicLong(1);

        @PostConstruct
        public void init() {
                // Seeding Data
                Novel novel1 = new Novel(novelIdGenerator.getAndIncrement(),
                                "星际穿越之编程大师",
                                "讲述一位程序员意外穿越到未来，用代码拯救宇宙的故事。",
                                "https://images.unsplash.com/photo-1550751827-4bd374c3f58b?q=80&w=800&auto=format&fit=crop",
                                LocalDateTime.now());
                novels.put(novel1.getId(), novel1);

                LocalDateTime now = LocalDateTime.now();
                chapters.put(chapterIdGenerator.get(), publishedChapter(chapterIdGenerator.getAndIncrement(), novel1.getId(),
                                "第一章：Hello World", 1,
                                "他醒来时，发现眼前只有绿色的代码流。\n"
                                + "终端上的光标一闪一闪，像是在等待他敲下生命中的第一行命令。他试着抬手，指尖却穿过了全息键盘，带起一串细碎的光点。\n"
                                + "\"系统启动完成，欢迎来到第零号宇宙。\"一个毫无感情的声音在脑海中响起。他深吸一口气，在虚空中敲下两个单词：Hello World。",
                                now));
                chapters.put(chapterIdGenerator.get(), publishedChapter(chapterIdGenerator.getAndIncrement(), novel1.getId(),
                                "第二章：变量声明", 2,
                                "“你是谁？”面前的机器人冷冷地问道，金属瞳孔中流淌着淡蓝色的光。\n"
                                + "他握紧了手中的数据线，努力让声音听起来不那么颤抖：“Define me.” 这是他醒来后记住的第二句话，也是这个世界定义存在的唯一方式。\n"
                                + "机器人沉默了三秒，像是在解析这句语法。随后它侧身让出通道：“身份已声明，请进，变量。”",
                                now));
                chapters.put(chapterIdGenerator.get(), publishedChapter(chapterIdGenerator.getAndIncrement(), novel1.getId(),
                                "第三章：循环陷阱", 3,
                                "时间仿佛陷入了死循环，他必须找到 break 的条件。\n"
                                + "走廊尽头的钟声第七次敲响，窗外的黄昏第七次落下，连咖啡杯上的热气都保持着一模一样的弧度。\n"
                                + "他在笔记本上划掉又一个错误的猜想，忽然注意到每次循环开始时，门牌上的数字都会比上一次多一。出口也许不在循环之外，而在累加的尽头。",
                                now));

                Novel novel2 = new Novel(novelIdGenerator.getAndIncrement(),
                                "灵气复苏时代的架构师",
                                "灵气复苏，万物进化。他发现修仙法门竟然符合微服务架构原理。",
                                "https://images.unsplash.com/photo-1518770660439-4636190af475?q=80&w=600&auto=format&fit=crop",
                                LocalDateTime.now());
                novels.put(novel2.getId(), novel2);

                chapters.put(chapterIdGenerator.get(), publishedChapter(chapterIdGenerator.getAndIncrement(), novel2.getId(),
                                "第一章：单体应用破碎", 1,
                                "天地巨变，世界原本的秩序（Monolith）在一道惊雷中崩塌了。\n"
                                + "山脉断裂成各自漂浮的孤岛，城市像被一只无形的手拆成了独立运转的模块，连风都在不同的区块里刮着不同的方向。\n"
                                + "他站在废墟之间，第一次看见灵气以接口报文的形态在空中流动。一个旧时代结束了，而新的架构图，正等着有人落笔。",
                                now));
                chapters.put(chapterIdGenerator.get(), publishedChapter(chapterIdGenerator.getAndIncrement(), novel2.getId(),
                                "第二章：服务发现", 2,
                                "他盘膝坐下，感应到了周围漂浮的灵气节点，就像注册中心里排列整齐的服务实例一样清晰。\n"
                                + "每一个节点都在向天地广播自己的名字、心跳与所能提供的法门。他试着把神识探过去，立刻收到了一连串心跳回执。\n"
                                + "原来修仙界的第一步从不是引气入体，而是先在天地间完成一次成功的服务注册。",
                                now));

                Novel novel3 = new Novel(novelIdGenerator.getAndIncrement(),
                                "只有我知道剧情的测试员",
                                "作为世界系统的唯一QA，他能看到由于Bug导致的隐藏剧情。",
                                "https://images.unsplash.com/photo-1555949963-ff9fe0c870eb?q=80&w=800&auto=format&fit=crop",
                                LocalDateTime.now());
                novels.put(novel3.getId(), novel3);
        }

        public List<Novel> findAllNovels(String keyword, int page, int size) {
                return novels.values().stream()
                                .filter(n -> keyword == null || keyword.isEmpty() || n.getTitle().contains(keyword)
                                                || n.getDescription().contains(keyword))
                                .sorted(Comparator.comparing(Novel::getId).reversed())
                                .skip((long) (page - 1) * size)
                                .limit(size)
                                .collect(Collectors.toList());
        }

        public long countNovels(String keyword) {
                return novels.values().stream()
                                .filter(n -> keyword == null || keyword.isEmpty() || n.getTitle().contains(keyword)
                                                || n.getDescription().contains(keyword))
                                .count();
        }

        public Novel findNovelById(Long id) {
                return novels.get(id);
        }

        public List<Chapter> findChaptersByNovelId(Long novelId) {
                return chapters.values().stream()
                                .filter(c -> c.getNovelId().equals(novelId))
                                .sorted(Comparator.comparing(Chapter::getOrderNo))
                                .collect(Collectors.toList());
        }

        public Chapter findChapterById(Long id) {
                return chapters.get(id);
        }

        /**
         * 新增或更新章节。新章节会自动分配 ID，并排在该小说当前章节的最后（orderNo 递增）。
         */
        public Chapter saveChapter(Chapter chapter) {
                if (chapter.getId() == null) {
                        chapter.setId(chapterIdGenerator.getAndIncrement());
                        if (chapter.getOrderNo() == null) {
                                chapter.setOrderNo(nextOrderNo(chapter.getNovelId()));
                        }
                }
                if (chapter.getCreatedAt() == null) {
                        chapter.setCreatedAt(LocalDateTime.now());
                }
                chapters.put(chapter.getId(), chapter);
                return chapter;
        }

        private int nextOrderNo(Long novelId) {
                return chapters.values().stream()
                                .filter(c -> c.getNovelId().equals(novelId))
                                .map(Chapter::getOrderNo)
                                .filter(Objects::nonNull)
                                .max(Integer::compareTo)
                                .orElse(0) + 1;
        }

        /** 构造一条“已发布”的种子章节（种子数据对读者可见）。 */
        private Chapter publishedChapter(Long id, Long novelId, String title, int orderNo, String content,
                        LocalDateTime createdAt) {
                return new Chapter(id, novelId, title, orderNo, content,
                                com.novel.service.ChapterValidator.countWords(content),
                                Chapter.STATUS_PUBLISHED, createdAt, createdAt);
        }
}
