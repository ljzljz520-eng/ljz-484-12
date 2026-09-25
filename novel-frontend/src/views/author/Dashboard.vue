<template>
  <div class="container dashboard">
    <div class="page-head">
      <div>
        <h1 class="page-title text-gradient">作者控制台</h1>
        <p class="page-sub">管理你的小说与章节，发布前系统会自动检查标题、字数与占位符</p>
      </div>
    </div>

    <el-tabs v-model="activeNovelId" @tab-change="loadChapters" class="novel-tabs">
      <el-tab-pane
        v-for="novel in novels"
        :key="novel.id"
 :label="novel.title"
        :name="String(novel.id)"
      />
    </el-tabs>

    <div v-loading="loading" class="glass-panel chapters-panel">
      <div class="panel-head">
        <h2 class="section-title">章节管理</h2>
        <el-button type="primary" round :icon="Plus" @click="createChapter">
          新建章节
        </el-button>
      </div>

      <el-table :data="chapters" style="width: 100%" empty-text="还没有章节，点击右上角新建">
        <el-table-column label="序号" width="80">
          <template #default="{ row }">{{ formatNumber(row.orderNo) }}</template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="220">
          <template #default="{ row }">
            <span :class="{ 'muted-title': !row.title }">{{ row.title || '（未命名草稿）' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="字数" width="100">
          <template #default="{ row }">{{ countWords(row.content) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'PUBLISHED'" type="success" effect="light" round>已发布</el-tag>
            <el-tag v-else type="info" effect="plain" round>草稿</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="editChapter(row.id)">编辑</el-button>
            <el-button
              v-if="row.status !== 'PUBLISHED'"
              text
              type="success"
              @click="quickPublish(row)"
            >发布</el-button>
            <router-link v-else :to="'/chapter/' + row.id" class="read-link">阅读</router-link>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { countWords, validateChapter } from '../../utils/chapterValidation.js'

const router = useRouter()
const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'

const novels = ref([])
const chapters = ref([])
const activeNovelId = ref('')
const loading = ref(false)

const loadNovels = async () => {
  try {
    const res = await axios.get(`${API_URL}/novels`, { params: { page: 1, size: 100 } })
    novels.value = res.data.data
    if (novels.value.length > 0) {
      activeNovelId.value = String(novels.value[0].id)
      await loadChapters()
    }
  } catch (err) {
    ElMessage.error('加载小说列表失败')
  }
}

const loadChapters = async () => {
  if (!activeNovelId.value) return
  loading.value = true
  try {
    const res = await axios.get(`${API_URL}/author/novels/${activeNovelId.value}/chapters`)
    chapters.value = res.data
  } catch (err) {
    ElMessage.error('加载章节失败')
  } finally {
    loading.value = false
  }
}

const createChapter = async () => {
  try {
    const res = await axios.post(`${API_URL}/author/novels/${activeNovelId.value}/chapters`, {
      title: '',
      content: ''
    })
    ElMessage.success('已创建草稿，开始创作吧')
    router.push(`/author/chapter/${res.data.data.id}`)
  } catch (err) {
    ElMessage.error('创建章节失败')
  }
}

const editChapter = (id) => {
  router.push(`/author/chapter/${id}`)
}

// 列表里的快捷发布：浏览器先校验，不通过直接提示；通过后后端会再校验一次
const quickPublish = async (row) => {
  const errors = validateChapter(row.title, row.content)
  if (errors.length > 0) {
    ElMessage({ type: 'warning', message: errors[0], duration: 4000 })
    router.push(`/author/chapter/${row.id}`)
    return
  }
  try {
    const res = await axios.post(`${API_URL}/author/chapters/${row.id}/publish`, {
      title: row.title,
      content: row.content
    })
    ElMessage.success(res.data.message || '章节已发布')
    await loadChapters()
  } catch (err) {
    showServerErrors(err)
  }
}

// 统一展示后端返回的校验错误（后端兜底，错误信息以后端为准）
const showServerErrors = (err) => {
  const data = err.response && err.response.data
  if (data && Array.isArray(data.errors)) {
    data.errors.forEach((msg) => ElMessage({ type: 'error', message: msg, duration: 5000 }))
  } else {
    ElMessage.error((data && data.message) || '操作失败，请稍后重试')
  }
}

const formatNumber = (num) => String(num ?? 0).padStart(2, '0')

onMounted(loadNovels)
</script>

<style scoped>
.dashboard {
  padding-top: 40px;
}

.page-head {
  margin-bottom: 20px;
}

.page-title {
  font-size: 2.2rem;
  margin-bottom: 8px;
}

.page-sub {
  color: var(--text-sub);
  margin: 0;
}

.chapters-panel {
  padding: 30px;
  background: white;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-title {
  font-size: 1.3rem;
  margin: 0;
  padding-left: 10px;
  border-left: 4px solid var(--primary-color);
}

.muted-title {
  color: var(--slate-400);
}

.read-link {
  color: var(--primary-color);
  text-decoration: none;
  font-size: 14px;
  padding: 0 8px;
}

.novel-tabs {
  margin-bottom: 10px;
}
</style>
