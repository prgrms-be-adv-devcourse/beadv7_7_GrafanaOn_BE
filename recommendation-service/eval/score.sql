-- 추천 품질 지표. eval_label 에 라벨을 적재한 뒤 실행한다.
--   docker exec -i commerce-postgres psql -U root -d dear -f - < score.sql
--
-- 전제: recommendation.eval_pair (생성 시점에 만들어짐)
--       recommendation.eval_label (pair_id, label) — 사람이 매긴 값

\pset pager off

\echo '=== 0. 라벨링 진행률 ==='
SELECT count(*) AS 전체쌍,
       count(l.label) AS 라벨완료,
       count(*) - count(l.label) AS 남음
  FROM recommendation.eval_pair e
  LEFT JOIN recommendation.eval_label l
         ON l.pair_id = md5((e.qid * 100000 + e.cid)::text);

\echo ''
\echo '=== 1. Precision@5 — 지금 상위 5개가 맞는가 ==='
SELECT e.qtype AS 유형,
       count(*) AS "상위5쌍",
       sum(l.label) AS 적절,
       round(100.0 * sum(l.label) / nullif(count(*), 0), 1) AS "정밀도%"
  FROM recommendation.eval_pair e
  JOIN recommendation.eval_label l
    ON l.pair_id = md5((e.qid * 100000 + e.cid)::text)
 WHERE e.rk <= 5
 GROUP BY ROLLUP(e.qtype)
 ORDER BY e.qtype NULLS LAST;

\echo ''
\echo '=== 2. Threshold 를 어디에 둘 것인가 ==='
\echo '    적절/부적절 각각의 거리 분포. 겹치는 구간이 좁을수록 자를 지점이 뚜렷하다.'
SELECT CASE l.label WHEN 1 THEN '적절' ELSE '부적절' END AS 판정,
       count(*) AS 쌍,
       round(min(e.dist)::numeric, 3) AS 최소,
       round(percentile_cont(0.25) WITHIN GROUP (ORDER BY e.dist)::numeric, 3) AS p25,
       round(percentile_cont(0.50) WITHIN GROUP (ORDER BY e.dist)::numeric, 3) AS 중앙,
       round(percentile_cont(0.75) WITHIN GROUP (ORDER BY e.dist)::numeric, 3) AS p75,
       round(max(e.dist)::numeric, 3) AS 최대
  FROM recommendation.eval_pair e
  JOIN recommendation.eval_label l
    ON l.pair_id = md5((e.qid * 100000 + e.cid)::text)
 GROUP BY l.label
 ORDER BY l.label DESC;

\echo ''
\echo '=== 3. Threshold 후보별 성적 ==='
\echo '    통과: 그 값 이하로 잘랐을 때 남는 쌍. 정밀도가 높고 적절회수가 많을수록 좋다.'
WITH j AS (
  SELECT e.dist, l.label
    FROM recommendation.eval_pair e
    JOIN recommendation.eval_label l
      ON l.pair_id = md5((e.qid * 100000 + e.cid)::text)
), th(v) AS (VALUES (0.35), (0.38), (0.40), (0.42), (0.45), (0.48), (0.50))
SELECT th.v AS threshold,
       count(*) FILTER (WHERE j.dist < th.v) AS 통과,
       count(*) FILTER (WHERE j.dist < th.v AND j.label = 1) AS "통과중 적절",
       round(100.0 * count(*) FILTER (WHERE j.dist < th.v AND j.label = 1)
             / nullif(count(*) FILTER (WHERE j.dist < th.v), 0), 1) AS "정밀도%",
       round(100.0 * count(*) FILTER (WHERE j.dist < th.v AND j.label = 1)
             / nullif(count(*) FILTER (WHERE j.label = 1), 0), 1) AS "재현율%"
  FROM th CROSS JOIN j
 GROUP BY th.v
 ORDER BY th.v;

\echo ''
\echo '=== 4. 순위 구간별 적중률 — 몇 위까지 쓸만한가 ==='
SELECT e.band AS 구간,
       min(e.rk) || '~' || max(e.rk) AS 순위,
       count(*) AS 쌍,
       sum(l.label) AS 적절,
       round(100.0 * sum(l.label) / nullif(count(*), 0), 1) AS "적중%"
  FROM recommendation.eval_pair e
  JOIN recommendation.eval_label l
    ON l.pair_id = md5((e.qid * 100000 + e.cid)::text)
 GROUP BY e.band
 ORDER BY min(e.rk);

\echo ''
\echo '=== 5. 놓친 것 — 순위는 낮은데 적절하다고 매긴 쌍 ==='
\echo '    여기 많으면 거리만으로는 부족하다는 뜻이다.'
SELECT e.qid AS 기준, e.cid AS 후보, e.rk AS 순위, round(e.dist::numeric, 3) AS 거리,
       regexp_replace(left(s.story, 50), E'[\r\n]+', ' ', 'g') AS 후보story
  FROM recommendation.eval_pair e
  JOIN recommendation.eval_label l
    ON l.pair_id = md5((e.qid * 100000 + e.cid)::text)
  JOIN recommendation.product_vector s ON s.product_id = e.cid
 WHERE l.label = 1 AND e.rk > 10
 ORDER BY e.rk DESC
 LIMIT 15;
