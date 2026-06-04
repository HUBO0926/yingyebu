import axios from 'axios';

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string;
}

export interface Category {
  id: number;
  name: string;
  sortOrder: number;
}

export interface FileRecord {
  originalName: string;
  objectKey: string;
  bucketName: string;
  fileSize: number;
  url: string;
  uploadedBy: string;
  uploadedAt: string;
  module: string;
}

export interface Product {
  id: number;
  categoryId: number;
  categoryName: string;
  name: string;
  model: string;
  summary: string;
  parameters: Record<string, string>;
  imageUrl: string;
  price: number | string;
  carouselImages: string[];
  detailHtml: string;
  attachments: FileRecord[];
  uploadedBy: string;
  updatedAt: string;
}

export interface Solution {
  id: number;
  title: string;
  industry: string;
  summary: string;
  architecture: string;
  imageUrl: string;
  recommendedProductIds: number[];
  carouselImages: string[];
  detailHtml: string;
  attachments: FileRecord[];
  uploadedBy: string;
  updatedAt: string;
}

export interface ProjectCase {
  id: number;
  name: string;
  location: string;
  industry: string;
  summary: string;
  imageUrl: string;
  carouselImages: string[];
  detailHtml: string;
  attachments: FileRecord[];
  uploadedBy: string;
  updatedAt: string;
}

export interface Inquiry {
  id: number;
  contactName: string;
  company: string;
  projectName: string;
  status: string;
  assignedSalesId: string;
  assignedSalesName: string;
  createdAt: string;
}

export interface SalesUser {
  id: string;
  name: string;
}

export interface QuoteSummary {
  id: number;
  quoteNo: string;
  projectName: string;
  customerName: string;
  ownerId: string;
  ownerName: string;
  totalAmount: number | string;
  status: string;
  createdAt: string;
}

export interface QuoteItemRow {
  measurement: string;
  categoryName: string;
  productName: string;
  quantity: number;
  unitPrice: number | string;
  amount: number | string;
}

export interface QuoteDetail {
  summary: QuoteSummary;
  quote: {
    id: number;
    quoteNo: string;
    projectName: string;
    discountRate: number | string;
    totalAmount: number | string;
    status: string;
  };
  items: QuoteItemRow[];
  wordPreview: {
    fileName: string;
    title: string;
    templateName: string;
    rows: QuoteItemRow[];
    totalAmount: number | string;
    footer: string;
  };
}

export interface QuoteMeasurement {
  name: string;
  categoryName: string;
  productName: string;
  quantity: number;
  unitPrice: number | string;
}

export interface QuoteTemplate {
  id: number;
  templateName: string;
  description: string;
  measurements: QuoteMeasurement[];
}

export interface Personnel {
  id: number;
  userId: string;
  openId: string;
  unionId: string;
  displayName: string;
  role: 'CUSTOMER' | 'SALES' | 'ENGINEER' | 'ADMIN';
  phone: string;
  status: string;
  remark: string;
  firstLoginAt: string;
  lastLoginAt: string;
}

export type WechatUser = Personnel;

export interface AiModelConfig {
  id: number;
  provider: string;
  providerCode: string;
  adapterType: string;
  baseUrl: string;
  chatModel: string;
  embeddingModel: string;
  maskedApiKey: string;
  keyStatus: string;
  enabled: boolean;
  remark: string;
  updatedAt: string;
}

export interface AiModelConfigPayload {
  provider: string;
  providerCode: string;
  adapterType: string;
  baseUrl: string;
  chatModel: string;
  embeddingModel: string;
  apiKey?: string;
  enabled: boolean;
  remark: string;
}

export interface AiConfigTestResult {
  ok: boolean;
  message: string;
  provider?: string;
  chatModel?: string;
}

export interface DashboardMetrics {
  inquiryCount: number;
  quoteCount: number;
  pendingInquiryCount: number;
  sentQuoteCount: number;
  browseCount: number;
  aiQuestionCount: number;
  recentInquiries: Inquiry[];
  recentQuotes: QuoteSummary[];
}

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000
});

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('adminToken');
  if (token) {
    config.headers = config.headers || {};
    config.headers.Authorization = token;
  }
  return config;
});

async function unwrap<T>(request: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  try {
    const response = await request;
    if (response.data.success === false) throw new Error(response.data.message || '请求失败');
    return response.data.data;
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '请求失败';
    throw new Error(message);
  }
}

export function getData<T>(url: string): Promise<T> {
  return unwrap(http.get<ApiResponse<T>>(url));
}

export function postData<T>(url: string, payload: unknown): Promise<T> {
  return unwrap(http.post<ApiResponse<T>>(url, payload));
}

export function putData<T>(url: string, payload: unknown): Promise<T> {
  return unwrap(http.put<ApiResponse<T>>(url, payload));
}

export function deleteData<T>(url: string): Promise<T> {
  return unwrap(http.delete<ApiResponse<T>>(url));
}

export function uploadFile(file: File, module = 'common', uploadedBy = '管理员'): Promise<FileRecord> {
  const formData = new FormData();
  formData.append('file', file);
  const query = new URLSearchParams({ module, uploadedBy });
  return unwrap(http.post<ApiResponse<FileRecord>>(`/files?${query.toString()}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 30000
  }));
}
