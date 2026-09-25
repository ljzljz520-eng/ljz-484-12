import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Detail from '../views/Detail.vue'
import Read from '../views/Read.vue'
import Dashboard from '../views/author/Dashboard.vue'
import ChapterEditor from '../views/author/ChapterEditor.vue'

const routes = [
    {
        path: '/',
        name: 'Home',
        component: Home
    },
    {
        path: '/novel/:id',
        name: 'Detail',
        component: Detail
    },
    {
        path: '/chapter/:id',
        name: 'Read',
        component: Read
    },
    {
        path: '/author',
        name: 'AuthorDashboard',
        component: Dashboard
    },
    {
        path: '/author/chapter/:id',
        name: 'ChapterEditor',
        component: ChapterEditor
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router
