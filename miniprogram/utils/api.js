const app = getApp();

function request(path, options = {}) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${app.globalData.apiBase}${path}`,
      method: options.method || 'GET',
      data: options.data || {},
      timeout: 6000,
      header: {
        Authorization: app.globalData.token || ''
      },
      success(res) {
        const body = res.data || {};
        if (res.statusCode >= 400) {
          reject(new Error(body.message || '\u8bf7\u6c42\u5931\u8d25'));
          return;
        }
        if (body.success === false) {
          reject(new Error(body.message || '\u8bf7\u6c42\u5931\u8d25'));
          return;
        }
        resolve(body.data || body);
      },
      fail(error) {
        reject(error);
      }
    });
  });
}

function toastApiOffline() {
  wx.showToast({ title: '\u8bf7\u542f\u52a8\u540e\u7aef API', icon: 'none' });
}

module.exports = {
  request,
  toastApiOffline
};
