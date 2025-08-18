import http from 'k6/http';
import { check } from 'k6';

function getRandomUserId(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

export let options = {
    scenarios: {
        constant_request_rate: {
            executor: 'ramping-arrival-rate',
            startRate: 167,  // 초당 약 167개의 요청으로 시작 (1분 동안 10,000 요청)
            timeUnit: '1s',
            preAllocatedVUs: 50,  // 초기 할당 VUs 수
            maxVUs: 100,  // 최대 VUs 수
            stages: [
                { duration: '1m', target: 167 },  // 1분 동안 초당 약 167개의 요청을 유지
            ],
        },
    },
};

//
// export default function () {
//     const userId = getRandomUserId(1, 10000);
//     const payload = JSON.stringify({
//         userId: userId
//     });
//     const params = {
//         headers: {
//             'Content-Type': 'application/json',
//         },
//     };
//
//     let response = http.post('http://host.docker.internal:8080/api/v1/user-access-logs', payload, params);
//
//     check(response, {
//         'status is 204': (r) => r.status === 204,
//     });
// }