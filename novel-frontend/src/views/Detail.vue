<template>
  <div class="page-container" v-loading="loading">
    
    <!-- Dynamic Backdrop -->
    <div class="backdrop" v-if="novel" :style="{ backgroundImage: 'url(' + novel.coverUrl + ')' }"></div>
    <div class="backdrop-overlay"></div>

    <div class="container content-wrapper">
      <div v-if="novel" class="novel-header">
        <el-button @click="goBack" circle plain icon="ArrowLeft" class="back-btn"></el-button>
        
        <div class="header-inner glass-panel">
          <div class="cover-wrapper">
             <img :src="novel.coverUrl" class="cover-img" />
          </div>
          <div class="info-content">
            <h1 class="novel-title text-gradient">{{ novel.title }}</h1>
            <div class="meta-tags">
                 <span class="meta-item">{{ formatDate(novel.createdAt) }}</span>
                 <span class="meta-item">{{ chapters.length }} 章</span>
            </div>
            <p class="description">{{ novel.description }}</p>
            <el-button type="primary" size="large" round class="start-read-btn" @click="startReading">
                开始阅读
            </el-button>
          </div>
        </div>
      </div>

      <div class="chapters-section glass-panel">
        <div class="section-header">
          <h2 class="section-title">章节目录</h2>
          <el-button type="primary" plain round :icon="Plus" @click="openCreateDialog">新建章节</el-button>
        </div>
        <div class="chapter-grid">
          <router-link
              v-for="chapter in chapters"
              :key="chapter.id"
              :to="'/chapter/' + chapter.id"
              class="chapter-card"
          >
              <span class="chapter-no">{{ formatNumber(chapter.orderNo) }}</span>
              <span class="chapter-title">{{ chapter.title }}</span>
              <el-tag
                  :type="chapter.published ? 'success' : 'info'"
                  size="small"
                  effect="light"
                  class="publish-tag"
              >
                  {{ chapter.published ? '已发布' : '草稿' }}
              </el-tag>
              <el-button
                  class="edit-btn"
                  circle
                  size="small"
                  :icon="Edit"
                  title="编辑章节"
                  @click.prevent.stop="openEditDialog(chapter)"
              ></el-button>
              <span class="status-dot"></span>
          </router-link>
        </div>
         <el-empty v-if="chapters.length === 0" description="暂无章节" />
      </div>

      <!-- 章节编辑 / 发布对话框 -->
      <el-dialog
          v-model="editorVisible"
          :title="editorMode === 'create' ? '新建章节' : '编辑章节'"
          width="720px"
          class="chapter-editor-dialog"
      >
          <el-form label-position="top">
              <el-form-item label="章节标题">
                  <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="请输入章节标题" />
              </el-form-item>
              <el-form-item label="章节正文">
                  <el-input
                      v-model="form.content"
                      type="textarea"
                      :rows="12"
                      placeholder="请输入章节正文..."
                  />
              </el-form-item>
          </el-form>

          <div class="editor-meta">
              <span class="word-count" :class="{ insufficient: wordCount < MIN_CONTENT_LENGTH }">
                  字数：{{ wordCount }} / 发布至少 {{ MIN_CONTENT_LENGTH }} 字
              </span>
          </div>
          <ul class="validation-hints" v-if="validationErrors.length > 0">
              <li v-for="(err, idx) in validationErrors" :key="idx">⚠ {{ err }}</li>
          </ul>
          <div class="validation-ok" v-else>✓ 已通过发布前检查，可以发布</div>

          <template #footer>
              <el-button @click="editorVisible = false">取消</el-button>
              <el-button :loading="saving" @click="saveDraft">保存草稿</el-button>
              <el-button type="primary" :loading="publishing" @click="publish">发布</el-button>
          </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Plus, Edit } from '@element-plus/icons-vue'
import { MIN_CONTENT_LENGTH, countWords, validateChapterForPublish } from '../utils/chapterValidation'

const route = useRoute()
const router = useRouter()
const novel = ref(null)
const chapters = ref([])
const loading = ref(true)
const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'

// 章节编辑 / 发布对话框状态
const editorVisible = ref(false)
const editorMode = ref('create') // 'create' | 'edit'
const editingId = ref(null)
const form = ref({ title: '', content: '' })
const saving = ref(false)
const publishing = ref(false)

// 实时字数与发布前校验提示（规则与后端一致）
const wordCount = computed(() => countWords(form.value.content))
const validationErrors = computed(() => validateChapterForPublish(form.value.title, form.value.content))

const fetchDetail = async () => {
  try {
    const res = await axios.get(`${API_URL}/novels/${route.params.id}`)
    novel.value = res.data.novel
    chapters.value = res.data.chapters
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}

const goBack = () => {
    router.back()
}

const startReading = () => {
    if (chapters.value.length > 0) {
        router.push('/chapter/' + chapters.value[0].id)
    }
}

const openCreateDialog = () => {
    editorMode.value = 'create'
    editingId.value = null
    form.value = { title: '', content: '' }
    editorVisible.value = true
}

const openEditDialog = (chapter) => {
    editorMode.value = 'edit'
    editingId.value = chapter.id
    form.value = { title: chapter.title, content: chapter.content || '' }
    editorVisible.value = true
}

// 保存草稿（新建或更新），返回章节 id
const saveChapter = async () => {
    if (editorMode.value === 'create') {
        const res = await axios.post(`${API_URL}/novels/${route.params.id}/chapters`, form.value)
        return res.data.id
    }
    await axios.put(`${API_URL}/chapters/${editingId.value}`, form.value)
    return editingId.value
}

const saveDraft = async () => {
    saving.value = true
    try {
        await saveChapter()
        ElMessage.success('草稿已保存')
        editorVisible.value = false
        await fetchDetail()
    } catch (err) {
        console.error(err)
        ElMessage.error('保存失败，请稍后重试')
    } finally {
        saving.value = false
    }
}

const publish = async () => {
    // 浏览器端预校验：正文太短 / 标题为空 / 含未替换占位符时，提示作者先修改
    const errors = validateChapterForPublish(form.value.title, form.value.content)
    if (errors.length > 0) {
        ElMessage.warning('请先修改：' + errors[0])
        return
    }
    publishing.value = true
    try {
        const id = await saveChapter()
        // 后端发布接口会重复校验一次，不通过会返回 400 及原因
        await axios.post(`${API_URL}/chapters/${id}/publish`)
        ElMessage.success('发布成功')
        editorVisible.value = false
        await fetchDetail()
    } catch (err) {
        const data = err.response?.data
        const reason = data?.errors?.length ? data.errors.join('；') : (data?.message || '发布失败，请稍后重试')
        ElMessage.error('发布失败：' + reason)
        await fetchDetail() // 草稿可能已保存，刷新目录状态
    } finally {
        publishing.value = false
    }
}

const formatDate = (val) => {
    if(!val) return ''
    return new Date(val).toLocaleDateString('zh-CN')
}

const formatNumber = (num) => {
    return num.toString().padStart(2, '0')
}

onMounted(fetchDetail)
</script>

<style scoped>
.page-container {
    min-height: 100vh;
    position: relative;
    padding-bottom: 60px;
}

.backdrop {
    position: absolute;
    top: 0; left: 0; right: 0;
    height: 60vh;
    background-size: cover;
    background-position: center;
    z-index: 0;
    filter: blur(20px);
    opacity: 0.3;
}

.backdrop-overlay {
    position: absolute;
    top: 0; left: 0; right: 0;
    height: 70vh;
    background: linear-gradient(to bottom, rgba(255,255,255,0.2), var(--bg-color));
    z-index: 1;
}

.content-wrapper {
    position: relative;
    z-index: 2;
    padding-top: 40px;
}

.back-btn {
    margin-bottom: 20px;
    background: white;
    border: 1px solid rgba(0,0,0,0.1);
    color: var(--text-main);
    box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.novel-header {
    margin-bottom: 40px;
}

.header-inner {
    display: flex;
    gap: 40px;
    padding: 40px;
    align-items: flex-start;
    background: rgba(255,255,255,0.8);
    backdrop-filter: blur(20px);
}

.cover-img {
    width: 220px;
    border-radius: 8px;
    box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.5);
}

.info-content {
    flex: 1;
}

.novel-title {
    font-size: 2.5rem;
    margin-bottom: 15px;
}

.meta-tags {
    display: flex;
    gap: 20px;
    color: var(--text-sub);
    font-size: 0.9rem;
    margin-bottom: 25px;
    text-transform: uppercase;
    letter-spacing: 0.05em;
}

.description {
    line-height: 1.8;
    color: var(--slate-600);
    font-size: 1.1rem;
    margin-bottom: 30px;
    max-width: 800px;
}

.start-read-btn {
    background: linear-gradient(135deg, #6366f1, #8b5cf6);
    border: none;
    padding: 24px 40px;
    font-weight: 600;
    font-size: 1.1rem;
    box-shadow: 0 4px 15px rgba(99, 102, 241, 0.3);
}

.start-read-btn:hover {
    filter: brightness(1.1);
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(99, 102, 241, 0.4);
}

.section-title {
    font-size: 1.5rem;
    margin-bottom: 25px;
    padding-left: 10px;
    border-left: 4px solid var(--primary-color);
    color: var(--slate-800);
}

.section-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 25px;
}

.section-header .section-title {
    margin-bottom: 0;
}

.chapters-section {
    padding: 40px;
    background: white;
}

.chapter-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 15px;
}

.chapter-card {
    display: flex;
    align-items: center;
    padding: 20px;
    background: var(--slate-50);
    border: 1px solid var(--border-color);
    border-radius: 8px;
    transition: all 0.2s;
}

.chapter-card:hover {
    background: white;
    border-color: var(--primary-color);
    transform: translateX(5px);
    box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

.chapter-no {
    font-family: 'Space Mono', monospace;
    color: var(--slate-400);
    margin-right: 15px;
    font-size: 0.9rem;
}

.chapter-title {
    flex: 1;
    font-weight: 500;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.publish-tag {
    flex-shrink: 0;
    margin-right: 8px;
}

.edit-btn {
    flex-shrink: 0;
    margin-right: 4px;
}

/* 章节编辑器 */
.editor-meta {
    display: flex;
    justify-content: flex-end;
    margin-bottom: 8px;
}

.word-count {
    font-size: 0.85rem;
    color: var(--slate-500);
}

.word-count.insufficient {
    color: #dc2626;
    font-weight: 600;
}

.validation-hints {
    margin: 0 0 12px;
    padding: 10px 14px;
    list-style: none;
    background: #fef2f2;
    border: 1px solid #fecaca;
    border-radius: 8px;
    color: #b91c1c;
    font-size: 0.9rem;
    line-height: 1.8;
}

.validation-ok {
    margin-bottom: 12px;
    padding: 10px 14px;
    background: #f0fdf4;
    border: 1px solid #bbf7d0;
    border-radius: 8px;
    color: #15803d;
    font-size: 0.9rem;
}

.status-dot {
    width: 6px;
    height: 6px;
    background-color: var(--primary-color);
    border-radius: 50%;
    opacity: 0;
    transition: opacity 0.2s;
}

.chapter-card:hover .status-dot {
    opacity: 1;
}

@media (max-width: 768px) {
    .header-inner {
        flex-direction: column;
        align-items: center;
        text-align: center;
    }
    
    .meta-tags {
        justify-content: center;
    }
    
    .start-read-btn {
        width: 100%;
    }
}
</style>
