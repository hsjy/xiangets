// 右键菜单：在弦歌中搜索选中的文字
chrome.runtime.onInstalled.addListener(() => {
  chrome.contextMenus.create({
    id: 'xiangets-search',
    title: '在弦歌中搜索「%s」',
    contexts: ['selection']
  })
})

chrome.contextMenus.onClicked.addListener((info) => {
  if (info.menuItemId === 'xiangets-search' && info.selectionText) {
    chrome.tabs.create({ url: `${API_BASE.replace('/api', '')}/#/poems?keyword=${encodeURIComponent(info.selectionText)}` })
  }
})
