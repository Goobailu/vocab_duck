# Debug Session: Dashboard-Review Mismatch

## Session Info
- Session ID: `dashboard-review-mismatch`
- Status: [OPEN]
- Started: 2026-07-02
- Description: Dashboard shows "暂无待复习单词" but Review Center shows 3+ words due for review

## Symptoms
- Expected: Dashboard "今日复习" and "待复习" should show same count as Review Center
- Actual: Dashboard shows 0, Review Center shows 3

## Reproduction Steps
1. Learn some new words in Learning page
2. Go to Review Center - shows 3 words due for review
3. Go back to Dashboard - shows "暂无待复习单词" and 0 in stats

## Hypotheses
1. **H1**: Backend `/api/dashboard/stats` returns 0 for `wordsDueReview` while `/api/learning/due-words` returns 3 words
2. **H2**: Frontend Dashboard.vue incorrectly parses the backend response
3. **H3**: There's a caching issue - frontend stores old data
4. **H4**: Different user contexts - API calls use different authentication
5. **H5**: Backend logic difference - Dashboard uses `countByUserIdAndNextReviewBeforeAndStatusIn` while Review uses `findDueWords`

## Evidence Collected
TBD

## Fix Applied
TBD

## Verification
TBD
