<template>
  <div class="page-container" v-loading="loading">
    <div class="container content-wrapper">
      <el-button @click="goBack" circle plain icon="ArrowLeft" class="back-btn"></el-button>

      <div class="editor-panel glass-panel" v-if="novel">
        <div class="editor-header">
          <div class="header-titles">
            <h1 class="page-title text-gradient">{{ isEdit ? '编辑章节' : '撰写新章节' }}</h1>
            <p class="novel-name">《{{ novel.title }}》
              <el-tag v-if="isEdit" :type="chapter.status === 'PUBLISHED' ? 'success' : 'info'" size="small" effect="plain">
                {{ chapter.status === 'PUBLISHED' ? '已发布' : '草稿' }}
              </el-tag>
            </p>
          </div>
          <div class="header-actions">
            <el-button size="large" @click="saveDraft" :loading="saving">保存草稿</el-button>
            <el-button type="primary" size="large" @click="publish" :loading="publishing">
              {{ isEdit && chapter.status === 'PUBLISHED' ? '重新发布' : '标记为已发布' }}
            </el-button>
          </div>
        </div>

        <!-- 发布前检查未通过时的提示：作者必须先修改，通过后才能发布 -->
        <el-alert
          v-if="publishErrors.length > 0"
          type="error"
          show-icon
          :closable="false"
          title="章节还不能发布，请先修改以下问题："
          class="error-alert"
        >
          <ul class="error-list">
            <li v-for="(err, idx) in publishErrors" :key="idx">{{ err.message }}</li>
          </ul>
        </el-alert>

        <el-alert
          v-if="serverErrors.length > 0"
          type="warning"
          show-icon
          :closable="false"
          title="服务端校验未通过（浏览器检查不能替代后端校验）："
          class="error-alert"
        >
          <ul class="error-list">
            <li v-for="(err, idx) in serverErrors" :key="'s' + idx">{{ err.message || err }}</li>
          </ul>
        </el-alert>

        <el-form label-position="top" class="chapter-form">
          <el-form-item required>
            <el-input
              v-model="form.title"
              placeholder="请输入章节标题"
              size="large"
              maxlength="100"
              clearable
            />
          </el-form-item>

          <el-form-item required>
            <div class="textarea-wrap">
              <el-input
                v-model="form.content"
                type="textarea"
                :rows="18"
                placeholder="请输入章节正文……"
                resize="vertical"
              />
              <div class="word-counter" :class="{ short: wordCount < MIN_CONTENT_WORDS, ok: wordCount >= MIN_CONTENT_WORDS }">
                <span>{{ wordCount }} 字</span>
                <span class="word-hint">（发布至少需要 {{ MIN_CONTENT_WORDS }} 字，不含空白）</span>
              </div>
            </div>
          </el-form-item>
        </el-form>

        <!-- v-pre：原样展示花括号，避免被 Vue 解析 -->
        <div class="placeholder-tip" v-pre>
          提示：正文中请不要残留 {{占位符}}、【待补充】、（此处省略）、&lt;请输入标题&gt; 等未替换内容，否则无法发布。
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { validateChapter, countWords, MIN_CONTENT_WORDS } from '../utils/chapterValidation'

const route = useRoute()
const router = useRouter()
const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'

const novel = ref(null)
const chapter = ref({ status: 'DRAFT' })
const form = ref({ title: '', content: '' })
const loading = ref(true)
const saving = ref(false)
const publishing = ref(false)
const publishErrors = ref([])
const serverErrors = ref([])

const isEdit = computed(() => !!route.params.chapterId)
const wordCount = computed(() => countWords(form.value.content))

const goBack = () => {
  router.push(`/novel/${route.params.id}`)
}

const loadData = async () => {
  try {
    const res = await axios.get(`${API_URL}/novels/${route.params.id}`)
    novel.value = res.data.novel
    if (route.params.chapterId) {
      const found = res.data.chapters.find((c) => c.id === Number(route.params.chapterId))
      if (!found) {
        ElMessage.error('章节不存在或尚未发布')
        goBack()
        return
      }
      chapter.value = found
      form.value = { title: found.title || '', content: found.content || '' }
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const buildPayload = (publish) => ({
  title: form.value.title,
  content: form.value.content,
  publish
})

const extractServerErrors = (err) => {
  const data = err.response?.data
  if (data?.details && Array.isArray(data.details)) {
    return data.details.map((d) => ({ field: d.field, message: d.message }))
  }
  return [data?.message || '请求失败，请稍后重试']
}

// 保存草稿不做发布校验，允许作者随时保留进度
const saveDraft = async () => {
  serverErrors.value = []
  saving.value = true
  try {
    if (isEdit.value) {
      await axios.put(`${API_URL}/chapters/${route.params.chapterId}`, buildPayload(false))
    } else {
      await axios.post(`${API_URL}/novels/${route.params.id}/chapters`, buildPayload(false))
    }
    ElMessage.success('草稿已保存')
    router.push(`/novel/${route.params.id}`)
  } catch (err) {
    serverErrors.value = extractServerErrors(err)
  } finally {
    saving.value = false
  }
}

// 标记为已发布：先在浏览器端预检，通过后再请求后端；后端会独立再校验一次
const publish = async () => {
  serverErrors.value = []
  const { valid, errors } = validateChapter(form.value.title, form.value.content)
  publishErrors.value = errors
  if (!valid) {
    ElMessage.error('章节未通过发布检查，请按提示修改')
    return
  }
  publishing.value = true
  try {
    if (isEdit.value) {
      await axios.put(`${API_URL}/chapters/${route.params.chapterId}`, buildPayload(true))
    } else {
      await axios.post(`${API_URL}/novels/${route.params.id}/chapters`, buildPayload(true))
    }
    ElMessage.success('章节已发布')
    router.push(`/novel/${route.params.id}`)
  } catch (err) {
    // 即便绕过了浏览器（或规则不一致），后端拒绝时也要把原因展示给作者
    serverErrors.value = extractServerErrors(err)
    publishErrors.value = serverErrors.value
  } finally {
    publishing.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-container {
    min-height: calc(100vh - 64px);
    padding-bottom: 60px;
}

.content-wrapper {
    padding-top: 30px;
}

.back-btn {
    margin-bottom: 20px;
    background: white;
    border: 1px solid rgba(0, 0, 0, 0.1);
    color: var(--text-main);
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.editor-panel {
    padding: 36px 40px;
    background: rgba(255, 255, 255, 0.9);
}

.editor-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 20px;
    flex-wrap: wrap;
    margin-bottom: 24px;
}

.page-title {
    font-size: 1.9rem;
    margin: 0 0 8px;
}

.novel-name {
    color: var(--text-sub);
    font-size: 1rem;
    display: flex;
    align-items: center;
    gap: 8px;
}

.header-actions {
    display: flex;
    gap: 12px;
}

.header-actions .el-button--primary {
    background: linear-gradient(135deg, #6366f1, #8b5cf6);
    border: none;
}

.error-alert {
    margin-bottom: 20px;
}

.error-list {
    margin: 8px 0 0;
    padding-left: 20px;
    line-height: 1.9;
}

.chapter-form {
    margin-top: 8px;
}

.textarea-wrap {
    width: 100%;
}

:deep(.el-textarea__inner) {
    font-size: 1.05rem;
    line-height: 1.9;
}

.word-counter {
    margin-top: 8px;
    text-align: right;
    font-size: 0.9rem;
    color: var(--slate-500);
    font-family: 'Space Mono', monospace;
}

.word-counter.short {
    color: #ef4444;
}

.word-counter.ok {
    color: #16a34a;
}

.word-hint {
    font-family: inherit;
    color: var(--slate-400);
}

.placeholder-tip {
    margin-top: 4px;
    padding: 12px 16px;
    background: var(--slate-50);
    border-left: 3px solid #f59e0b;
    border-radius: 4px;
    color: var(--slate-500);
    font-size: 0.9rem;
    line-height: 1.7;
}

@media (max-width: 768px) {
    .editor-panel {
        padding: 24px 18px;
    }

    .header-actions {
        width: 100%;
    }

    .header-actions .el-button {
        flex: 1;
    }
}
</style>
