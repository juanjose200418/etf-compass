const PROXY_CONFIG = {
  '/api/yahoo': {
    target: 'https://query2.finance.yahoo.com',
    secure: true,
    changeOrigin: true,
    pathRewrite: { '^/api/yahoo': '' },
  },
};
module.exports = PROXY_CONFIG;
