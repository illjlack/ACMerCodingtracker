// 持久化 allPbInfo 页面设置的 store 模块

// 从 localStorage 获取设置
function getSettingsFromStorage() {
  try {
    const settings = localStorage.getItem('allPbInfo_settings')
    return settings ? JSON.parse(settings) : {}
  } catch (error) {
    console.error('Failed to parse allPbInfo settings from localStorage:', error)
    return {}
  }
}

// 保存设置到 localStorage
function saveSettingsToStorage(settings) {
  try {
    localStorage.setItem('allPbInfo_settings', JSON.stringify(settings))
  } catch (error) {
    console.error('Failed to save allPbInfo settings to localStorage:', error)
  }
}

const defaultSettings = {
  startDate: new Date(2021, 0, 1), // 2021年1月1日
  endDate: new Date(), // 当前时间
  selectedPlatforms: [], // 选中的平台
  selectedTags: [], // 选中的标签
  search: '' // 搜索关键词
}

const storedSettings = getSettingsFromStorage()

const state = {
  // 合并默认设置和存储的设置
  ...defaultSettings,
  ...storedSettings,
  // 确保日期对象正确
  startDate: storedSettings.startDate ? new Date(storedSettings.startDate) : defaultSettings.startDate,
  endDate: storedSettings.endDate ? new Date(storedSettings.endDate) : defaultSettings.endDate
}

const mutations = {
  SET_START_DATE: (state, date) => {
    state.startDate = date
    saveSettingsToStorage(state)
  },
  SET_END_DATE: (state, date) => {
    state.endDate = date
    saveSettingsToStorage(state)
  },
  SET_SELECTED_PLATFORMS: (state, platforms) => {
    state.selectedPlatforms = platforms
    saveSettingsToStorage(state)
  },
  SET_SELECTED_TAGS: (state, tags) => {
    state.selectedTags = tags
    saveSettingsToStorage(state)
  },
  SET_SEARCH: (state, search) => {
    state.search = search
    saveSettingsToStorage(state)
  },
  RESET_SETTINGS: (state) => {
    Object.assign(state, defaultSettings)
    localStorage.removeItem('allPbInfo_settings')
  }
}

const actions = {
  setStartDate({ commit }, date) {
    commit('SET_START_DATE', date)
  },
  setEndDate({ commit }, date) {
    commit('SET_END_DATE', date)
  },
  setSelectedPlatforms({ commit }, platforms) {
    commit('SET_SELECTED_PLATFORMS', platforms)
  },
  setSelectedTags({ commit }, tags) {
    commit('SET_SELECTED_TAGS', tags)
  },
  setSearch({ commit }, search) {
    commit('SET_SEARCH', search)
  },
  resetSettings({ commit }) {
    commit('RESET_SETTINGS')
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
