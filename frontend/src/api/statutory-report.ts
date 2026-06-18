/** 法定报表导出 API */

import http from './http';

/** 下载社保申报表 */
export function downloadSocialInsuranceReport(periodMonth: string): Promise<void> {
  return downloadReport(`/reports/statutory/social-insurance?periodMonth=${periodMonth}`, `社保申报表_${periodMonth}.xlsx`);
}

/** 下载个税申报表 */
export function downloadIitReport(periodMonth: string): Promise<void> {
  return downloadReport(`/reports/statutory/iit?periodMonth=${periodMonth}`, `个税申报表_${periodMonth}.xlsx`);
}

/** 下载公积金申报表 */
export function downloadHousingFundReport(periodMonth: string): Promise<void> {
  return downloadReport(`/reports/statutory/housing-fund?periodMonth=${periodMonth}`, `公积金申报表_${periodMonth}.xlsx`);
}

/** 通用报表下载辅助函数 */
async function downloadReport(url: string, fileName: string): Promise<void> {
  const resp = await http.get(url, { responseType: 'blob' });
  const blob = new Blob([resp.data]);
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = fileName;
  link.click();
  URL.revokeObjectURL(link.href);
}
