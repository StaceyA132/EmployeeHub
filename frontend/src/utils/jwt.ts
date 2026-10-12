export function parseJwtPayload(token: string) {
  try {
    const base64Payload = token.split('.')[1];
    if (!base64Payload) {
      return null;
    }
    const payload = atob(base64Payload.replace(/-/g, '+').replace(/_/g, '/'));
    return JSON.parse(decodeURIComponent(escape(payload)));
  } catch {
    return null;
  }
}

export function getJwtRoles(token: string): string[] {
  const payload = parseJwtPayload(token);
  if (!payload || !Array.isArray(payload.roles)) {
    return [];
  }
  return payload.roles;
}

export function getJwtUsername(token: string): string {
  return parseJwtPayload(token)?.sub ?? '';
}

export function isJwtExpired(token: string): boolean {
  const payload = parseJwtPayload(token);
  if (!payload || typeof payload.exp !== 'number') {
    return true;
  }
  return payload.exp * 1000 <= Date.now();
}
