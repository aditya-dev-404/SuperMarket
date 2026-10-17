/** Single place that reads build-time environment variables. */
export const env = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL ?? '',
  appName: 'SuperMarket',
};
