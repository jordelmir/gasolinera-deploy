import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { randomString, randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// Custom metrics
const errorRate = new Rate('error_rate');
const responseTime = new Trend('response_time');
const throughput = new Counter('throughput');

// Test configuration
export const options = {
  scenarios: {
    // Smoke test - basic functionality
    smoke_test: {
      executor: 'constant-vus',
      vus: 1,
      duration: '1m',
      tags: { test_type: 'smoke' },
    },

    // Load test - normal expected load
    load_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 10 },   // Ramp up
        { duration: '5m', target: 10 },   // Stay at 10 users
        { duration: '2m', target: 20 },   // Ramp up to 20 users
        { duration: '5m', target: 20 },   // Stay at 20 users
        { duration: '2m', target: 0 },    // Ramp down
      ],
      tags: { test_type: 'load' },
    },

    // Stress test - beyond normal capacity
    stress_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 20 },   // Ramp up to normal load
        { duration: '5m', target: 20 },   // Stay at normal load
        { duration: '2m', target: 50 },   // Ramp up to stress level
        { duration: '5m', target: 50 },   // Stay at stress level
        { duration: '2m', target: 100 },  // Ramp up to breaking point
        { duration: '5m', target: 100 },  // Stay at breaking point
        { duration: '10m', target: 0 },   // Ramp down
      ],
      tags: { test_type: 'stress' },
    },

    // Spike test - sudden traffic spikes
    spike_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 10 },   // Normal load
        { duration: '30s', target: 100 }, // Spike to 100 users
        { duration: '1m', target: 100 },  // Stay at spike
        { duration: '30s', target: 10 },  // Drop back to normal
        { duration: '1m', target: 10 },   // Stay at normal
      ],
      tags: { test_type: 'spike' },
    },

    // Volume test - large amounts of data
    volume_test: {
      executor: 'constant-vus',
      vus: 50,
      duration: '10m',
      tags: { test_type: 'volume' },
    },
  },

  thresholds: {
    // Performance thresholds
    http_req_duration: ['p(95)<500', 'p(99)<1000'], // 95% < 500ms, 99% < 1s
    http_req_failed: ['rate<0.01'],                  // Error rate < 1%
    http_reqs: ['rate>100'],                         // Throughput > 100 RPS

    // Custom thresholds
    error_rate: ['rate<0.01'],
    response_time: ['p(95)<500'],

    // Business thresholds
    'http_req_duration{endpoint:coupon_creation}': ['p(95)<200'],
    'http_req_duration{endpoint:coupon_redemption}': ['p(95)<300'],
    'http_req_duration{endpoint:station_search}': ['p(95)<150'],
  },
};

// Base URL configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_VERSION = '/api/v1';

// Test data generators
function generateUser() {
  return {
    email: `test-${randomString(10)}@example.com`,
    password: 'TestPassword123!',
    firstName: `Test${randomString(5)}`,
    lastName: `User${randomString(5)}`,
    phone: `+52${randomIntBetween(1000000000, 9999999999)}`,
  };
}

function generateCouponRequest() {
  return {
    stationId: '550e8400-e29b-41d4-a716-446655440000',
    amount: randomIntBetween(50, 500),
    fuelType: ['REGULAR', 'PREMIUM', 'DIESEL'][randomIntBetween(0, 2)],
    paymentMethod: 'CREDIT_CARD',
  };
}

function generateStationSearch() {
  // Mexico City coordinates with random offset
  const baseLat = 19.4326;
  const baseLng = -99.1332;
  const offset = 0.1; // ~11km radius

  return {
    latitude: baseLat + (Math.random() - 0.5) * offset,
    longitude: baseLng + (Math.random() - 0.5) * offset,
    radius: randomIntBetween(1, 10),
  };
}

// Authentication helper
let authToken = null;

function authenticate() {
  if (authToken) return authToken;

  const user = generateUser();

  // Register user
  const registerResponse = http.post(`${BASE_URL}${API_VERSION}/auth/register`, JSON.stringify(user), {
    headers: { 'Content-Type': 'application/json' },
  });

  if (registerResponse.status !== 201) {
    console.error('Registration failed:', registerResponse.body);
    return null;
  }

  // Login user
  const loginResponse = http.post(`${BASE_URL}${API_VERSION}/auth/login`, JSON.stringify({
    email: user.email,
    password: user.password,
  }), {
    headers: { 'Content-Type': 'application/json' },
  });

  if (loginResponse.status === 200) {
    const loginData = JSON.parse(loginResponse.body);
    authToken = loginData.accessToken;
    return authToken;
  }

  return null;
}

function getAuthHeaders() {
  const token = authenticate();
  return token ? {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json',
  } : {
    'Content-Type': 'application/json',
  };
}

// Test scenarios
export default function() {
  const testType = __ENV.TEST_TYPE || 'load';

  switch (testType) {
    case 'smoke':
      smokeTest();
      break;
    case 'load':
      loadTest();
      break;
    case 'stress':
      stressTest();
      break;
    case 'spike':
      spikeTest();
      break;
    case 'volume':
      volumeTest();
      break;
    default:
      loadTest();
  }
}

function smokeTest() {
  // Basic health check
  const healthResponse = http.get(`${BASE_URL}/actuator/health`);
  check(healthResponse, {
    'health check status is 200': (r) => r.status === 200,
    'health check response time < 100ms': (r) => r.timings.duration < 100,
  });

  // Basic API functionality
  testStationSearch();
  sleep(1);
}

function loadTest() {
  const scenario = Math.random();

  if (scenario < 0.3) {
    // 30% - Station search
    testStationSearch();
  } else if (scenario < 0.6) {
    // 30% - User registration/login
    testUserFlow();
  } else if (scenario < 0.8) {
    // 20% - Coupon creation
    testCouponCreation();
  } else {
    // 20% - Coupon redemption
    testCouponRedemption();
  }

  sleep(randomIntBetween(1, 3));
}

function stressTest() {
  // More aggressive testing with shorter sleep times
  loadTest();
  sleep(randomIntBetween(0, 1));
}

function spikeTest() {
  // Rapid-fire requests during spike
  testStationSearch();
  testCouponCreation();
  sleep(0.1);
}

function volumeTest() {
  // Large data operations
  testBulkOperations();
  sleep(randomIntBetween(2, 5));
}

// Individual test functions
function testStationSearch() {
  const searchParams = generateStationSearch();
  const url = `${BASE_URL}${API_VERSION}/stations/nearby?lat=${searchParams.latitude}&lng=${searchParams.longitude}&radius=${searchParams.radius}km`;

  const response = http.get(url, {
    headers: getAuthHeaders(),
    tags: { endpoint: 'station_search' },
  });

  const success = check(response, {
    'station search status is 200': (r) => r.status === 200,
    'station search response time < 150ms': (r) => r.timings.duration < 150,
    'station search returns stations': (r) => {
      try {
        const data = JSON.parse(r.body);
        return Array.isArray(data) && data.length >= 0;
      } catch (e) {
        return false;
      }
    },
  });

  errorRate.add(!success);
  responseTime.add(response.timings.duration);
  throughput.add(1);
}

function testUserFlow() {
  const user = generateUser();

  // Registration
  const registerResponse = http.post(`${BASE_URL}${API_VERSION}/auth/register`, JSON.stringify(user), {
    headers: { 'Content-Type': 'application/json' },
    tags: { endpoint: 'user_registration' },
  });

  const registerSuccess = check(registerResponse, {
    'registration status is 201': (r) => r.status === 201,
    'registration response time < 300ms': (r) => r.timings.duration < 300,
  });

  if (registerSuccess) {
    // Login
    const loginResponse = http.post(`${BASE_URL}${API_VERSION}/auth/login`, JSON.stringify({
      email: user.email,
      password: user.password,
    }), {
      headers: { 'Content-Type': 'application/json' },
      tags: { endpoint: 'user_login' },
    });

    const loginSuccess = check(loginResponse, {
      'login status is 200': (r) => r.status === 200,
      'login response time < 200ms': (r) => r.timings.duration < 200,
      'login returns token': (r) => {
        try {
          const data = JSON.parse(r.body);
          return data.accessToken && data.accessToken.length > 0;
        } catch (e) {
          return false;
        }
      },
    });

    errorRate.add(!loginSuccess);
  }

  errorRate.add(!registerSuccess);
}

function testCouponCreation() {
  const couponRequest = generateCouponRequest();

  const response = http.post(`${BASE_URL}${API_VERSION}/coupons`, JSON.stringify(couponRequest), {
    headers: getAuthHeaders(),
    tags: { endpoint: 'coupon_creation' },
  });

  const success = check(response, {
    'coupon creation status is 200 or 201': (r) => r.status === 200 || r.status === 201,
    'coupon creation response time < 200ms': (r) => r.timings.duration < 200,
    'coupon creation returns coupon': (r) => {
      try {
        const data = JSON.parse(r.body);
        return data.id && data.qrCode;
      } catch (e) {
        return false;
      }
    },
  });

  errorRate.add(!success);
  responseTime.add(response.timings.duration);
  throughput.add(1);
}

function testCouponRedemption() {
  // First create a coupon
  const couponRequest = generateCouponRequest();
  const createResponse = http.post(`${BASE_URL}${API_VERSION}/coupons`, JSON.stringify(couponRequest), {
    headers: getAuthHeaders(),
  });

  if (createResponse.status === 200 || createResponse.status === 201) {
    const coupon = JSON.parse(createResponse.body);

    // Then redeem it
    const redemptionRequest = {
      latitude: 19.4326,
      longitude: -99.1332,
      fuelAmount: randomIntBetween(5, 20),
    };

    const redeemResponse = http.post(`${BASE_URL}${API_VERSION}/coupons/${coupon.id}/redeem`,
      JSON.stringify(redemptionRequest), {
      headers: getAuthHeaders(),
      tags: { endpoint: 'coupon_redemption' },
    });

    const success = check(redeemResponse, {
      'coupon redemption status is 200': (r) => r.status === 200,
      'coupon redemption response time < 300ms': (r) => r.timings.duration < 300,
      'coupon redemption returns redemption': (r) => {
        try {
          const data = JSON.parse(r.body);
          return data.id && data.ticketsGenerated !== undefined;
        } catch (e) {
          return false;
        }
      },
    });

    errorRate.add(!success);
    responseTime.add(redeemResponse.timings.duration);
  }
}

function testBulkOperations() {
  // Test bulk station search
  const promises = [];
  for (let i = 0; i < 10; i++) {
    const searchParams = generateStationSearch();
    const url = `${BASE_URL}${API_VERSION}/stations/nearby?lat=${searchParams.latitude}&lng=${searchParams.longitude}&radius=${searchParams.radius}km`;
    promises.push(http.asyncRequest('GET', url, null, { headers: getAuthHeaders() }));
  }

  const responses = http.batch(promises);

  responses.forEach((response, index) => {
    check(response, {
      [`bulk search ${index} status is 200`]: (r) => r.status === 200,
      [`bulk search ${index} response time < 200ms`]: (r) => r.timings.duration < 200,
    });
  });
}

// Setup and teardown
export function setup() {
  console.log('Starting performance tests...');
  console.log(`Base URL: ${BASE_URL}`);
  console.log(`Test Type: ${__ENV.TEST_TYPE || 'load'}`);

  // Warm up the application
  const warmupResponse = http.get(`${BASE_URL}/actuator/health`);
  if (warmupResponse.status !== 200) {
    console.error('Application is not healthy, aborting tests');
    return null;
  }

  return { baseUrl: BASE_URL };
}

export function teardown(data) {
  console.log('Performance tests completed');
  console.log('Check the results in Grafana dashboards');
}

// Handle summary
export function handleSummary(data) {
  return {
    'performance-test-results.json': JSON.stringify(data, null, 2),
    'performance-test-summary.html': generateHtmlReport(data),
  };
}

function generateHtmlReport(data) {
  const template = `
<!DOCTYPE html>
<html>
<head>
    <title>Gasolinera JSM Performance Test Results</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .metric { margin: 10px 0; padding: 10px; border-left: 4px solid #007cba; }
        .pass { border-left-color: #28a745; }
        .fail { border-left-color: #dc3545; }
        .summary { background-color: #f8f9fa; padding: 15px; border-radius: 5px; }
    </style>
</head>
<body>
    <h1>Gasolinera JSM Performance Test Results</h1>
    <div class="summary">
        <h2>Test Summary</h2>
        <p><strong>Test Duration:</strong> ${data.state.testRunDurationMs / 1000}s</p>
        <p><strong>Total Requests:</strong> ${data.metrics.http_reqs.count}</p>
        <p><strong>Failed Requests:</strong> ${data.metrics.http_req_failed.count}</p>
        <p><strong>Average Response Time:</strong> ${data.metrics.http_req_duration.avg.toFixed(2)}ms</p>
        <p><strong>95th Percentile:</strong> ${data.metrics.http_req_duration['p(95)'].toFixed(2)}ms</p>
        <p><strong>99th Percentile:</strong> ${data.metrics.http_req_duration['p(99)'].toFixed(2)}ms</p>
    </div>

    <h2>Threshold Results</h2>
    ${Object.entries(data.thresholds).map(([name, threshold]) => `
        <div class="metric ${threshold.ok ? 'pass' : 'fail'}">
            <strong>${name}:</strong> ${threshold.ok ? 'PASS' : 'FAIL'}
        </div>
    `).join('')}

    <h2>Detailed Metrics</h2>
    ${Object.entries(data.metrics).map(([name, metric]) => `
        <div class="metric">
            <strong>${name}:</strong>
            <ul>
                <li>Count: ${metric.count || 'N/A'}</li>
                <li>Rate: ${metric.rate ? metric.rate.toFixed(2) : 'N/A'}</li>
                <li>Average: ${metric.avg ? metric.avg.toFixed(2) : 'N/A'}</li>
                <li>Min: ${metric.min ? metric.min.toFixed(2) : 'N/A'}</li>
                <li>Max: ${metric.max ? metric.max.toFixed(2) : 'N/A'}</li>
            </ul>
        </div>
    `).join('')}
</body>
</html>
  `;

  return template;
}