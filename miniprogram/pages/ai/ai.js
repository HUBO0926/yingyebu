const { request, toastApiOffline } = require('../../utils/api');

Page({
  data: {
    copy: {
      botName: '\u5c0f\u6052',
      botRole: '\u667a\u80fd\u62a5\u4ef7\u673a\u5668\u4eba\u52a9\u624b',
      status: '\u7b2c\u4e00\u9636\u6bb5\u5360\u4f4d\u63a5\u53e3',
      placeholder: '\u95ee\u5c0f\u6052\u4ea7\u54c1\u3001\u65b9\u6848\u6216\u62a5\u4ef7\u95ee\u9898',
      send: '\u53d1\u9001'
    },
    suggestions: [
      '\u8fb9\u5761\u76d1\u6d4b\u9700\u8981\u54ea\u4e9b\u6838\u5fc3\u8bbe\u5907\uff1f',
      '\u5c3e\u77ff\u5e93\u76d1\u6d4b\u65b9\u6848\u600e\u4e48\u914d\u7f6e\uff1f',
      '\u5e2e\u6211\u4f30\u7b97\u4e00\u5957\u6c34\u5e93\u5927\u575d\u76d1\u6d4b\u62a5\u4ef7\u3002'
    ],
    question: '',
    loading: false,
    lastMessageId: 'msg-0',
    messages: [
      {
        id: 'msg-0',
        role: 'assistant',
        speaker: '\u5c0f\u6052',
        content: '\u4f60\u597d\uff0c\u6211\u662f\u5c0f\u6052\u3002\u53ef\u4ee5\u5e2e\u4f60\u68b3\u7406\u4ea7\u54c1\u53c2\u6570\u3001\u65b9\u6848\u914d\u7f6e\u3001\u62a5\u4ef7\u601d\u8def\u548c\u6280\u672f\u8d44\u6599\u3002\u76ee\u524d\u662f\u7b2c\u4e00\u9636\u6bb5\u5360\u4f4d\u52a9\u624b\u3002'
      }
    ]
  },
  updateQuestion(event) {
    this.setData({ question: event.detail.value });
  },
  useSuggestion(event) {
    this.setData({ question: event.currentTarget.dataset.question });
  },
  createMessage(role, content) {
    const id = `msg-${Date.now()}-${Math.floor(Math.random() * 1000)}`;
    return {
      id,
      role,
      speaker: role === 'assistant' ? this.data.copy.botName : '\u6211',
      content
    };
  },
  async ask() {
    if (this.data.loading) return;
    const question = this.data.question.trim();
    if (!question) {
      wx.showToast({ title: '\u8bf7\u8f93\u5165\u95ee\u9898', icon: 'none' });
      return;
    }
    const userMessage = this.createMessage('user', question);
    const nextMessages = [...this.data.messages, userMessage];
    this.setData({
      messages: nextMessages,
      question: '',
      loading: true,
      lastMessageId: userMessage.id
    });
    try {
      const result = await request('/ai/chat', { method: 'POST', data: { question } });
      const answer = result.answer || '\u6211\u5df2\u8bb0\u4e0b\u8fd9\u4e2a\u95ee\u9898\uff0c\u5f53\u524d\u7b2c\u4e00\u9636\u6bb5 AI \u95ee\u7b54\u4ec5\u505a\u5360\u4f4d\u3002';
      const assistantMessage = this.createMessage('assistant', answer);
      this.setData({
        messages: [...nextMessages, assistantMessage],
        loading: false,
        lastMessageId: assistantMessage.id
      });
    } catch (error) {
      toastApiOffline();
      const assistantMessage = this.createMessage(
        'assistant',
        '\u540e\u7aef\u672a\u542f\u52a8\uff0c\u5c0f\u6052\u5148\u4f7f\u7528\u5360\u4f4d\u56de\u590d\u3002\u7b2c\u4e00\u9636\u6bb5 AI \u77e5\u8bc6\u5e93\u4ec5\u9884\u7559\u95ee\u7b54\u63a5\u53e3\uff0c\u6682\u4e0d\u5b9e\u73b0\u771f\u5b9e\u5411\u91cf\u68c0\u7d22\u3002'
      );
      this.setData({
        messages: [...nextMessages, assistantMessage],
        loading: false,
        lastMessageId: assistantMessage.id
      });
    }
  }
});
