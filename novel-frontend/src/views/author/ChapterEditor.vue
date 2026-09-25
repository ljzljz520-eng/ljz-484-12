<template>
  <div class="container editor-page" v-loading="loading">
    <div class="editor-head">
      <el-button @click="goBack" plain round :icon="ArrowLeft">返回控制台</el-button>
      <el-tag v-if="chapter && chapter.status === 'PUBLISHED'" type="success" effect="light" round>
        当前：已发布
      </el-tag>
      <el-tag v-else type="info" effect="plain" round>当前：草稿</el-tag>
    </div>

    <div v-if="chapter" class="glass-panel editor-card">
      <el-input
        v-model="title"
        placeholder="请输入章节标题"
        class="title-input"
        size="large"
        maxlength="100"
      />

      <el-input
        v-model="content"
        type="textarea"
        :rows="20"
        placeholder="在这里开始创作……提示：发布要求正文至少 100 字（不含空格换行），并替换掉所有占位符"
        class="content-input"
        resize="vertical"
      />

      <!-- 实时状态条：字数进度 + 占位符提醒，不阻断写作 -->
      <div class="status-bar">
        <span class="word-count" :class="{ ok: wordCount >= MIN_CONTENT_WORDS }">
          字数：{{ wordCount }} / {{ MIN_CONTENT_WORDS }} 字
        </span>
        <el-progress
          :percentage="wordPercent"
          :stroke-width="6"
          :show-text="false"
          class="word-progress"
          :status="wordCount >= MIN_CONTENT_WORDS ? 'success' : ''"
        />
        <span v-if="hasPlaceholder" class="placeholder-warn">⚠ 检测到未替换的占位符</span>
      </div>

      <!-- 尝试发布但未通过时，页面列出全部待修改项 -->
      <el-alert
        v-if="publishErrors.length > 0"
        type="warning"
        show-icon
        :closable="false"
        title="暂时不能发布，请先修改以下问题："
        class="error-alert"
      >
        <ul class="error-list">
          <li v-for="(msg, i) in publishErrors" :key="i">{{ msg }}</li>
        </ul>
      </el-alert>

      <div class="actions">
        <el-button size="large" round :loading="saving" @click="saveDraft">
          保存草稿
        </el-button>
        <el-button
          type="primary"
          size="large"
          round
          :loading="publishing"
          @click="publish"
        >
          检查并发布
        </el-button>
      </div>
      <p class="tip">说明：发布前浏览器会检查标题、字数和占位符；提交时服务器会用相同规则再校验一次。</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import {
  MIN_CONTENT_WORDS,
  countWords,
  containsPlaceholder,
  validateChapter
} from '../../utils/chapterValidation.js'

const route = useRoute()
const router = useRouter()
const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'

const chapter = ref(null)
const title = ref('')
const content = ref('')
const loading = ref(true)
const saving = ref(false)
const publishing = ref(false)
const publishErrors = ref([])

const wordCount = computed(() => countWords(content.value))
const wordPercent = computed(() =>
  Math.min(100, Math.round((wordCount.value / MIN_CONTENT_WORDS) * 100))
)
const hasPlaceholder = computed(() =>
  containsPlaceholder(title.value) || containsPlaceholder(content.value)
)

const loadChapter = async () => {
  try {
    const res = await axios.get(`${API_URL}/chapters/${route.params.id}`)
    chapter.value = res.data
    title.value = res.data.title || ''
    content.value = res.data.content || ''
  } catch (err) {
    ElMessage.error('章节加载失败')
  } finally {
    loading.value = false
  }
}

const saveDraft = async () => {
  saving.value = true
  try {
    const res = await axios.put(`${API_URL}/author/chapters/${route.params.id}`, {
      title: title.value,
      content: content.value
    })
    chapter.value = res.data.data
    ElMessage.success(res.data.message || '草稿已保存')
  } catch (err) {
    showServerErrors(err)
  } finally {
    saving.value = false
  }
}

const publish = async () => {
  publishErrors.value = []

  // 第一道：浏览器端校验，不通过直接提示作者修改，不发请求
  const errors = validateChapter(title.value, content.value)
  if (errors.length > 0) {
    publishErrors.value = errors
    ElMessage.warning('校验未通过，请根据页面提示修改后再发布')
    return
  }

  // 第二道：Java 后端会独立重复校验，错误信息以后端为准
  publishing.value = true
  try {
    const res = await axios.post(`${API_URL}/author/chapters/${route.params.id}/publish`, {
      title: title.value,
      content: content.value
    })
    chapter.value = res.data.data
    ElMessage.success(res.data.message || '章节已通过校验并发布')
    router.push('/author')
  } catch (err) {
    showServerErrors(err)
  } finally {
    publishing.value = false
  }
}

const showServerErrors = (err) => {
  const data = err.response && err.response.data
  if (data && Array.isArray(data.errors) && data.errors.length > 0) {
    publishErrors.value = data.errors
  } else {
    ElMessage.error((data && data.message) || '操作失败，请稍后重试')
  }
}

const goBack = () => router.push('/author')

onMounted(loadChapter)
</script>

<style scoped>
.editor-page {
  padding-top: 30px;
  max-width: 860px;
}

.editor-head {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.editor-card {
  padding: 30px;
  background: white;
}

.title-input {
  margin-bottom: 20px;
}

:deep(.title-input .el-input__inner) {
  font-size: 1.3rem;
  font-weight: 600;
}

:deep(.content-input textarea) {
  font-size: 1.05rem;
  line-height: 1.9;
  font-family: inherit;
}

.status-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 16px 0;
  padding: 12px 16px;
  background: var(--slate-50);
  border-radius: 8px;
  border: 1px solid var(--border-color);
}

.word-count {
  font-family: 'Space Mono', monospace;
  font-size: 0.9rem;
  color: var(--text-sub);
  white-space: nowrap;
}

.word-count.ok {
  color: #16a34a;
  font-weight: 600;
}

.word-progress {
  flex: 1;
  max-width: 240px;
}

.placeholder-warn {
  color: #d97706;
  font-size: 0.88rem;
  font-weight: 500;
}

.error-alert {
  margin: 10px 0 20px;
}

.error-list {
  margin: 8px 0 0;
  padding-left: 20px;
}

.error-list li {
  line-height: 1.9;
}

.actions {
  display: flex;
  gap: 16px;
  justify-content: flex-end;
  margin-top: 10px;
}

.tip {
  margin: 14px 0 0;
  font-size: 0.82rem;
  color: var(--slate-400);
  text-align: right;
}
</style>
