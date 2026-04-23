import http from 'k6/http';
import { check, sleep } from 'k6';
import crypto from 'k6/crypto';
import encoding from 'k6/encoding';
import { Counter } from 'k6/metrics';

const baseUrl = (__ENV.BASE_URL || 'http://host.docker.internal:8080').replace(/\/$/, '');
const adminEmail = __ENV.ADMIN_EMAIL || 'admin@travel-plan.com';
const adminPassword = __ENV.ADMIN_PASSWORD || 'admin123';
const jwtSecret = __ENV.JWT_SECRET;

const vus = Number(__ENV.K6_VUS || 10);
const duration = __ENV.K6_DURATION || '60s';
const p95Threshold = __ENV.K6_P95_THRESHOLD || '800';
const errorRateThreshold = __ENV.K6_ERROR_RATE_THRESHOLD || '0.05';
const mode = (__ENV.K6_MODE || 'capacity').toLowerCase();

const httpStatus2xx = new Counter('http_status_2xx');
const httpStatus4xx = new Counter('http_status_4xx');
const httpStatus5xx = new Counter('http_status_5xx');
const httpStatus200 = new Counter('http_status_200');
const httpStatus401 = new Counter('http_status_401');
const httpStatus403 = new Counter('http_status_403');
const httpStatus429 = new Counter('http_status_429');
const httpStatus5xxExact = new Counter('http_status_5xx_exact');

const successThreshold = mode === 'protection' ? 'rate>0.10' : 'rate>0.95';

export const options = {
  vus,
  duration,
  thresholds: {
    http_req_failed: [`rate<${errorRateThreshold}`],
    http_req_duration: [`p(95)<${p95Threshold}`],
    checks: [successThreshold],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'max'],
};

function registerStatus(status) {
  if (status >= 200 && status < 300) {
    httpStatus2xx.add(1);
  }
  if (status >= 400 && status < 500) {
    httpStatus4xx.add(1);
  }
  if (status >= 500) {
    httpStatus5xx.add(1);
    httpStatus5xxExact.add(1);
  }

  if (status === 200) {
    httpStatus200.add(1);
  }
  if (status === 401) {
    httpStatus401.add(1);
  }
  if (status === 403) {
    httpStatus403.add(1);
  }
  if (status === 429) {
    httpStatus429.add(1);
  }
}

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

  registerStatus(response.status);

  const isProtectionAccepted = mode === 'protection' && (response.status === 200 || response.status === 429);
  const isCapacityAccepted = response.status === 200;
  const isAcceptedStatus = mode === 'protection' ? isProtectionAccepted : isCapacityAccepted;

  check(response, {
    'payments status accepted by mode': () => isAcceptedStatus,
    'payments response is successful': (res) => {
      if (mode === 'protection' && res.status === 429) {
        return true;
      }
      const body = res.json();
      return Boolean(body && body.success === true);
    },
  });

  sleep(1);
}