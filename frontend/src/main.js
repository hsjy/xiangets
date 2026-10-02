import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './assets/styles/main.css'

const app = createApp(App)

/** v-reveal：滚动渐现（IntersectionObserver，进入视口加 .revealed） */
app.directive('reveal', {
  mounted(el) {
    el.classList.add('reveal-init')
    const io = new IntersectionObserver(
      (entries) => entries.forEach((e) => {
        if (e.isIntersecting) {
          e.target.classList.add('revealed')
          io.unobserve(e.target)
        }
      }),
      { threshold: 0.12 }
    )
    io.observe(el)
  }
})

app.use(createPinia())
app.use(router)
app.mount('#app')
