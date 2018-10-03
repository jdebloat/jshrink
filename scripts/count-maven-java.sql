Select count(repo_name) from `bigquery-public-data.github_repos.sample_repos` as sample
where repo_name in(
select distinct(t.repo_name) from
((SELECT repo_name FROM `bigquery-public-data.github_repos.languages`
CROSS JOIN UNNEST (`bigquery-public-data.github_repos.languages`.language)
where name = "Java" or name = "java")) as t
INNER JOIN `bigquery-public-data.github_repos.files` as q
on t.repo_name = q.repo_name and path = 'pom.xml')
