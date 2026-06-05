App({
  globalData: {
    localApiBase: 'http://localhost:8080/api',
    releaseApiBase: 'https://api.example.com/api',
    apiBase: 'https://api.example.com/api',
    token: '',
    currentUser: null,
    pendingQuotePreview: null,
    mockData: {
      industries: [
        '\u8fb9\u5761\u76d1\u6d4b',
        '\u5c3e\u77ff\u5e93\u76d1\u6d4b',
        '\u6c34\u5e93\u5927\u575d\u76d1\u6d4b',
        '\u5730\u8d28\u707e\u5bb3\u76d1\u6d4b',
        '\u667a\u6167\u77ff\u5c71'
      ],
      categories: [
        { id: 0, name: '\u5168\u90e8' },
        { id: 1, name: '\u4f20\u611f\u5668' },
        { id: 2, name: '\u91c7\u96c6\u4eea' },
        { id: 3, name: '\u901a\u4fe1' },
        { id: 4, name: '\u5b89\u88c5\u9644\u4ef6' },
        { id: 5, name: '\u4f9b\u7535\u7cfb\u7edf' },
        { id: 6, name: '\u5b89\u88c5\u8017\u6750' }
      ],
      products: [
        {
          id: 1,
          categoryId: 1,
          categoryName: '\u4f20\u611f\u5668',
          name: '\u667a\u80fd\u4f4d\u79fb\u4f20\u611f\u5668',
          model: 'PS-DIS-300',
          summary: '\u7528\u4e8e\u8fb9\u5761\u3001\u575d\u4f53\u548c\u77ff\u5c71\u7ed3\u6784\u4f4d\u79fb\u8fde\u7eed\u76d1\u6d4b\u3002',
          parameters: { '\u91cf\u7a0b': '300mm', '\u7cbe\u5ea6': '0.1mm', '\u9632\u62a4': 'IP67' },
          imageUrl: 'https://images.unsplash.com/photo-1581092160607-ee22621dd758?auto=format&fit=crop&w=900&q=80'
        },
        {
          id: 2,
          categoryId: 2,
          categoryName: '\u91c7\u96c6\u4eea',
          name: '\u591a\u901a\u9053\u6570\u636e\u91c7\u96c6\u4eea',
          model: 'PS-DAQ-16',
          summary: '\u652f\u6301\u591a\u6d4b\u70b9\u63a5\u5165\u3001\u8fb9\u7f18\u7f13\u5b58\u548c\u8fdc\u7a0b\u914d\u7f6e\u3002',
          parameters: { '\u901a\u9053': '16', '\u901a\u4fe1': '4G / \u4ee5\u592a\u7f51', '\u4f9b\u7535': 'DC12V' },
          imageUrl: 'https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=900&q=80'
        },
        {
          id: 3,
          categoryId: 3,
          categoryName: '\u901a\u4fe1',
          name: '\u5de5\u4e1a 4G \u901a\u4fe1\u7f51\u5173',
          model: 'PS-GW-4G',
          summary: '\u9762\u5411\u91ce\u5916\u9879\u76ee\u7684\u6570\u636e\u56de\u4f20\u548c\u8bbe\u5907\u8fdc\u7a0b\u7ef4\u62a4\u3002',
          parameters: { '\u7f51\u7edc': '4G Cat.4', '\u534f\u8bae': 'MQTT / HTTP', '\u6e29\u5ea6': '-20~70C' },
          imageUrl: 'https://images.unsplash.com/photo-1597852074816-d933c7d2b988?auto=format&fit=crop&w=900&q=80'
        },
        {
          id: 4,
          categoryId: 5,
          categoryName: '\u4f9b\u7535\u7cfb\u7edf',
          name: '\u592a\u9633\u80fd\u4f9b\u7535\u7bb1',
          model: 'PS-SOLAR-120',
          summary: '\u9002\u5408\u65e0\u5e02\u7535\u533a\u57df\u7684\u76d1\u6d4b\u7ad9\u6301\u7eed\u4f9b\u7535\u3002',
          parameters: { '\u529f\u7387': '120W', '\u7535\u6c60': '80Ah', '\u9632\u62a4': 'IP65' },
          imageUrl: 'https://images.unsplash.com/photo-1509391366360-2e959784a276?auto=format&fit=crop&w=900&q=80'
        }
      ],
      solutions: [
        {
          id: 1,
          title: '\u8fb9\u5761\u81ea\u52a8\u5316\u76d1\u6d4b\u65b9\u6848',
          industry: '\u8fb9\u5761\u76d1\u6d4b',
          summary: '\u4f4d\u79fb\u3001\u96e8\u91cf\u3001\u89c6\u9891\u548c\u91c7\u96c6\u7f51\u5173\u7ec4\u5408\uff0c\u9002\u5408\u65bd\u5de5\u671f\u4e0e\u8fd0\u8425\u671f\u76d1\u6d4b\u3002',
          imageUrl: 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1200&q=80'
        },
        {
          id: 2,
          title: '\u5c3e\u77ff\u5e93\u5b89\u5168\u76d1\u6d4b\u65b9\u6848',
          industry: '\u5c3e\u77ff\u5e93\u76d1\u6d4b',
          summary: '\u56f4\u7ed5\u575d\u4f53\u4f4d\u79fb\u3001\u6d78\u6da6\u7ebf\u3001\u5e93\u6c34\u4f4d\u4e0e\u89c6\u9891\u5de1\u68c0\u5f62\u6210\u95ed\u73af\u3002',
          imageUrl: 'https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?auto=format&fit=crop&w=1200&q=80'
        }
      ],
      cases: [
        {
          id: 1,
          name: '\u534e\u5317\u77ff\u533a\u8fb9\u5761\u76d1\u6d4b\u9879\u76ee',
          location: '\u6cb3\u5317 \u5510\u5c71',
          industry: '\u667a\u6167\u77ff\u5c71',
          summary: '\u90e8\u7f72 48 \u4e2a\u76d1\u6d4b\u70b9\uff0c\u5f62\u6210\u81ea\u52a8\u5316\u9884\u8b66\u548c\u65e5\u62a5\u5206\u6790\u3002',
          imageUrl: 'https://images.unsplash.com/photo-1473649085228-583485e6e4d7?auto=format&fit=crop&w=1200&q=80'
        },
        {
          id: 2,
          name: '\u897f\u5357\u6c34\u5e93\u5927\u575d\u5b89\u5168\u76d1\u6d4b',
          location: '\u4e91\u5357 \u6606\u660e',
          industry: '\u6c34\u5e93\u5927\u575d\u76d1\u6d4b',
          summary: '\u5b8c\u6210\u6e17\u538b\u3001\u4f4d\u79fb\u3001\u96e8\u91cf\u548c\u89c6\u9891\u8054\u52a8\u76d1\u6d4b\u3002',
          imageUrl: 'https://images.unsplash.com/photo-1437482078695-73f5ca6c96e2?auto=format&fit=crop&w=1200&q=80'
        }
      ]
    }
  }
});
