import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

const AUTH_TOKEN_KEY = 'employeehub_token';
const existingToken = typeof window !== 'undefined' ? localStorage.getItem(AUTH_TOKEN_KEY) : null;
if (existingToken) {
  api.defaults.headers.common.Authorization = `Bearer ${existingToken}`;
}

export const setAuthToken = (token: string | null) => {
  if (token) {
    api.defaults.headers.common.Authorization = `Bearer ${token}`;
  } else {
    delete api.defaults.headers.common.Authorization;
  }
};

export default api;
