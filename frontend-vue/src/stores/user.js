import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const user = ref(null)
  const streakDays = ref(0)

  function setUser(userData) {
    user.value = userData
    if (userData?.streakDays !== undefined) {
      streakDays.value = userData.streakDays
    }
  }

  function clearUser() {
    user.value = null
    streakDays.value = 0
  }

  function updateStreak(days) {
    streakDays.value = days
    if (user.value) {
      user.value.streakDays = days
    }
  }

  return { user, streakDays, setUser, clearUser, updateStreak }
})
