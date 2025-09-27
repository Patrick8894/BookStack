import { useNuxtApp } from '#app';

export async function login(username: string, password: string) {
  const { $api } = useNuxtApp();
  const res = await $api.post('/auth/login', { username, password });
  return res.data; // { token, user }
}

export async function register(userData: any) {
  const { $api } = useNuxtApp();
  const res = await $api.post('/auth/register', userData);
  return res.data; // user info
}
