/**
 * K6 Performance Tests for Gasolinera JSM
 * Tests critical endpoints under load to validate scalability
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('error_rate');
const responseTime = new Trend('response_time');
const requestCount = new Counter('request_count');

// Test configuration
export const options = {
  stages: [
    { duration: '2m', target: 10 },   // Ramp up to 10 users
    { duration: '5m', target: 10 },   // Stay at 10 users
    { duration: '2m', target: 20 },   // Ramp up to 20 users
    { duration: '5m', target: 20 },   // Stay at 20 users
    { duration: '2m', target: 50 },   // Ramp up to 50 users
    { duration: '5m', target: 50 },   // Stay at 50 users
    { duration: '5m', target: 0 },    // Ramp down to 0 users
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'], // 95% of requests should be below 2s
    http_req_failed: ['rate<0.05'],    // Error rate should be below 5%
    error_rate: ['rate<0.05'],         // Custom error rate should be below 5%
  },
};

// Test data
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api/v1';
const TEST_USERS = generateTestUsers(100);
const TEST_STATIONS = generateTestStations(20);

let userTokens = new Map();

export function setup() {
  console.log('Setting up performance test environment...');

  // Pre-create test users and get their tokens
  const setupUsers = TEST_USERS.slice(0, 10);
  const tokens = {};

  setupUsers.forEach((user, index) => {
    const registrationResponse = registerUser(user);
    if (registrationResponse.status === 201) {
      const loginResponse = loginUser(user);
      if (loginResponse.status === 200) {
        tokens[user.email] = JSON.parse(loginResponse.body).accessToken;
      }
    }
    sleep(0.1); // Small delay to avoid overwhelming the system during setup
  });

  console.log(`Setup completed. Created ${Object.keys(tokens).length} test users.`);
  return { tokens };
}

export default function(data) {
  const user = TEST_USERS[Math.floor(Math.random() * TEST_USERS.length)];
  const station = TEST_STATIONS[Math.floor(Math.random() * TEST_STATIONS.length)];

  // Get or create user token
  let token = data.tokens[user.email];
  if (!token) {
    token = getOrCreateUserToken(user);
    if (token) {
      data.tokens[user.email] = token;
    }
  }

  if (!token) {
    console.log(`Failed to get token for user ${user.email}`);
    return;
  }

  // Execute test scenario
  executeUserJourneyScenario(user, station, token);
}

export function teardown(data) {
  console.log('Cleaning up performance test environment...');
  // Cleanup logic if needed
}

// Test Scenarios
function executeUserJourneyScenario(user, station, token) {
  const scenario = Math.random();

  if (scenario < 0.4) {
    // 40% - Complete coupon flow
    executeCouponFlow(user, station, token);
  } else if (scenario < 0.7) {
    // 30% - Browse and search
    executeBrowsingFlow(user, token);
  } else if (scenario < 0.9) {
    // 20% - Dashboard and profile
    executeDashboardFlow(user, token);
  } else {
    // 10% - Raffle participation
    executeRaffleFlow(user, token);
  }
}

function executeCouponFlow(user, station, token) {
  const headers = {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json',
  };

  // Step 1: Search for stations
  const stationSearchResponse = http.get(
    `${BASE_URL}/stations/nearby?latitude=${station.latitude}&longitude=${station.longitude}&radius=10`,
    { headers }
  );

  check(stationSearchResponse, {
    'station search status is 200': (r) => r.status === 200,
    'station search response time < 1s': (r) => r.timings.duration < 1000,
  });

  errorRate.add(stationSearchResponse.status !== 200);
  responseTime.add(stationSearchResponse.timings.duration);
  requestCount.add(1);

  if (stationSearchResponse.status !== 200) return;

  sleep(1);

  // Step 2: Purchase coupon
  const couponPurchasePayload = {
    stationId: station.id,
    amount: 500.00,
    fuelType: 'REGULAR',
    paymentMethod: 'CREDIT_CARD',
    paymentToken: `test_token_${Date.now()}`
  };

  const couponPurchaseResponse = http.post(
    `${BASE_URL}/coupons/purchase`,
    JSON.stringify(couponPurchasePayload),
    { headers }
  );

  check(couponPurchaseResponse, {
    'coupon purchase status is 201': (r) => r.status === 201,
    'coupon purchase response time < 2s': (r) => r.timings.duration < 2000,
    'coupon has QR code': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.qrCode && body.qrCode.length > 0;
      } catch (e) {
        return false;
      }
    },
  });

  errorRate.add(couponPurchaseResponse.status !== 201);
  responseTime.add(couponPurchaseResponse.timings.duration);
  requestCount.add(1);

  if (couponPurchaseResponse.status !== 201) return;

  const couponData = JSON.parse(couponPurchaseResponse.body);
  sleep(2);

  // Step 3: Redeem coupon
  const redemptionPayload = {
    qrCode: couponData.qrCode,
    stationId: station.id,
    fuelAmount: 25.5,
    pricePerLiter: 22.50
  };

  const redemptionResponse = http.post(
    `${BASE_URL}/coupons/redeem`,
    JSON.stringify(redemptionPayload),
    { headers }
  );

  check(redemptionResponse, {
    'coupon redemption status is 200': (r) => r.status === 200,
    'coupon redemption response time < 3s': (r) => r.timings.duration < 3000,
    'redemption generates tickets': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.ticketsGenerated && body.ticketsGenerated > 0;
      } catch (e) {
        return false;
      }
    },
  });

  errorRate.add(redemptionResponse.status !== 200);
  responseTime.add(redemptionResponse.timings.duration);
  requestCount.add(1);

  sleep(1);
}

function executeBrowsingFlow(user, token) {
  const headers = {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json',
  };

  // Browse user's coupons
  const couponsResponse = http.get(
    `${BASE_URL}/coupons?page=0&size=10&sort=createdAt,desc`,
    { headers }
  );

  check(couponsResponse, {
    'coupons list status is 200': (r) => r.status === 200,
    'coupons list response time < 1s': (r) => r.timings.duration < 1000,
  });

  errorRate.add(couponsResponse.status !== 200);
  responseTime.add(couponsResponse.timings.duration);
  requestCount.add(1);

  sleep(1);

  // Browse stations
  const stationsResponse = http.get(
    `${BASE_URL}/stations?page=0&size=20`,
    { headers }
  );

  check(stationsResponse, {
    'stations list status is 200': (r) => r.status === 200,
    'stations list response time < 1s': (r) => r.timings.duration < 1000,
  });

  errorRate.add(stationsResponse.status !== 200);
  responseTime.add(stationsResponse.timings.duration);
  requestCount.add(1);

  sleep(1);
}

function executeDashboardFlow(user, token) {
  const headers = {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json',
  };

  // Get dashboard data
  const dashboardResponse = http.get(
    `${BASE_URL}/dashboard/user`,
    { headers }
  );

  check(dashboardResponse, {
    'dashboard status is 200': (r) => r.status === 200,
    'dashboard response time < 1s': (r) => r.timings.duration < 1000,
    'dashboard has required fields': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.totalCoupons !== undefined &&
               body.totalTickets !== undefined &&
               body.totalSpent !== undefined;
      } catch (e) {
        return false;
      }
    },
  });

  errorRate.add(dashboardResponse.status !== 200);
  responseTime.add(dashboardResponse.timings.duration);
  requestCount.add(1);

  sleep(1);

  // Get user profile
  const profileResponse = http.get(
    `${BASE_URL}/auth/profile`,
    { headers }
  );

  check(profileResponse, {
    'profile status is 200': (r) => r.status === 200,
    'profile response time < 500ms': (r) => r.timings.duration < 500,
  });

  errorRate.add(profileResponse.status !== 200);
  responseTime.add(profileResponse.timings.duration);
  requestCount.add(1);

  sleep(1);

  // Get statistics
  const statsResponse = http.get(
    `${BASE_URL}/coupons/statistics`,
    { headers }
  );

  check(statsResponse, {
    'statistics status is 200': (r) => r.status === 200,
    'statistics response time < 1s': (r) => r.timings.duration < 1000,
  });

  errorRate.add(statsResponse.status !== 200);
  responseTime.add(statsResponse.timings.duration);
  requestCount.add(1);
}

function executeRaffleFlow(user, token) {
  const headers = {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json',
  };

  // Get active raffles
  const rafflesResponse = http.get(
    `${BASE_URL}/raffles/active`,
    { headers }
  );

  check(rafflesResponse, {
    'raffles status is 200': (r) => r.status === 200,
    'raffles response time < 1s': (r) => r.timings.duration < 1000,
  });

  errorRate.add(rafflesResponse.status !== 200);
  responseTime.add(rafflesResponse.timings.duration);
  requestCount.add(1);

  if (rafflesResponse.status === 200) {
    try {
      const raffles = JSON.parse(rafflesResponse.body).raffles;
      if (raffles && raffles.length > 0) {
        const raffle = raffles[0];

        sleep(1);

        // Participate in raffle (if user has tickets)
        const participationPayload = {
          raffleId: raffle.id,
          ticketsToUse: Math.floor(Math.random() * 5) + 1
        };

        const participationResponse = http.post(
          `${BASE_URL}/raffles/participate`,
          JSON.stringify(participationPayload),
          { headers }
        );

        check(participationResponse, {
          'raffle participation response time < 2s': (r) => r.timings.duration < 2000,
        });

        responseTime.add(participationResponse.timings.duration);
        requestCount.add(1);
      }
    } catch (e) {
      console.log('Error parsing raffles response:', e);
    }
  }
}

// Helper functions
function getOrCreateUserToken(user) {
  // Try to login first
  const loginResponse = loginUser(user);
  if (loginResponse.status === 200) {
    return JSON.parse(loginResponse.body).accessToken;
  }

  // If login fails, try to register
  const registrationResponse = registerUser(user);
  if (registrationResponse.status === 201) {
    sleep(0.5); // Brief delay after registration
    const loginResponse = loginUser(user);
    if (loginResponse.status === 200) {
      return JSON.parse(loginResponse.body).accessToken;
    }
  }

  return null;
}

function registerUser(user) {
  const payload = {
    email: user.email,
    phone: user.phone,
    firstName: user.firstName,
    lastName: user.lastName,
    password: user.password
  };

  return http.post(
    `${BASE_URL}/auth/register`,
    JSON.stringify(payload),
    {
      headers: { 'Content-Type': 'application/json' },
      timeout: '10s'
    }
  );
}

function loginUser(user) {
  const payload = {
    identifier: user.email,
    password: user.password
  };

  return http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify(payload),
    {
      headers: { 'Content-Type': 'application/json' },
      timeout: '10s'
    }
  );
}

function generateTestUsers(count) {
  const users = [];
  for (let i = 0; i < count; i++) {
    users.push({
      email: `perftest.user.${i}@gasolinera-test.com`,
      phone: `555${String(1000000 + i).padStart(7, '0')}`,
      firstName: `PerfTest${i}`,
      lastName: 'User',
      password: 'PerfTestPassword123!'
    });
  }
  return users;
}

function generateTestStations(count) {
  const stations = [];
  const baseLatitude = 19.4326;
  const baseLongitude = -99.1332;

  for (let i = 0; i < count; i++) {
    stations.push({
      id: `station-${i}`,
      name: `Test Station ${i}`,
      latitude: baseLatitude + (Math.random() - 0.5) * 0.1,
      longitude: baseLongitude + (Math.random() - 0.5) * 0.1
    });
  }
  return stations;
}

// Stress test configuration
export const stressTestOptions = {
  stages: [
    { duration: '1m', target: 100 },   // Ramp up to 100 users
    { duration: '5m', target: 100 },   // Stay at 100 users
    { duration: '1m', target: 200 },   // Ramp up to 200 users
    { duration: '5m', target: 200 },   // Stay at 200 users
    { duration: '1m', target: 300 },   // Ramp up to 300 users
    { duration: '5m', target: 300 },   // Stay at 300 users
    { duration: '5m', target: 0 },     // Ramp down to 0 users
  ],
  thresholds: {
    http_req_duration: ['p(95)<5000'], // 95% of requests should be below 5s
    http_req_failed: ['rate<0.10'],    // Error rate should be below 10%
  },
};

// Spike test configuration
export const spikeTestOptions = {
  stages: [
    { duration: '2m', target: 10 },    // Normal load
    { duration: '1m', target: 500 },   // Spike to 500 users
    { duration: '2m', target: 500 },   // Stay at spike
    { duration: '1m', target: 10 },    // Return to normal
    { duration: '2m', target: 10 },    // Stay at normal
  ],
  thresholds: {
    http_req_duration: ['p(95)<10000'], // 95% of requests should be below 10s
    http_req_failed: ['rate<0.15'],     // Error rate should be below 15%
  },
};