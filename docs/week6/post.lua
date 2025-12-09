wrk.method = "POST"
wrk.body   = [[
{
  "orderId": "1351039135",
  "cardType": "SAMSUNG",
  "cardNo": "1234-5678-9814-1451",
  "amount": "5000",
  "callbackUrl": "http://localhost:8080/api/v1/payments/callback"
}
]]
wrk.headers["Content-Type"] = "application/json"
wrk.headers["X-USER-ID"]    = "135135"
