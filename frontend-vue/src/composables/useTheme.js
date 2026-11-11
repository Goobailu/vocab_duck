import { ref, watch } from 'vue'

const theme = ref(localStorage.getItem('vocab_theme') || 'dark')

function applyTheme(t) {
  document.documentElement.classList.remove('dark', 'light')
  document.documentElement.classList.add(t)
  document.documentElement.setAttribute('data-theme', t)
}

// Apply on load
applyTheme(theme.value)

watch(theme, (val) => {
  localStorage.setItem('vocab_theme', val)
  applyTheme(val)
})

export function useTheme() {
  function toggleTheme() {
    theme.value = theme.value === 'dark' ? 'light' : 'dark'
  }

  function setTheme(t) {
    theme.value = t
  }

  return { theme, toggleTheme, setTheme }
}
