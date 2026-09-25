import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Detail from '../views/Detail.vue'
import Read from '../views/Read.vue'
import Write from '../views/Write.vue'

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
        path: '/novel/:id/write',
        name: 'WriteNewChapter',
        component: Write
    },
    {
        path: '/novel/:id/write/:chapterId',
        name: 'EditChapter',
        component: Write
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router
