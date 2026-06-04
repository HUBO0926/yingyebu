const { request } = require('../../utils/api');

Page({
  data: {
    copy: {
      title: '\u5ba2\u6237\u8be2\u4ef7',
      hint: '\u586b\u5199\u9879\u76ee\u57fa\u672c\u4fe1\u606f\uff0c\u9500\u552e\u4eba\u5458\u4f1a\u57fa\u4e8e\u6d4b\u9879\u4e0e\u4ea7\u54c1\u914d\u7f6e\u751f\u6210\u62a5\u4ef7\u3002',
      contactName: '\u8054\u7cfb\u4eba',
      phone: '\u624b\u673a\u53f7',
      company: '\u516c\u53f8\u540d\u79f0',
      projectName: '\u9879\u76ee\u540d\u79f0',
      projectLocation: '\u9879\u76ee\u5730\u70b9',
      requirement: '\u9700\u6c42\u63cf\u8ff0',
      submit: '\u63d0\u4ea4\u8be2\u4ef7'
    },
    form: {
      contactName: '',
      phone: '',
      company: '',
      projectName: '',
      projectLocation: '',
      requirement: ''
    }
  },
  updateField(event) {
    const field = event.currentTarget.dataset.field;
    this.setData({ [`form.${field}`]: event.detail.value });
  },
  validate() {
    const labels = {
      contactName: '\u8054\u7cfb\u4eba',
      phone: '\u624b\u673a\u53f7',
      company: '\u516c\u53f8\u540d\u79f0',
      projectName: '\u9879\u76ee\u540d\u79f0',
      projectLocation: '\u9879\u76ee\u5730\u70b9',
      requirement: '\u9700\u6c42\u63cf\u8ff0'
    };
    for (const key of Object.keys(labels)) {
      if (!this.data.form[key].trim()) {
        wx.showToast({ title: `\u8bf7\u586b\u5199${labels[key]}`, icon: 'none' });
        return false;
      }
    }
    if (!/^1\d{10}$/.test(this.data.form.phone.trim())) {
      wx.showToast({ title: '\u8bf7\u586b\u5199\u6b63\u786e\u624b\u673a\u53f7', icon: 'none' });
      return false;
    }
    return true;
  },
  async submit() {
    if (!this.validate()) return;
    wx.showLoading({ title: '\u63d0\u4ea4\u4e2d' });
    try {
      await request('/inquiries', { method: 'POST', data: this.data.form });
      wx.hideLoading();
      wx.showToast({ title: '\u5df2\u63d0\u4ea4' });
      setTimeout(() => wx.navigateBack(), 700);
    } catch (error) {
      wx.hideLoading();
      wx.showModal({
        title: '\u540e\u7aef\u672a\u542f\u52a8',
        content: '\u5df2\u4fdd\u7559\u672c\u5730\u6f14\u793a\u8868\u5355\uff1b\u542f\u52a8 http://localhost:8080/api \u540e\u53ef\u771f\u5b9e\u63d0\u4ea4\u3002',
        showCancel: false
      });
    }
  }
});
