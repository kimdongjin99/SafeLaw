import axios, {
  AxiosInstance,
  InternalAxiosRequestConfig,
  AxiosResponse,
  AxiosError,
} from 'axios';
import EncryptedStorage from 'react-native-encrypted-storage';

const apiClient: AxiosInstance = axios.create({
  baseURL: 'http://52.78.89.29:8080',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// API 명세서 규칙: /api/v1/users/ 로 시작하는 요청에만 Authorization 헤더 필요
apiClient.interceptors.request.use(
  async (config: InternalAxiosRequestConfig): Promise<InternalAxiosRequestConfig> => {
    const url = config.url || '';
    const requiresAuth = url.startsWith('/api/v1/users/');

    if (requiresAuth) {
      try {
        const token = await EncryptedStorage.getItem('accessToken');
        if (token && config.headers) {
          config.headers.Authorization = `Bearer ${token}`;
        }
      } catch (error) {
        console.error('토큰 로드 실패:', error);
      }
    } else {
      if (config.headers) {
        delete config.headers.Authorization;
      }
    }

    return config;
  },
  (error: AxiosError) => Promise.reject(error)
);

apiClient.interceptors.response.use(
  (response: AxiosResponse) => response,
  async (error: AxiosError) => {
    if (error.response?.status === 401) {
      const url = error.config?.url || '';
      // /api/v1/users/ (회원 정보 API)에서 401 발생 시 토큰 삭제 후 로그인 유도
      if (url.startsWith('/api/v1/users/')) {
        await EncryptedStorage.removeItem('accessToken');
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;