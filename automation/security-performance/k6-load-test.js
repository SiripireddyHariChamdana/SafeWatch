import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '10s', target: 100 }, // Quick ramp up to 100 users
    { duration: '1m', target: 100 },  // Steady baseline load for 1 minute
    { duration: '10s', target: 0 },   // Graceful ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'], // Latency thresholds
    http_req_failed: ['rate<0.05'],                 // Failure rate < 5%
  },
};

export default function () {
  const url = process.env.TARGET_URL || 'https://majestic-pudding-3979e7.netlify.app/';
  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const res = http.get(url, params);

  check(res, {
    'is status 200': (r) => r.status === 200,
    'response time < 1500ms': (r) => r.timings.duration < 1500,
  });

  sleep(0.5); // Pace requests to achieve continuous RPS
}
