// Ruta: apps/admin/src/lib/api/client.ts
import axios from 'axios';
import { useAuthStore } from '@/store/authStore';

const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api', // Apunta a la URL del API Gateway
});

// Interceptor: Esto es magia. Se ejecuta ANTES de cada petición.
apiClient.interceptors.request.use(
  (config) => {
    const { accessToken } = useAuthStore.getState();
    if (accessToken) {
      // Si hay un token en nuestro store, lo añade a la cabecera
      config.headers.Authorization = `Bearer ${accessToken}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export default apiClient;
