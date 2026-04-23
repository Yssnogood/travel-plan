import http from 'k6/http';
import { check, sleep } from 'k6';
import crypto from 'k6/crypto';
import encoding from 'k6/encoding';

const baseUrl = (__ENV.BASE_URL || 'http://host.docker.internal:8080').replace(/\/$/, '');
const adminEmail = __ENV.ADMIN_EMAIL || 'admin@travel-plan.com';
const adminPassword = __ENV.ADMIN_PASSWORD || 'admin123';
const jwtSecret = __ENV.JWT_SECRET;

const vus = Number(__ENV.K6_VUS || 10);
const duration = __ENV.K6_DURATION || '60s';
const p95Threshold = __ENV.K6_P95_THRESHOLD || '800';
const errorRateThreshold = __ENV.K6_ERROR_RATE_THRESHOLD || '0.05';

export const options = {
  vus,
  duration,
  thresholds: {
    http_req_failed: [`rate<${errorRateThreshold}`],
    http_req_duration: [`p(95)<${p95Threshold}`],
    checks: ['rate>0.95'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'max'],
};

function createJwtToken(secret) {
  const now = Math.floor(Date.now() / 1000);
  const header = {
    alg: 'HS256',
    typ: 'JWT',
  };
  const payload = {
    userId: 1,
    email: adminEmail,
    firstName: 'System',
    lastName: 'Admin',
    role: 'ADMIN',
    permissions: ['admin:access', 'payments:read', 'payments:write'],
    sub: adminEmail,
    iat: now,
    exp: now + 3600,
  };

  const encodedHeader = encoding.b64encode(JSON.stringify(header), 'rawurl');
  const encodedPayload = encoding.b64encode(JSON.stringify(payload), 'rawurl');
  const unsignedToken = `${encodedHeader}.${encodedPayload}`;
  const signature = crypto.hmac('sha256', secret, unsignedToken, 'base64rawurl');

  return `${unsignedToken}.${signature}`;
}

export function setup() {
  if (jwtSecret) {
    return {
      accessToken: createJwtToken(jwtSecret),
    };
  }

  const loginPayload = JSON.stringify({
    email: adminEmail,
    password: adminPassword,
  });

  const loginResponse = http.post(`${baseUrl}/api/v1/auth/login`, loginPayload, {
    headers: {
      'Content-Type': 'application/json',
    },
    tags: {
      name: 'auth_login',
    },
  });

  check(loginResponse, {
    'login returns 200': (response) => response.status === 200,
    'login returns access token': (response) => {
      const body = response.json();
      return Boolean(body && body.data && body.data.accessToken);
    },
  });

  if (loginResponse.status !== 200) {
    throw new Error(`Unable to authenticate admin user. HTTP ${loginResponse.status}: ${loginResponse.body}`);
  }

  const body = loginResponse.json();
  return {
    accessToken: body.data.accessToken,
  };
}

export default function (data) {
  const response = http.get(`${baseUrl}/api/v1/payments?page=0&size=10`, {
    headers: {
      Authorization: `Bearer ${data.accessToken}`,
      Accept: 'application/json',
    },
    tags: {
      name: 'admin_list_payments',
    },
  });

  check(response, {
    'payments returns 200': (res) => res.status === 200,
    'payments response is successful': (res) => {
      const body = res.json();
      return Boolean(body && body.success === true);
    },
  });

  sleep(1);
}